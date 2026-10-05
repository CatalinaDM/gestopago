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
            @Valid @RequestBody ActualizarPasswordRequest request) {
        usuarioService.actualizarPassword(id, request);
        GenericResponse response = new GenericResponse();
        response.setCodigo(0);
        response.setMensaje("Contraseña actualizada correctamente");
        return ResponseEntity.ok(response);
    }
}
