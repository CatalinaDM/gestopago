package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.usuario.Usuario;
import com.proyecto.servicios.exception.ContrasenaInvalidaException;
import com.proyecto.servicios.exception.UsuarioNoEncontradoException;
import com.proyecto.servicios.mapper.UsuarioMapper;
import com.proyecto.servicios.model.usuario.ActualizarPasswordRequest;
import com.proyecto.servicios.model.usuario.UsuarioResponse;
import com.proyecto.servicios.repositorys.usuario.UsuarioRepository;
import com.proyecto.servicios.service.UsuarioService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioMapper usuarioMapper;
    private final PasswordEncoder passwordEncoder;

    public UsuarioServiceImpl(
            UsuarioRepository usuarioRepository,
            UsuarioMapper usuarioMapper,
            PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioMapper = usuarioMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UsuarioResponse obtenerPorId(Integer id) {
        log.info("Consultando usuario por ID: {}", id);
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("AUTH-001: Usuario no encontrado con ID: {}", id);
                    return new UsuarioNoEncontradoException(String.valueOf(id));
                });
        return usuarioMapper.toResponse(usuario);
    }

    @Override
    @Transactional
    public void actualizarPassword(Integer id, ActualizarPasswordRequest request) {
        log.info("Actualizando contraseña para el usuario ID: {}", id);

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("AUTH-001: Usuario no encontrado con ID: {}", id);
                    return new UsuarioNoEncontradoException(String.valueOf(id));
                });

        if (!passwordEncoder.matches(request.getPasswordActual(), usuario.getPassword())) {
            log.warn("AUTH-004: La contraseña actual proporcionada es incorrecta para el usuario ID: {}", id);
            throw new ContrasenaInvalidaException("La contraseña actual no coincide");
        }

        if (passwordEncoder.matches(request.getPasswordNuevo(), usuario.getPassword())) {
            log.warn("AUTH-004: La nueva contraseña no puede ser igual a la anterior para el usuario ID: {}", id);
            throw new ContrasenaInvalidaException("La nueva contraseña no puede ser igual a la contraseña actual");
        }

        usuario.setPassword(passwordEncoder.encode(request.getPasswordNuevo()));
        usuarioRepository.save(usuario);

        log.info("Contraseña actualizada exitosamente para el usuario ID: {}", id);
    }
}
