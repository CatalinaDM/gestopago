package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoAuthClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.mapper.GestoPagoTokenMapper;
import com.proyecto.servicios.model.gestopago.GestoPagoAuthResponse;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoTokenRepository;
import com.proyecto.servicios.service.GestoPagoTokenService;
import feign.FeignException;
import feign.RetryableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class GestoPagoTokenServiceImpl implements GestoPagoTokenService {

    private final GestoPagoAuthClient gestoPagoAuthClient;
    private final GestoPagoTokenRepository tokenRepository;
    private final GestoPagoTokenMapper tokenMapper;

    @Value("${gestopago.auth.id-distribuidor}")
    private Integer idDistribuidor;

    @Value("${gestopago.auth.codigo-dispositivo}")
    private String codigoDispositivo;

    @Value("${gestopago.auth.password}")
    private String password;

    public GestoPagoTokenServiceImpl(GestoPagoAuthClient gestoPagoAuthClient,
                                     GestoPagoTokenRepository tokenRepository,
                                     GestoPagoTokenMapper tokenMapper) {
        this.gestoPagoAuthClient = gestoPagoAuthClient;
        this.tokenRepository = tokenRepository;
        this.tokenMapper = tokenMapper;
    }

    @Override
    @Scheduled(fixedRateString = "${gestopago.auth.refresh-rate-ms:3600000}", initialDelay = 0)
    public void renovarToken() {
        log.info("Renovando token GestoPago para distribuidor={}", idDistribuidor);
        try {
            GestoPagoAuthResponse response = gestoPagoAuthClient.authenticate(
                    idDistribuidor, codigoDispositivo, password);

            if (response == null || response.getToken() == null) {
                log.error("La respuesta de GestoPago no contiene token");
                return;
            }

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
        }
    }

    @Override
    public Optional<GestoPagoToken> obtenerTokenActivo(Integer idDistribuidor, String codigoDispositivo) {
        return tokenRepository.findByIdDistribuidorAndCodigoDispositivo(idDistribuidor, codigoDispositivo);
    }

    @Override
    public Optional<String> obtenerTokenParaConsulta(Integer idDistribuidor, String codigoDispositivo) {
        try {
            Optional<GestoPagoToken> tokenPersistido = obtenerTokenActivo(idDistribuidor, codigoDispositivo);
            if (tokenPersistido.isPresent()) {
                return tokenPersistido.map(GestoPagoToken::getToken);
            }
        } catch (DataAccessException e) {
            log.warn("AUTH-005: no se pudo consultar el token persistido; se intentará autenticar con GestoPago");
        }

        try {
            GestoPagoAuthResponse response = gestoPagoAuthClient.authenticate(
                    idDistribuidor, codigoDispositivo, password);
            return response == null || response.getToken() == null
                    ? Optional.empty()
                    : Optional.of(response.getToken());
        } catch (Exception e) {
            log.error("AUTH-999: no se pudo obtener un token para consultar GestoPago ({})",
                    e.getClass().getSimpleName());
            return Optional.empty();
        }
    }
}
