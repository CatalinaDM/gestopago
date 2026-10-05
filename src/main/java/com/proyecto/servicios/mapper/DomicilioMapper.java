package com.proyecto.servicios.mapper;

import com.proyecto.servicios.entity.cliente.Domicilio;
import com.proyecto.servicios.model.cliente.DomicilioDTO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface DomicilioMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "cliente", ignore = true)
    Domicilio toEntity(DomicilioDTO dto);

    DomicilioDTO toDto(Domicilio entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "cliente", ignore = true)
    void updateEntity(DomicilioDTO dto, @MappingTarget Domicilio entity);
}
