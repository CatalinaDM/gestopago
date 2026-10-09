package com.proyecto.servicios.repositorys.usuario;

import com.proyecto.servicios.entity.usuario.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByCorreo(String correo);

    Optional<Usuario> findByClienteId(Integer clienteId);

    boolean existsByCorreo(String correo);
}
