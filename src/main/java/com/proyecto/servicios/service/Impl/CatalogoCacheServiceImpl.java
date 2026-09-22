package com.proyecto.servicios.service.Impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.entity.gestopago.GestoPagoProducto;
import com.proyecto.servicios.service.CatalogoCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class CatalogoCacheServiceImpl implements CatalogoCacheService {

    private static final String CATALOGO_KEY = "gestopago:productos";

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public CatalogoCacheServiceImpl(
            RedisTemplate<String, Object> redisTemplate,
            ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<List<GestoPagoProducto>> obtenerCatalogo() {
        try {
            Object valor = redisTemplate.opsForValue().get(CATALOGO_KEY);

            if (valor == null) {
                return Optional.empty();
            }

            List<GestoPagoProducto> productos = objectMapper.convertValue(
                    valor,
                    new TypeReference<>() {
                    }
            );

            return Optional.of(productos);
        } catch (RuntimeException e) {
                log.warn("REDIS-001: no se pudo leer el catálogo desde Redis ({})",
                    e.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    @Override
    public void guardarCatalogo(List<GestoPagoProducto> productos) {
        try {
            redisTemplate.opsForValue().set(CATALOGO_KEY, productos);
            log.info("Catálogo guardado en Redis. Productos: {}", productos.size());
        } catch (RuntimeException e) {
                log.warn("REDIS-002: no se pudo guardar el catálogo en Redis ({})",
                    e.getClass().getSimpleName());
        }
    }
}
