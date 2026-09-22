package com.proyecto.servicios.mapper;

import com.proyecto.servicios.entity.gestopago.GestoPagoProducto;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GestoPagoProductoMapperTest {

    private final GestoPagoProductoMapper mapper =
            Mappers.getMapper(GestoPagoProductoMapper.class);

    @Test
    void debeConvertirModeloXmlAEntidadSinSobrescribirId() {
        com.proyecto.servicios.model.gestopago.GestoPagoProducto modelo = productoModelo();

        GestoPagoProducto resultado = mapper.toEntity(modelo);

        assertNull(resultado.getId());
        assertEquals(modelo.getIdServicio(), resultado.getIdServicio());
        assertEquals(modelo.getProducto(), resultado.getProducto());
        assertEquals(modelo.getLegend(), resultado.getLegend());
    }

    @Test
    void debeActualizarCamposDeNegocioSinCambiarIdentidad() {
        com.proyecto.servicios.model.gestopago.GestoPagoProducto modelo = productoModelo();
        GestoPagoProducto entidad = new GestoPagoProducto();
        entidad.setId(99);
        entidad.setIdServicio(1);
        entidad.setIdProducto(2);

        mapper.updateEntity(modelo, entidad);

        assertEquals(99, entidad.getId());
        assertEquals(1, entidad.getIdServicio());
        assertEquals(2, entidad.getIdProducto());
        assertEquals(modelo.getPrecio(), entidad.getPrecio());
        assertEquals(modelo.getLegend(), entidad.getLegend());
    }

    private com.proyecto.servicios.model.gestopago.GestoPagoProducto productoModelo() {
        com.proyecto.servicios.model.gestopago.GestoPagoProducto producto =
                new com.proyecto.servicios.model.gestopago.GestoPagoProducto();
        producto.setIdServicio(56);
        producto.setIdProducto(185);
        producto.setProducto("Recarga");
        producto.setPrecio("10.0");
        producto.setLegend("Ayuda");
        return producto;
    }
}
