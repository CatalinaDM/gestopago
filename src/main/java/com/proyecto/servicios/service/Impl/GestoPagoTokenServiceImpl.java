package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoAuthClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.exception.GestoPagoAuthException;
import com.proyecto.servicios.mapper.GestoPagoTokenMapper;
import com.proyecto.servicios.model.gestopago.GestoPagoAuthResponse;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoTokenRepository;
import com.proyecto.servicios.service.GestoPagoTokenService;
import feign.FeignException;
import feign.RetryableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Slf4j
public class GestoPagoTokenServiceImpl implements GestoPagoTokenService {

    private static final String REDIS_TOKEN_KEY_PREFIX = "gestopago:jwt:token:";

    private final GestoPagoAuthClient gestoPagoAuthClient;
    private final GestoPagoTokenRepository tokenRepository;
    private final GestoPagoTokenMapper tokenMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ReentrantLock renewalLock = new ReentrantLock();

    @Value("${gestopago.auth.id-distribuidor}")
    private Integer idDistribuidor;

    @Value("${gestopago.auth.codigo-dispositivo}")
    private String codigoDispositivo;

    @Value("${gestopago.auth.password}")
    private String password;

    @Value("${gestopago.auth.token-ttl-seconds:82800}")
    private long tokenTtlSeconds = 82800L;

    public GestoPagoTokenServiceImpl(
            GestoPagoAuthClient gestoPagoAuthClient,
            GestoPagoTokenRepository tokenRepository,
            GestoPagoTokenMapper tokenMapper) {
        this(gestoPagoAuthClient, tokenRepository, tokenMapper, null);
    }

    @Autowired
    public GestoPagoTokenServiceImpl(
            GestoPagoAuthClient gestoPagoAuthClient,
            GestoPagoTokenRepository tokenRepository,
            GestoPagoTokenMapper tokenMapper,
            RedisTemplate<String, Object> redisTemplate) {
        this.gestoPagoAuthClient = gestoPagoAuthClient;
        this.tokenRepository = tokenRepository;
        this.tokenMapper = tokenMapper;
        this.redisTemplate = redisTemplate;
    }

    @Override
    @Scheduled(fixedRateString = "${gestopago.auth.refresh-rate-ms:3600000}", initialDelay = 0)
    public void renovarToken() {
        renewalLock.lock();
        try {
            log.info("Renovando token GestoPago para distribuidor={}", idDistribuidor);
            GestoPagoAuthResponse response = gestoPagoAuthClient.authenticate(
                    idDistribuidor, codigoDispositivo, password);

            if (response == null || response.getToken() == null || response.getToken().isBlank()) {
                log.error("La respuesta de GestoPago no contiene token");
                return;
            }

            persistirYCachearToken(response);
            log.info("Token GestoPago renovado correctamente");

        } catch (RetryableException e) {
            log.error("AUTH-001: timeout o error de conexión al renovar token GestoPago");
        } catch (FeignException e) {
            if (e.status() == 401 || e.status() == 403) {
                log.error("AUTH-002: GestoPago rechazó las credenciales. HTTP {}", e.status());
            } else {
                log.error("AUTH-003: GestoPago respondió con HTTP {}", e.status());
            }
        } catch (DataAccessException e) {
            log.error("AUTH-004: error al guardar el token en PostgreSQL");
        } catch (Exception e) {
            log.error("AUTH-999: error inesperado al renovar token ({})",
                    e.getClass().getSimpleName());
        } finally {
            renewalLock.unlock();
        }
    }

    @Override
    public Optional<GestoPagoToken> obtenerTokenActivo(Integer idDistribuidor, String codigoDispositivo) {
        return tokenRepository.findByIdDistribuidorAndCodigoDispositivo(idDistribuidor, codigoDispositivo);
    }

    @Override
    public Optional<String> obtenerTokenParaConsulta(Integer idDistribuidor, String codigoDispositivo) {
        Optional<String> tokenEnRedis = leerTokenDeRedis(idDistribuidor, codigoDispositivo);
        if (tokenEnRedis.isPresent()) {
            return tokenEnRedis;
        }

        try {
            Optional<GestoPagoToken> tokenPersistido = obtenerTokenActivo(idDistribuidor, codigoDispositivo);
            if (tokenPersistido.isPresent() && tokenPersistido.get().getToken() != null) {
                String token = tokenPersistido.get().getToken();
                guardarTokenEnRedis(idDistribuidor, codigoDispositivo, token);
                return Optional.of(token);
            }
        } catch (DataAccessException e) {
            log.warn("AUTH-005: no se pudo consultar el token persistido; se intentará autenticar con GestoPago");
        }

        try {
            GestoPagoAuthResponse response = gestoPagoAuthClient.authenticate(
                    idDistribuidor, codigoDispositivo, password);
            if (response == null || response.getToken() == null || response.getToken().isBlank()) {
                return Optional.empty();
            }
            guardarTokenEnRedis(idDistribuidor, codigoDispositivo, response.getToken());
            return Optional.of(response.getToken());
        } catch (Exception e) {
            log.error("AUTH-999: no se pudo obtener un token para consultar GestoPago ({})",
                    e.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    @Override
    public String obtenerTokenValido() {
        Optional<String> enRedis = leerTokenDeRedis(idDistribuidor, codigoDispositivo);
        if (enRedis.isPresent()) {
            return enRedis.get();
        }

        try {
            Optional<GestoPagoToken> enBd = obtenerTokenActivo(idDistribuidor, codigoDispositivo);
            if (enBd.isPresent() && enBd.get().getToken() != null && !enBd.get().getToken().isBlank()) {
                String token = enBd.get().getToken();
                guardarTokenEnRedis(idDistribuidor, codigoDispositivo, token);
                return token;
            }
        } catch (DataAccessException e) {
            log.warn("AUTH-005: No fue posible leer el token desde PostgreSQL, renovando directamente");
        }

        return renovarYObtenerToken(null);
    }

    @Override
    public String renovarYObtenerToken(String tokenRechazado) {
        renewalLock.lock();
        try {
            // Doble verificación: si otro hilo ya renovó el token mientras esperábamos el lock, reutilizarlo
            Optional<String> tokenActual = leerTokenDeRedis(idDistribuidor, codigoDispositivo);
            if (tokenActual.isPresent() && tokenRechazado != null && !tokenRechazado.equals(tokenActual.get())) {
                log.info("Token GestoPago ya fue renovado por un hilo concurrente");
                return tokenActual.get();
            }

            log.info("Iniciando renovación bajo demanda de token GestoPago para distribuidor={}", idDistribuidor);
            GestoPagoAuthResponse response = gestoPagoAuthClient.authenticate(
                    idDistribuidor, codigoDispositivo, password);

            if (response == null || response.getToken() == null || response.getToken().isBlank()) {
                throw new GestoPagoAuthException("El proveedor GestoPago no devolvió un token válido al autenticar");
            }

            persistirYCachearToken(response);
            log.info("Finalizó renovación bajo demanda de token GestoPago exitosamente");
            return response.getToken();
        } catch (GestoPagoAuthException e) {
            throw e;
        } catch (Exception e) {
            log.error("AUTH-GP-001: Error al renovar token con GestoPago ({})", e.getClass().getSimpleName());
            throw new GestoPagoAuthException("No fue posible autenticar con el proveedor GestoPago");
        } finally {
            renewalLock.unlock();
        }
    }

    private void persistirYCachearToken(GestoPagoAuthResponse response) {
        guardarTokenEnRedis(idDistribuidor, codigoDispositivo, response.getToken());
        try {
            GestoPagoToken tokenEntity = tokenRepository
                    .findByIdDistribuidorAndCodigoDispositivo(idDistribuidor, codigoDispositivo)
                    .map(existing -> {
                        tokenMapper.updateEntity(response, existing);
                        return existing;
                    })
                    .orElseGet(() -> {
                        GestoPagoToken nuevo = tokenMapper.toEntity(response);
                        nuevo.setIdDistribuidor(idDistribuidor);
                        nuevo.setCodigoDispositivo(codigoDispositivo);
                        nuevo.setActivo(true);
                        return nuevo;
                    });
            tokenRepository.save(tokenEntity);
        } catch (DataAccessException e) {
            log.warn("AUTH-004: El token se guardó en Redis pero no pudo persistirse en PostgreSQL");
        }
    }

    private Optional<String> leerTokenDeRedis(Integer idDist, String codDisp) {
        if (redisTemplate == null) {
            return Optional.empty();
        }
        try {
            Object val = redisTemplate.opsForValue().get(claveRedis(idDist, codDisp));
            if (val instanceof String str && !str.isBlank()) {
                return Optional.of(str);
            }
            return Optional.empty();
        } catch (RuntimeException e) {
            log.warn("REDIS-TOKEN-001: No se pudo leer el token desde Redis ({})", e.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    private void guardarTokenEnRedis(Integer idDist, String codDisp, String token) {
        if (redisTemplate == null || token == null || token.isBlank()) {
            return;
        }
        try {
            long ttl = (tokenTtlSeconds > 0 && tokenTtlSeconds < 86400) ? tokenTtlSeconds : 82800L;
            redisTemplate.opsForValue().set(claveRedis(idDist, codDisp), token, Duration.ofSeconds(ttl));
        } catch (RuntimeException e) {
            log.warn("REDIS-TOKEN-002: No se pudo guardar el token en Redis ({})", e.getClass().getSimpleName());
        }
    }

    private String claveRedis(Integer idDist, String codDisp) {
        return REDIS_TOKEN_KEY_PREFIX + idDist + ":" + codDisp;
    }
}
