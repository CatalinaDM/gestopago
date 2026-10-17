package com.proyecto.servicios.service;

import com.proyecto.servicios.model.GenericRequest;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.usuario.ActualizarPasswordRequest;
import com.proyecto.servicios.model.usuario.UsuarioConsultaRequest;
import com.proyecto.servicios.model.usuario.UsuarioDesbloqueoRequest;
import com.proyecto.servicios.model.usuario.UsuarioResponse;

public interface UsuarioService {

    UsuarioResponse obtenerPorId(GenericRequest<UsuarioConsultaRequest> request);

    GenericResponse actualizarPassword(GenericRequest<ActualizarPasswordRequest> request);

    GenericResponse desbloquearUsuario(GenericRequest<UsuarioDesbloqueoRequest> request);

    // Sobrecargas de conveniencia para servicios internos / tests
    UsuarioResponse obtenerPorId(Integer id);

    GenericResponse desbloquearUsuario(Integer id);
}
