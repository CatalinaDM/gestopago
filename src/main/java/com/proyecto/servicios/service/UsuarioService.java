package com.proyecto.servicios.service;

import com.proyecto.servicios.model.usuario.ActualizarPasswordRequest;
import com.proyecto.servicios.model.usuario.UsuarioResponse;

public interface UsuarioService {

    UsuarioResponse obtenerPorId(Integer id);

    void actualizarPassword(Integer id, ActualizarPasswordRequest request);

    void desbloquearUsuario(Integer id);
}
