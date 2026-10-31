package com.proyecto.servicios.mapper;

import com.proyecto.servicios.entity.gestopago.GestoPagoProducto;
import com.proyecto.servicios.entity.pago.TransaccionPago;
import com.proyecto.servicios.model.pago.ComprobantePagoResponse;
import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface TransaccionPagoMapper {

    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "codigo", constant = "0")
    @Mapping(target = "mensaje", source = "mensajeRespuesta")
    @Mapping(target = "idTransaccion", source = "transaccion.id")
    @Mapping(target = "upc", source = "transaccion.upc")
    @Mapping(target = "numeroCuenta", source = "numeroCuenta")
    @Mapping(target = "idServicio", source = "transaccion.idServicio")
    @Mapping(target = "idProducto", source = "transaccion.idProducto")
    @Mapping(target = "servicio", source = "productoCatalogo.servicio")
    @Mapping(target = "producto", source = "productoCatalogo.producto")
    @Mapping(target = "telefono", source = "transaccion.telefonoEnviado")
    @Mapping(target = "referencia", source = "transaccion.referenciaEnviada")
    @Mapping(target = "monto", source = "transaccion.monto")
    @Mapping(target = "comision", source = "transaccion.comision")
    @Mapping(target = "totalCobrado", source = "transaccion.totalCobrado")
    @Mapping(target = "saldoDisponible", source = "saldoDisponible")
    @Mapping(target = "estado", source = "transaccion.estado")
    @Mapping(target = "idTxProveedor", source = "transaccion.idTxProveedor")
    @Mapping(target = "numAutorizacion", source = "transaccion.numAutorizacion")
    @Mapping(target = "pin", source = "transaccion.pinProducto")
    @Mapping(target = "folio", source = "transaccion.folio")
    @Mapping(target = "leyenda", source = "transaccion.leyenda")
    @Mapping(target = "fechaOperacion", source = "transaccion.fechaActualizacion")
    ComprobantePagoResponse toComprobanteResponse(
            TransaccionPago transaccion,
            GestoPagoProducto productoCatalogo,
            String numeroCuenta,
            BigDecimal saldoDisponible,
            String mensajeRespuesta
    );

    @AfterMapping
    default void completarMensajePorDefecto(
            String mensajeRespuesta,
            @MappingTarget ComprobantePagoResponse target) {
        if (target.getCodigo() == null) {
            target.setCodigo(0);
        }
        if (target.getMensaje() == null || target.getMensaje().isBlank()) {
            target.setMensaje(mensajeRespuesta != null ? mensajeRespuesta : "Operación exitosa");
        }
    }
}
