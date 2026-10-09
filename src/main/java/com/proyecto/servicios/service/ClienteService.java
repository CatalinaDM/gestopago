package com.proyecto.servicios.service;

import com.proyecto.servicios.model.cliente.ClienteActualizaRequest;
import com.proyecto.servicios.model.cliente.ClienteRegistroRequest;
import com.proyecto.servicios.model.cliente.ClienteResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface ClienteService {

    ClienteResponse registrarCliente(ClienteRegistroRequest request);

    List<ClienteResponse> obtenerTodos();

    Page<ClienteResponse> obtenerPaginados(Pageable pageable);

    List<ClienteResponse> obtenerActivos();

    Page<ClienteResponse> obtenerActivosPaginados(Pageable pageable);

    Page<ClienteResponse> buscarClientesPaginados(String filtro, Pageable pageable);

    ClienteResponse obtenerPorId(Integer id);

    ClienteResponse obtenerPerfil(Integer clienteId);

    List<ClienteResponse> obtenerPorCurp(String curp);

    List<ClienteResponse> obtenerPorRfc(String rfc);

    List<ClienteResponse> obtenerPorCorreo(String correo);

    ClienteResponse obtenerPorNumeroCuenta(String numeroCuenta);

    List<ClienteResponse> obtenerPorRangoFechas(LocalDate fechaInicio, LocalDate fechaFin);

    List<ClienteResponse> buscarClientes(String filtro);

    List<ClienteResponse> buscarPorCriterios(String curp, String rfc, String email, String numeroCuenta);

    ClienteResponse actualizarCliente(Integer id, ClienteActualizaRequest request);

    void darDeBajaCliente(Integer id);
}
