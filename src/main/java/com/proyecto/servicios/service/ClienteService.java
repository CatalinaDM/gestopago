package com.proyecto.servicios.service;

import com.proyecto.servicios.model.cliente.ClienteActualizaRequest;
import com.proyecto.servicios.model.cliente.ClienteRegistroRequest;
import com.proyecto.servicios.model.cliente.ClienteResponse;

import java.time.LocalDate;
import java.util.List;

public interface ClienteService {

    ClienteResponse registrarCliente(ClienteRegistroRequest request);

    List<ClienteResponse> obtenerTodos();

    List<ClienteResponse> obtenerActivos();

    ClienteResponse obtenerPorId(Integer id);

    ClienteResponse obtenerPorCurp(String curp);

    ClienteResponse obtenerPorRfc(String rfc);

    ClienteResponse obtenerPorCorreo(String correo);

    ClienteResponse obtenerPorNumeroCuenta(String numeroCuenta);

    List<ClienteResponse> obtenerPorRangoFechas(LocalDate fechaInicio, LocalDate fechaFin);

    List<ClienteResponse> buscarClientes(String filtro);

    List<ClienteResponse> buscarPorCriterios(String curp, String rfc, String email, String numeroCuenta);

    ClienteResponse actualizarCliente(Integer id, ClienteActualizaRequest request);

    void darDeBajaCliente(Integer id);
}
