package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.cliente.Cuenta;
import com.proyecto.servicios.exception.CuentaNoEncontradaException;
import com.proyecto.servicios.mapper.CuentaMapper;
import com.proyecto.servicios.model.cuenta.CuentaResponse;
import com.proyecto.servicios.model.cuenta.SaldoCuentaResponse;
import com.proyecto.servicios.repositorys.cliente.CuentaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CuentaServiceImplTest {

    @Mock
    private CuentaRepository cuentaRepository;

    private CuentaServiceImpl cuentaService;

    @BeforeEach
    void setUp() {
        CuentaMapper cuentaMapper = Mappers.getMapper(CuentaMapper.class);
        cuentaService = new CuentaServiceImpl(cuentaRepository, cuentaMapper);
    }

    @Test
    void obtenerPorNumeroCuenta_Exitoso() {
        Cuenta cuenta = new Cuenta();
        cuenta.setId(1);
        cuenta.setNumeroCuenta("1234567890");
        cuenta.setSaldo(new BigDecimal("5000.00"));
        cuenta.setActivo(true);

        when(cuentaRepository.findByNumeroCuenta("1234567890")).thenReturn(Optional.of(cuenta));

        CuentaResponse response = cuentaService.obtenerPorNumeroCuenta("1234567890");

        assertNotNull(response);
        assertEquals("1234567890", response.getNumeroCuenta());
        assertEquals(new BigDecimal("5000.00"), response.getSaldo());
    }

    @Test
    void obtenerPorNumeroCuenta_NoEncontrado() {
        when(cuentaRepository.findByNumeroCuenta("0000000000")).thenReturn(Optional.empty());

        assertThrows(CuentaNoEncontradaException.class, () -> cuentaService.obtenerPorNumeroCuenta("0000000000"));
    }

    @Test
    void obtenerSaldo_Exitoso() {
        Cuenta cuenta = new Cuenta();
        cuenta.setNumeroCuenta("1234567890");
        cuenta.setSaldo(new BigDecimal("1500.50"));
        cuenta.setActivo(true);

        when(cuentaRepository.findByNumeroCuenta("1234567890")).thenReturn(Optional.of(cuenta));

        SaldoCuentaResponse response = cuentaService.obtenerSaldo("1234567890");

        assertNotNull(response);
        assertEquals("1234567890", response.getNumeroCuenta());
        assertEquals(new BigDecimal("1500.50"), response.getSaldo());
    }

    @Test
    void obtenerCuentasActivas_Exitoso() {
        Cuenta cuenta = new Cuenta();
        cuenta.setNumeroCuenta("1234567890");
        cuenta.setActivo(true);

        when(cuentaRepository.findByActivoTrue()).thenReturn(List.of(cuenta));

        List<CuentaResponse> response = cuentaService.obtenerCuentasActivas();

        assertNotNull(response);
        assertEquals(1, response.size());
    }
}
