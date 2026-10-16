package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.GenericRequest;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.usuario.ActualizarPasswordRequest;
import com.proyecto.servicios.model.usuario.UsuarioResponse;
import com.proyecto.servicios.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> obtenerPorId(
            @PathVariable Integer id,
            @RequestAttribute(value = "rol", required = false) Integer rol) {
        if (rol == null || rol != 1) {
            throw new com.proyecto.servicios.exception.AccesoDenegadoException(
                    "AUTH-004", "Acceso denegado: Solo los administradores pueden consultar información de usuarios");
        }
        return ResponseEntity.ok(usuarioService.obtenerPorId(id));
    }

    @PutMapping("/actualizar-password")
    public ResponseEntity<GenericResponse> actualizarPassword(
            @Valid @RequestBody GenericRequest<ActualizarPasswordRequest> request,
            @RequestAttribute(value = "rol", required = false) Integer rol,
            @RequestAttribute(value = "usuarioId", required = false) Integer usuarioId) {

        Integer idTarget = request.getDatos() != null ? request.getDatos().getId() : null;
        if ((rol == null || rol != 1) && (usuarioId != null && !usuarioId.equals(idTarget))) {
            throw new com.proyecto.servicios.exception.AccesoDenegadoException(
                    "AUTH-004", "Acceso denegado: No tiene permisos para modificar la contraseña de otro usuario");
        }

        GenericResponse response = usuarioService.actualizarPassword(request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/desbloquear")
    public ResponseEntity<GenericResponse> desbloquearUsuario(
            @PathVariable Integer id,
            @RequestAttribute(value = "rol", required = false) Integer rol) {

        if (rol == null || rol != 1) {
            throw new com.proyecto.servicios.exception.AccesoDenegadoException(
                    "AUTH-008", "Acceso denegado: Solo un administrador puede desbloquear usuarios");
        }

        usuarioService.desbloquearUsuario(id);
        GenericResponse response = new GenericResponse();
        response.setCodigo(0);
        response.setMensaje("Usuario desbloqueado y reactivado exitosamente");
        return ResponseEntity.ok(response);
    }
}
