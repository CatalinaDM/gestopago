package com.proyecto.servicios.service;

import com.proyecto.servicios.model.cuenta.CuentaResponse;
import com.proyecto.servicios.model.cuenta.SaldoCuentaResponse;

import java.util.List;

public interface CuentaService {

    CuentaResponse obtenerPorNumeroCuenta(String numeroCuenta);

    SaldoCuentaResponse obtenerSaldo(String numeroCuenta);

    List<CuentaResponse> obtenerCuentasActivas();
}
