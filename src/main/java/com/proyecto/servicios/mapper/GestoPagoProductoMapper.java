package com.proyecto.servicios.mapper;

import com.proyecto.servicios.entity.gestopago.GestoPagoProducto;
import com.proyecto.servicios.enums.ModoCaptura;
import com.proyecto.servicios.model.gestopago.GestoPagoProductoResponse;
import com.proyecto.servicios.service.CatalogoClasificadorComponent;
import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring")
public interface GestoPagoProductoMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "modoCaptura", ignore = true)
    GestoPagoProducto toEntity(
            com.proyecto.servicios.model.gestopago.GestoPagoProducto producto);

    GestoPagoProductoResponse toResponse(GestoPagoProducto entity);

    List<GestoPagoProductoResponse> toResponseList(List<GestoPagoProducto> entities);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "idServicio", ignore = true)
    @Mapping(target = "idProducto", ignore = true)
    @Mapping(target = "modoCaptura", ignore = true)
    void updateEntity(
            com.proyecto.servicios.model.gestopago.GestoPagoProducto producto,
            @MappingTarget GestoPagoProducto entity);

    @AfterMapping
    default void calcularModoCaptura(
            com.proyecto.servicios.model.gestopago.GestoPagoProducto source,
            @MappingTarget GestoPagoProducto target) {
        if (source != null && target != null) {
            target.setModoCaptura(CatalogoClasificadorComponent.calcularModoCaptura(
                    target.getTipoFront(),
                    target.getIdCatTipoServicio(),
                    target.getTipoReferencia()
            ));
        }
    }

    @AfterMapping
    default void asegurarModoCapturaEnResponse(
            GestoPagoProducto source,
            @MappingTarget GestoPagoProductoResponse target) {
        if (source != null && target != null) {
            if (target.getModoCaptura() == null || target.getModoCaptura() == ModoCaptura.NO_SOPORTADO) {
                target.setModoCaptura(CatalogoClasificadorComponent.calcularModoCaptura(
                        source.getTipoFront(),
                        source.getIdCatTipoServicio(),
                        source.getTipoReferencia()
                ));
            }
        }
    }
}
