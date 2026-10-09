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
            log.warn("AUTH-002: Intento de inicio de sesión de usuario inactivo o bloqueado: {}", request.getCorreo());
            if (usuario.getIntentosFallidos() != null && usuario.getIntentosFallidos() >= 3) {
                throw new UsuarioInactivoException("AUTH-002", "Acceso bloqueado por exceder el límite de intentos fallidos permitidos. Por favor, contacte al administrador para reactivar su cuenta.");
            }
            throw new UsuarioInactivoException(request.getCorreo());
        }

        if (!passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
            int intentos = (usuario.getIntentosFallidos() != null ? usuario.getIntentosFallidos() : 0) + 1;
            usuario.setIntentosFallidos(intentos);
            if (intentos >= 3) {
                usuario.setActivo(false);
                usuarioRepository.save(usuario);
                log.warn("AUTH-002: Usuario bloqueado por alcanzar 3 intentos fallidos de login: {}", request.getCorreo());
                throw new UsuarioInactivoException("AUTH-002", "Acceso bloqueado: ha excedido el límite de 3 intentos fallidos de contraseña. Por favor, contacte al administrador para desbloquear su cuenta.");
            }
            usuarioRepository.save(usuario);
            log.warn("AUTH-003: Contraseña incorrecta para el usuario: {} (Intento {}/3)", request.getCorreo(), intentos);
            throw new CredencialesInvalidasException();
        }

        // Resetear contador de intentos tras autenticación exitosa
        if (usuario.getIntentosFallidos() != null && usuario.getIntentosFallidos() > 0) {
            usuario.setIntentosFallidos(0);
            usuarioRepository.save(usuario);
        }

        Integer clienteId = usuario.getCliente() != null ? usuario.getCliente().getId() : null;
        Integer rol = usuario.getRol() != null ? usuario.getRol() : 2;
        String token = jwtUtil.generarToken(usuario.getCorreo(), usuario.getId(), clienteId, rol);

        log.info("Autenticación exitosa para usuario ID: {} (Rol: {})", usuario.getId(), rol == 1 ? "ADMIN" : "CLIENTE");

        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .usuarioId(usuario.getId())
                .clienteId(clienteId)
                .correo(usuario.getCorreo())
                .rol(rol)
                .build();
    }
}
