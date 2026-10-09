package com.proyecto.servicios.mapper;

import com.proyecto.servicios.entity.cliente.Domicilio;
import com.proyecto.servicios.model.cliente.DomicilioDTO;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {StringSanitizer.class})
public interface DomicilioMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "cliente", ignore = true)
    @Mapping(target = "calle", qualifiedByName = "trim")
    @Mapping(target = "numeroExterior", qualifiedByName = "trim")
    @Mapping(target = "numeroInterior", qualifiedByName = "trim")
    @Mapping(target = "colonia", qualifiedByName = "trim")
    @Mapping(target = "municipio", qualifiedByName = "trim")
    @Mapping(target = "estado", qualifiedByName = "trim")
    @Mapping(target = "codigoPostal", qualifiedByName = "trim")
    @Mapping(target = "pais", qualifiedByName = "trim")
    Domicilio toEntity(DomicilioDTO dto);

    DomicilioDTO toDto(Domicilio entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "cliente", ignore = true)
    @Mapping(target = "calle", qualifiedByName = "trim")
    @Mapping(target = "numeroExterior", qualifiedByName = "trim")
    @Mapping(target = "numeroInterior", qualifiedByName = "trim")
    @Mapping(target = "colonia", qualifiedByName = "trim")
    @Mapping(target = "municipio", qualifiedByName = "trim")
    @Mapping(target = "estado", qualifiedByName = "trim")
    @Mapping(target = "codigoPostal", qualifiedByName = "trim")
    @Mapping(target = "pais", qualifiedByName = "trim")
    void updateEntity(DomicilioDTO dto, @MappingTarget Domicilio entity);
}
