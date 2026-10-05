package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.cliente.Cliente;
import com.proyecto.servicios.entity.usuario.Usuario;
import com.proyecto.servicios.exception.CredencialesInvalidasException;
import com.proyecto.servicios.exception.UsuarioInactivoException;
import com.proyecto.servicios.exception.UsuarioNoEncontradoException;
import com.proyecto.servicios.model.auth.LoginRequest;
import com.proyecto.servicios.model.auth.LoginResponse;
import com.proyecto.servicios.repositorys.usuario.UsuarioRepository;
import com.proyecto.servicios.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(usuarioRepository, passwordEncoder, jwtUtil);
    }

    @Test
    void login_Exitoso() {
        LoginRequest request = new LoginRequest("test@example.com", "Password123!");
        Usuario usuario = new Usuario();
        usuario.setId(1);
        usuario.setCorreo("test@example.com");
        usuario.setPassword("hashed_pass");
        usuario.setActivo(true);
        Cliente cliente = new Cliente();
        cliente.setId(10);
        usuario.setCliente(cliente);

        when(usuarioRepository.findByCorreo("test@example.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("Password123!", "hashed_pass")).thenReturn(true);
        when(jwtUtil.generarToken("test@example.com", 1, 10)).thenReturn("jwt-token-sample");

        LoginResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("jwt-token-sample", response.getToken());
        assertEquals(1, response.getUsuarioId());
        assertEquals(10, response.getClienteId());
    }

    @Test
    void login_UsuarioNoEncontrado() {
        LoginRequest request = new LoginRequest("noexiste@example.com", "Password123!");
        when(usuarioRepository.findByCorreo("noexiste@example.com")).thenReturn(Optional.empty());

        assertThrows(UsuarioNoEncontradoException.class, () -> authService.login(request));
    }

    @Test
    void login_UsuarioInactivo() {
        LoginRequest request = new LoginRequest("inactivo@example.com", "Password123!");
        Usuario usuario = new Usuario();
        usuario.setCorreo("inactivo@example.com");
        usuario.setActivo(false);

        when(usuarioRepository.findByCorreo("inactivo@example.com")).thenReturn(Optional.of(usuario));

        assertThrows(UsuarioInactivoException.class, () -> authService.login(request));
    }

    @Test
    void login_CredencialesInvalidas() {
        LoginRequest request = new LoginRequest("test@example.com", "WrongPassword");
        Usuario usuario = new Usuario();
        usuario.setCorreo("test@example.com");
        usuario.setPassword("hashed_pass");
        usuario.setActivo(true);

        when(usuarioRepository.findByCorreo("test@example.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("WrongPassword", "hashed_pass")).thenReturn(false);

        assertThrows(CredencialesInvalidasException.class, () -> authService.login(request));
    }
}
