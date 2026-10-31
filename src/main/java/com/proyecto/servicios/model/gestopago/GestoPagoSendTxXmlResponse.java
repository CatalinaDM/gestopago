package com.proyecto.servicios.model.gestopago;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;

import java.math.BigDecimal;

@Data
@XmlRootElement(name = "RESPONSE")
@XmlAccessorType(XmlAccessType.FIELD)
public class GestoPagoSendTxXmlResponse {

    @XmlElement(name = "MENSAJE")
    private MensajeDetalle mensaje;

    @XmlElement(name = "CODIGO")
    private String codigo;

    @XmlElement(name = "CODIGO_MENSAJE")
    private String codigoMensaje;

    @XmlElement(name = "TEXTO")
    private String texto;

    @XmlElement(name = "ID_TX")
    private String idTx;

    @XmlElement(name = "NUM_AUTORIZACION")
    private String numAutorizacion;

    @XmlElement(name = "SALDO")
    private String saldo;

    @XmlElement(name = "COMISION")
    private String comision;

    @XmlElement(name = "SALDO_F")
    private String saldoF;

    @XmlElement(name = "COMISION_F")
    private String comisionF;

    @XmlElement(name = "FECHA")
    private String fecha;

    @XmlElement(name = "MONTO")
    private String monto;

    @XmlElement(name = "REFERENCIA")
    private String referencia;

    @XmlElement(name = "PIN")
    private String pin;

    @XmlElement(name = "ALFILER")
    private String alfiler;

    @XmlElement(name = "FOLIO")
    private String folio;

    @XmlElement(name = "LEYENDA")
    private String leyenda;

    @XmlElement(name = "legend")
    private String legend;

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class MensajeDetalle {

        @XmlElement(name = "CODIGO")
        private String codigo;

        @XmlElement(name = "TEXTO")
        private String texto;

        @XmlElement(name = "REFERENCIA")
        private String referencia;

        @XmlElement(name = "PIN")
        private String pin;

        @XmlElement(name = "ALFILER")
        private String alfiler;

        @XmlElement(name = "FOLIO")
        private String folio;

        @XmlElement(name = "LEYENDA")
        private String leyenda;

        @XmlElement(name = "legend")
        private String legend;
    }

    public String obtenerCodigoEfectivo() {
        if (codigo != null && !codigo.isBlank()) {
            return codigo.trim();
        }
        if (codigoMensaje != null && !codigoMensaje.isBlank()) {
            return codigoMensaje.trim();
        }
        if (mensaje != null && mensaje.getCodigo() != null && !mensaje.getCodigo().isBlank()) {
            return mensaje.getCodigo().trim();
        }
        return null;
    }

    public String obtenerTextoEfectivo() {
        if (texto != null && !texto.isBlank()) {
            return texto.trim();
        }
        if (mensaje != null && mensaje.getTexto() != null && !mensaje.getTexto().isBlank()) {
            return mensaje.getTexto().trim();
        }
        return null;
    }

    public String obtenerPinEfectivo() {
        if (pin != null && !pin.isBlank()) {
            return pin.trim();
        }
        if (alfiler != null && !alfiler.isBlank()) {
            return alfiler.trim();
        }
        if (mensaje != null) {
            if (mensaje.getPin() != null && !mensaje.getPin().isBlank()) {
                return mensaje.getPin().trim();
            }
            if (mensaje.getAlfiler() != null && !mensaje.getAlfiler().isBlank()) {
                return mensaje.getAlfiler().trim();
            }
        }
        return null;
    }

    public String obtenerFolioEfectivo() {
        if (folio != null && !folio.isBlank()) {
            return folio.trim();
        }
        if (mensaje != null && mensaje.getFolio() != null && !mensaje.getFolio().isBlank()) {
            return mensaje.getFolio().trim();
        }
        return null;
    }

    public String obtenerLeyendaEfectiva() {
        if (leyenda != null && !leyenda.isBlank()) {
            return leyenda.trim();
        }
        if (legend != null && !legend.isBlank()) {
            return legend.trim();
        }
        if (mensaje != null) {
            if (mensaje.getLeyenda() != null && !mensaje.getLeyenda().isBlank()) {
                return mensaje.getLeyenda().trim();
            }
            if (mensaje.getLegend() != null && !mensaje.getLegend().isBlank()) {
                return mensaje.getLegend().trim();
            }
        }
        return null;
    }

    /**
     * Convierte el SALDO del proveedor a BigDecimal soportando notación científica (p. ej. "1.5E4").
     */
    public BigDecimal obtenerSaldoDecimal() {
        if (saldo == null || saldo.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(saldo.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

