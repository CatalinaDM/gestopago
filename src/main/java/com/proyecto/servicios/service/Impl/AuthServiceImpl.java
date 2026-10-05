package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.usuario.Usuario;
import com.proyecto.servicios.exception.CredencialesInvalidasException;
import com.proyecto.servicios.exception.UsuarioInactivoException;
import com.proyecto.servicios.exception.UsuarioNoEncontradoException;
import com.proyecto.servicios.model.auth.LoginRequest;
import com.proyecto.servicios.model.auth.LoginResponse;
import com.proyecto.servicios.repositorys.usuario.UsuarioRepository;
import com.proyecto.servicios.security.JwtUtil;
import com.proyecto.servicios.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthServiceImpl(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        log.info("Iniciando autenticación para el usuario: {}", request.getCorreo());

        Usuario usuario = usuarioRepository.findByCorreo(request.getCorreo())
                .orElseThrow(() -> {
                    log.warn("AUTH-001: Usuario no encontrado con correo: {}", request.getCorreo());
                    return new UsuarioNoEncontradoException(request.getCorreo());
                });

        if (Boolean.FALSE.equals(usuario.getActivo())) {
            log.warn("AUTH-002: Intento de inicio de sesión de usuario inactivo: {}", request.getCorreo());
            throw new UsuarioInactivoException(request.getCorreo());
        }

        if (!passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
            log.warn("AUTH-003: Contraseña incorrecta para el usuario: {}", request.getCorreo());
            throw new CredencialesInvalidasException();
        }

        Integer clienteId = usuario.getCliente() != null ? usuario.getCliente().getId() : null;
        String token = jwtUtil.generarToken(usuario.getCorreo(), usuario.getId(), clienteId);

        log.info("Autenticación exitosa para usuario ID: {}", usuario.getId());

        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .usuarioId(usuario.getId())
                .clienteId(clienteId)
                .correo(usuario.getCorreo())
                .build();
    }
}
