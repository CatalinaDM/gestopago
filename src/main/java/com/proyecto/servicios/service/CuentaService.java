package com.proyecto.servicios.service;

import com.proyecto.servicios.model.cuenta.CuentaResponse;
import com.proyecto.servicios.model.cuenta.SaldoCuentaResponse;

import java.util.List;

public interface CuentaService {

    CuentaResponse obtenerPorNumeroCuenta(String numeroCuenta);

    CuentaResponse obtenerPorNumeroCuenta(String numeroCuenta, Integer rol, Integer tokenClienteId);

    SaldoCuentaResponse obtenerSaldo(String numeroCuenta);

    SaldoCuentaResponse obtenerSaldo(String numeroCuenta, Integer rol, Integer tokenClienteId);

    List<CuentaResponse> obtenerCuentasActivas();

    List<CuentaResponse> obtenerCuentasPorCliente(Integer clienteId);
}
