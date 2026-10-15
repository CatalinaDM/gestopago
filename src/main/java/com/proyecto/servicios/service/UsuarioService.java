package com.proyecto.servicios.service;

import com.proyecto.servicios.model.GenericRequest;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.usuario.ActualizarPasswordRequest;
import com.proyecto.servicios.model.usuario.UsuarioResponse;

public interface UsuarioService {

    UsuarioResponse obtenerPorId(Integer id);

    GenericResponse actualizarPassword(GenericRequest<ActualizarPasswordRequest> request);

    void desbloquearUsuario(Integer id);
}
