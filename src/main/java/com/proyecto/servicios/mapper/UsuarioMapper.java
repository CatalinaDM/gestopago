package com.proyecto.servicios.mapper;

import com.proyecto.servicios.entity.usuario.Usuario;
import com.proyecto.servicios.model.usuario.UsuarioResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UsuarioMapper {

    @Mapping(target = "clienteId", source = "cliente.id")
    UsuarioResponse toResponse(Usuario entity);
}
