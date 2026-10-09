package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.usuario.Usuario;
import com.proyecto.servicios.exception.ContrasenaInvalidaException;
import com.proyecto.servicios.exception.UsuarioNoEncontradoException;
import com.proyecto.servicios.mapper.UsuarioMapper;
import com.proyecto.servicios.model.usuario.ActualizarPasswordRequest;
import com.proyecto.servicios.model.usuario.UsuarioResponse;
import com.proyecto.servicios.repositorys.usuario.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UsuarioServiceImpl usuarioService;

    @BeforeEach
    void setUp() {
        UsuarioMapper usuarioMapper = Mappers.getMapper(UsuarioMapper.class);
        usuarioService = new UsuarioServiceImpl(usuarioRepository, usuarioMapper, passwordEncoder);
    }

    @Test
    void obtenerPorId_Exitoso() {
        Usuario usuario = new Usuario();
        usuario.setId(1);
        usuario.setCorreo("user@example.com");
        usuario.setActivo(true);

        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));

        UsuarioResponse response = usuarioService.obtenerPorId(1);

        assertNotNull(response);
        assertEquals(1, response.getId());
        assertEquals("user@example.com", response.getCorreo());
    }

    @Test
    void obtenerPorId_NoEncontrado() {
        when(usuarioRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(UsuarioNoEncontradoException.class, () -> usuarioService.obtenerPorId(99));
    }

    @Test
    void actualizarPassword_Exitoso() {
        Usuario usuario = new Usuario();
        usuario.setId(1);
        usuario.setPassword("old_hash");

        ActualizarPasswordRequest request = new ActualizarPasswordRequest("OldPassword123!", "NewPassword123!");

        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("OldPassword123!", "old_hash")).thenReturn(true);
        when(passwordEncoder.matches("NewPassword123!", "old_hash")).thenReturn(false);
        when(passwordEncoder.encode("NewPassword123!")).thenReturn("new_hash");

        usuarioService.actualizarPassword(1, request);

        assertEquals("new_hash", usuario.getPassword());
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void actualizarPassword_PasswordActualInvalido() {
        Usuario usuario = new Usuario();
        usuario.setId(1);
        usuario.setPassword("old_hash");

        ActualizarPasswordRequest request = new ActualizarPasswordRequest("WrongPassword!", "NewPassword123!");

        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("WrongPassword!", "old_hash")).thenReturn(false);

        assertThrows(ContrasenaInvalidaException.class, () -> usuarioService.actualizarPassword(1, request));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void actualizarPassword_PasswordNuevoIgualAlActual() {
        Usuario usuario = new Usuario();
        usuario.setId(1);
        usuario.setPassword("old_hash");

        ActualizarPasswordRequest request = new ActualizarPasswordRequest("SamePassword123!", "SamePassword123!");

        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("SamePassword123!", "old_hash")).thenReturn(true);

        assertThrows(ContrasenaInvalidaException.class, () -> usuarioService.actualizarPassword(1, request));
        verify(usuarioRepository, never()).save(any());
    }
}
