package com.proyecto.servicios.mapper;

import com.proyecto.servicios.entity.cliente.Cuenta;
import com.proyecto.servicios.model.cuenta.CuentaResponse;
import com.proyecto.servicios.model.cuenta.SaldoCuentaResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CuentaMapper {

    @Mapping(target = "clienteId", source = "cliente.id")
    CuentaResponse toResponse(Cuenta entity);

    List<CuentaResponse> toResponseList(List<Cuenta> entities);

    SaldoCuentaResponse toSaldoResponse(Cuenta entity);
}
