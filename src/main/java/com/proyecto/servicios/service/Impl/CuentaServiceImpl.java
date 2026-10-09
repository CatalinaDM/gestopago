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
        Cuenta cuenta = buscarCuentaPorNumero(numeroCuenta);
        return cuentaMapper.toResponse(cuenta);
    }

    @Override
    public CuentaResponse obtenerPorNumeroCuenta(String numeroCuenta, Integer rol, Integer tokenClienteId) {
        log.info("Consultando cuenta por número: {} con validación de permisos (Rol: {}, ClienteId: {})",
                numeroCuenta, rol, tokenClienteId);
        Cuenta cuenta = buscarCuentaPorNumero(numeroCuenta);
        validarPropiedadCuenta(cuenta, rol, tokenClienteId);
        return cuentaMapper.toResponse(cuenta);
    }

    @Override
    public SaldoCuentaResponse obtenerSaldo(String numeroCuenta) {
        log.info("Consultando saldo de la cuenta: {}", numeroCuenta);
        Cuenta cuenta = buscarCuentaPorNumero(numeroCuenta);
        return cuentaMapper.toSaldoResponse(cuenta);
    }

    @Override
    public SaldoCuentaResponse obtenerSaldo(String numeroCuenta, Integer rol, Integer tokenClienteId) {
        log.info("Consultando saldo de la cuenta: {} con validación de permisos (Rol: {}, ClienteId: {})",
                numeroCuenta, rol, tokenClienteId);
        Cuenta cuenta = buscarCuentaPorNumero(numeroCuenta);
        validarPropiedadCuenta(cuenta, rol, tokenClienteId);
        return cuentaMapper.toSaldoResponse(cuenta);
    }

    @Override
    public List<CuentaResponse> obtenerCuentasActivas() {
        log.info("Consultando todas las cuentas activas");
        List<Cuenta> cuentas = cuentaRepository.findByActivoTrue();
        return cuentaMapper.toResponseList(cuentas);
    }

    @Override
    public List<CuentaResponse> obtenerCuentasPorCliente(Integer clienteId) {
        log.info("Consultando cuentas activas para el cliente ID: {}", clienteId);
        List<Cuenta> cuentas = cuentaRepository.findByClienteIdAndActivoTrue(clienteId);
        return cuentaMapper.toResponseList(cuentas);
    }

    private Cuenta buscarCuentaPorNumero(String numeroCuenta) {
        return cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> {
                    log.warn("CUENTA-001: Cuenta no encontrada con número: {}", numeroCuenta);
                    return new CuentaNoEncontradaException(numeroCuenta);
                });
    }

    private void validarPropiedadCuenta(Cuenta cuenta, Integer rol, Integer tokenClienteId) {
        // Si no es ADMIN (Rol 1), la cuenta debe pertenecer obligatoriamente al cliente del token
        if (rol == null || rol != 1) {
            if (tokenClienteId == null || cuenta.getCliente() == null || !tokenClienteId.equals(cuenta.getCliente().getId())) {
                log.warn("AUTH-004: Intento no autorizado de consultar cuenta {} perteneciente a cliente {}, desde token con clienteId {}",
                        cuenta.getNumeroCuenta(),
                        cuenta.getCliente() != null ? cuenta.getCliente().getId() : "null",
                        tokenClienteId);
                throw new com.proyecto.servicios.exception.AccesoDenegadoException(
                        "AUTH-004", "Acceso denegado: No tiene permisos para consultar una cuenta bancaria que no le pertenece");
            }
        }
    }
}
