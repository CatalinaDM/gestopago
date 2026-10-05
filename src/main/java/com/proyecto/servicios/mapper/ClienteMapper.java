package com.proyecto.servicios.mapper;

import com.proyecto.servicios.entity.cliente.Cliente;
import com.proyecto.servicios.model.cliente.ClienteActualizaRequest;
import com.proyecto.servicios.model.cliente.ClienteRegistroRequest;
import com.proyecto.servicios.model.cliente.ClienteResponse;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", uses = {DomicilioMapper.class, CuentaMapper.class})
public interface ClienteMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "fechaActualizacion", ignore = true)
    @Mapping(target = "domicilio", ignore = true)
    @Mapping(target = "cuentas", ignore = true)
    @Mapping(target = "usuario", ignore = true)
    Cliente toEntity(ClienteRegistroRequest request);

    @Mapping(target = "nombreCompleto", expression = "java(construirNombreCompleto(entity))")
    ClienteResponse toResponse(Cliente entity);

    List<ClienteResponse> toResponseList(List<Cliente> entities);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "curp", ignore = true)
    @Mapping(target = "rfc", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "fechaActualizacion", ignore = true)
    @Mapping(target = "domicilio", ignore = true)
    @Mapping(target = "cuentas", ignore = true)
    @Mapping(target = "usuario", ignore = true)
    void updateEntity(ClienteActualizaRequest request, @MappingTarget Cliente entity);

    default String construirNombreCompleto(Cliente cliente) {
        if (cliente == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        if (cliente.getNombre() != null) {
            sb.append(cliente.getNombre().trim());
        }
        if (cliente.getSegundoNombre() != null && !cliente.getSegundoNombre().isBlank()) {
            sb.append(" ").append(cliente.getSegundoNombre().trim());
        }
        if (cliente.getApellidoPaterno() != null) {
            sb.append(" ").append(cliente.getApellidoPaterno().trim());
        }
        if (cliente.getApellidoMaterno() != null) {
            sb.append(" ").append(cliente.getApellidoMaterno().trim());
        }
        return sb.toString().trim();
    }
}
