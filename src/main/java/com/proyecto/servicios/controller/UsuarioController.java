package com.proyecto.servicios.controller;

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
    public ResponseEntity<UsuarioResponse> obtenerPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(usuarioService.obtenerPorId(id));
    }

    @PutMapping("/{id}/password")
    public ResponseEntity<GenericResponse> actualizarPassword(
            @PathVariable Integer id,
            @RequestAttribute(value = "usuarioId", required = false) Integer tokenUsuarioId,
            @RequestAttribute(value = "rol", required = false) Integer rol,
            @Valid @RequestBody ActualizarPasswordRequest request) {

        // Validar que el usuario solo pueda modificar su propia contraseña salvo que sea ADMIN (rol 1)
        if (tokenUsuarioId != null && !tokenUsuarioId.equals(id) && (rol == null || rol != 1)) {
            throw new com.proyecto.servicios.exception.ValidacionException(
                    "AUTH-007", "No tiene permisos para modificar la contraseña de otro usuario");
        }

        usuarioService.actualizarPassword(id, request);
        GenericResponse response = new GenericResponse();
        response.setCodigo(0);
        response.setMensaje("Contraseña actualizada correctamente");
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/desbloquear")
    public ResponseEntity<GenericResponse> desbloquearUsuario(
            @PathVariable Integer id,
            @RequestAttribute(value = "rol", required = false) Integer rol) {

        if (rol == null || rol != 1) {
            throw new com.proyecto.servicios.exception.ValidacionException(
                    "AUTH-008", "Solo un administrador puede desbloquear usuarios");
        }

        usuarioService.desbloquearUsuario(id);
        GenericResponse response = new GenericResponse();
        response.setCodigo(0);
        response.setMensaje("Usuario desbloqueado y reactivado exitosamente");
        return ResponseEntity.ok(response);
    }
}
