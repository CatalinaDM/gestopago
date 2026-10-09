package com.proyecto.servicios.mapper;

import com.proyecto.servicios.entity.gestopago.GestoPagoProducto;
import com.proyecto.servicios.model.gestopago.GestoPagoProductoResponse;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring")
public interface GestoPagoProductoMapper {

    @Mapping(target = "id", ignore = true)
    GestoPagoProducto toEntity(
            com.proyecto.servicios.model.gestopago.GestoPagoProducto producto);

    GestoPagoProductoResponse toResponse(GestoPagoProducto entity);

    List<GestoPagoProductoResponse> toResponseList(List<GestoPagoProducto> entities);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "idServicio", ignore = true)
    @Mapping(target = "idProducto", ignore = true)
    void updateEntity(
            com.proyecto.servicios.model.gestopago.GestoPagoProducto producto,
            @MappingTarget GestoPagoProducto entity);
}