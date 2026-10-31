package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.gestopago.GestoPagoProducto;
import com.proyecto.servicios.enums.ModoCaptura;
import com.proyecto.servicios.model.catalogo.ProductoFormularioResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class CatalogoClasificadorComponent {

    private static final Map<Integer, String> NOMBRES_CATEGORIAS = Map.ofEntries(
            Map.entry(1, "Tiempo Aire"),
            Map.entry(2, "Paquetes y Datos Móviles"),
            Map.entry(3, "Televisión y Streaming"),
            Map.entry(4, "Luz y Gas"),
            Map.entry(5, "Agua y Servicios Municipales"),
            Map.entry(6, "Telefonía Fija e Internet"),
            Map.entry(7, "Gobierno e Impuestos"),
            Map.entry(8, "Ventas por Catálogo"),
            Map.entry(9, "Servicios Financieros"),
            Map.entry(10, "Entretenimiento y Pines"),
            Map.entry(11, "Tarjetas de Regalo y Juegos"),
            Map.entry(12, "Transporte y Peajes"),
            Map.entry(15, "Pago de Servicios")
    );

    public ModoCaptura clasificar(GestoPagoProducto producto) {
        if (producto == null) {
            return ModoCaptura.NO_SOPORTADO;
        }
        return calcularModoCaptura(
                producto.getTipoFront(),
                producto.getIdCatTipoServicio(),
                producto.getTipoReferencia()
        );
    }

    public ModoCaptura clasificar(com.proyecto.servicios.model.gestopago.GestoPagoProducto productoDto) {
        if (productoDto == null) {
            return ModoCaptura.NO_SOPORTADO;
        }
        return calcularModoCaptura(
                productoDto.getTipoFront(),
                productoDto.getIdCatTipoServicio(),
                productoDto.getTipoReferencia()
        );
    }

    public static ModoCaptura calcularModoCaptura(Integer tipoFront, Integer idCatTipoServicio, String tipoReferencia) {
        if (tipoFront != null && (tipoFront == 4 || tipoFront == 5 || tipoFront == 30 || tipoFront == 31 || tipoFront == 40)) {
            return ModoCaptura.NO_SOPORTADO;
        }

        if (idCatTipoServicio != null && (idCatTipoServicio == 10 || idCatTipoServicio == 11)) {
            return ModoCaptura.PREPAGO_PIN;
        }

        if (tipoFront == null) {
            return ModoCaptura.NO_SOPORTADO;
        }

        if (tipoFront == 1) {
            return ModoCaptura.RECARGA;
        }

        if (tipoFront == 2) {
            String ref = tipoReferencia != null ? tipoReferencia.trim().toLowerCase() : "";
            if ("b".equals(ref)) {
                return ModoCaptura.PAGO_REFERENCIA;
            }
            if ("c".equals(ref)) {
                return ModoCaptura.PAGO_CODIGO_BARRAS;
            }
            return ModoCaptura.NO_SOPORTADO;
        }

        return ModoCaptura.NO_SOPORTADO;
    }

    public ProductoFormularioResponse toProductoFormularioResponse(GestoPagoProducto producto) {
        ModoCaptura modo = producto.getModoCaptura() != null && producto.getModoCaptura() != ModoCaptura.NO_SOPORTADO
                ? producto.getModoCaptura()
                : clasificar(producto);

        boolean requiereTelefono = modo == ModoCaptura.RECARGA;
        boolean requiereReferencia = modo == ModoCaptura.PAGO_REFERENCIA || modo == ModoCaptura.PAGO_CODIGO_BARRAS;
        boolean requiereMonto = modo == ModoCaptura.PAGO_REFERENCIA || modo == ModoCaptura.PAGO_CODIGO_BARRAS;
        boolean montoFijo = modo == ModoCaptura.RECARGA || modo == ModoCaptura.PREPAGO_PIN;

        String etiquetaCampo = switch (modo) {
            case RECARGA -> "Número telefónico (10 dígitos)";
            case PAGO_REFERENCIA -> "Referencia de pago";
            case PAGO_CODIGO_BARRAS -> "Código de barras";
            case PREPAGO_PIN -> "Entrega de PIN electrónico";
            case NO_SOPORTADO -> "No disponible";
        };

        return ProductoFormularioResponse.builder()
                .idProducto(producto.getIdProducto())
                .nombre(producto.getProducto())
                .precio(parsearPrecio(producto.getPrecio()))
                .modoCaptura(modo)
                .requiereTelefono(requiereTelefono)
                .requiereReferencia(requiereReferencia)
                .requiereMonto(requiereMonto)
                .montoFijo(montoFijo)
                .etiquetaCampo(etiquetaCampo)
                .leyenda(producto.getLegend())
                .build();
    }

    public String obtenerNombreCategoria(Integer idCatTipoServicio) {
        if (idCatTipoServicio == null) {
            return "Otros Servicios";
        }
        return NOMBRES_CATEGORIAS.getOrDefault(idCatTipoServicio, "Categoría " + idCatTipoServicio);
    }

    public BigDecimal parsearPrecio(String precioStr) {
        if (precioStr == null || precioStr.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(precioStr.trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}

