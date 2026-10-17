package com.proyecto.servicios.service;

import com.proyecto.servicios.model.GenericRequest;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.cliente.ClienteAccionRequest;
import com.proyecto.servicios.model.cliente.ClienteActualizaRequest;
import com.proyecto.servicios.model.cliente.ClienteConsultaRequest;
import com.proyecto.servicios.model.cliente.ClienteRegistroRequest;
import com.proyecto.servicios.model.cliente.ClienteResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface ClienteService {

    ClienteResponse registrarCliente(GenericRequest<ClienteRegistroRequest> request);

    // Consulta unificada por DTO con filtros combinables y paginación
    Page<ClienteResponse> consultar(GenericRequest<ClienteConsultaRequest> request);

    // Acciones por DTO
    GenericResponse darDeBajaCliente(GenericRequest<ClienteAccionRequest> request);

    GenericResponse reactivarCliente(GenericRequest<ClienteAccionRequest> request);

    ClienteResponse actualizarCliente(GenericRequest<ClienteActualizaRequest> request);

    ClienteResponse obtenerPerfil(Integer clienteId);

    // Métodos de conveniencia / compatibilidad para servicios internos y tests
    List<ClienteResponse> obtenerTodos();

    Page<ClienteResponse> obtenerPaginados(Pageable pageable);

    List<ClienteResponse> obtenerActivos();

    Page<ClienteResponse> obtenerActivosPaginados(Pageable pageable);

    Page<ClienteResponse> buscarClientesPaginados(String filtro, Pageable pageable);

    ClienteResponse obtenerPorId(Integer id);

    List<ClienteResponse> obtenerPorCurp(String curp);

    List<ClienteResponse> obtenerPorRfc(String rfc);

    List<ClienteResponse> obtenerPorCorreo(String correo);

    ClienteResponse obtenerPorNumeroCuenta(String numeroCuenta);

    List<ClienteResponse> obtenerPorRangoFechas(LocalDate fechaInicio, LocalDate fechaFin);

    List<ClienteResponse> buscarClientes(String filtro);

    List<ClienteResponse> buscarPorCriterios(String curp, String rfc, String email, String numeroCuenta);

    void darDeBajaCliente(Integer id);

    void reactivarCliente(Integer id);
}
