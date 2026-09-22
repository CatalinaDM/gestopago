package com.proyecto.servicios.service.Impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.entity.gestopago.GestoPagoProducto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogoCacheServiceImplTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private CatalogoCacheServiceImpl cacheService;

    @BeforeEach
    void setUp() {
        cacheService = new CatalogoCacheServiceImpl(redisTemplate, new ObjectMapper());
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void debeDevolverCatalogoCuandoExisteEnRedis() {
        when(valueOperations.get("gestopago:productos")).thenReturn(List.of(Map.of(
                "idServicio", 56,
                "idProducto", 185,
                "producto", "Recarga"
        )));

        Optional<List<GestoPagoProducto>> resultado = cacheService.obtenerCatalogo();

        assertTrue(resultado.isPresent());
        assertEquals(1, resultado.get().size());
        assertEquals(56, resultado.get().get(0).getIdServicio());
        assertEquals(185, resultado.get().get(0).getIdProducto());
    }

    @Test
    void debeGuardarCatalogoEnRedis() {
        List<GestoPagoProducto> productos = List.of(producto());

        cacheService.guardarCatalogo(productos);

        verify(valueOperations).set("gestopago:productos", productos);
    }

    @Test
    void debeDevolverMissCuandoRedisNoEstaDisponible() {
        when(valueOperations.get("gestopago:productos"))
                .thenThrow(new RuntimeException("Redis no disponible"));

        Optional<List<GestoPagoProducto>> resultado = cacheService.obtenerCatalogo();

        assertTrue(resultado.isEmpty());
    }

    @Test
    void noDebePropagarErrorCuandoFallaLaEscrituraEnRedis() {
        doThrow(new RuntimeException("Redis no disponible"))
                .when(valueOperations).set("gestopago:productos", List.of(producto()));

        cacheService.guardarCatalogo(List.of(producto()));
    }

    private GestoPagoProducto producto() {
        GestoPagoProducto producto = new GestoPagoProducto();
        producto.setIdServicio(56);
        producto.setIdProducto(185);
        producto.setProducto("Recarga");
        return producto;
    }
}
