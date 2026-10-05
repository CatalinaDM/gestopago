package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.cliente.Cuenta;
import com.proyecto.servicios.exception.CuentaNoEncontradaException;
import com.proyecto.servicios.mapper.CuentaMapper;
import com.proyecto.servicios.model.cuenta.CuentaResponse;
import com.proyecto.servicios.model.cuenta.SaldoCuentaResponse;
import com.proyecto.servicios.repositorys.cliente.CuentaRepository;
import com.proyecto.servicios.service.CuentaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class CuentaServiceImpl implements CuentaService {

    private final CuentaRepository cuentaRepository;
    private final CuentaMapper cuentaMapper;

    public CuentaServiceImpl(CuentaRepository cuentaRepository, CuentaMapper cuentaMapper) {
        this.cuentaRepository = cuentaRepository;
        this.cuentaMapper = cuentaMapper;
    }

    @Override
    public CuentaResponse obtenerPorNumeroCuenta(String numeroCuenta) {
        log.info("Consultando cuenta por número: {}", numeroCuenta);
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> {
                    log.warn("CUENTA-001: Cuenta no encontrada con número: {}", numeroCuenta);
                    return new CuentaNoEncontradaException(numeroCuenta);
                });
        return cuentaMapper.toResponse(cuenta);
    }

    @Override
    public SaldoCuentaResponse obtenerSaldo(String numeroCuenta) {
        log.info("Consultando saldo de la cuenta: {}", numeroCuenta);
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> {
                    log.warn("CUENTA-001: Cuenta no encontrada para consulta de saldo: {}", numeroCuenta);
                    return new CuentaNoEncontradaException(numeroCuenta);
                });
        return cuentaMapper.toSaldoResponse(cuenta);
    }

    @Override
    public List<CuentaResponse> obtenerCuentasActivas() {
        log.info("Consultando todas las cuentas activas");
        List<Cuenta> cuentas = cuentaRepository.findByEstatus("ACTIVA");
        return cuentaMapper.toResponseList(cuentas);
    }
}
