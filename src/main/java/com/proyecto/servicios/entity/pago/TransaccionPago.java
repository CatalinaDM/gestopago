package com.proyecto.servicios.entity.pago;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.proyecto.servicios.entity.cliente.Cuenta;
import com.proyecto.servicios.enums.EstadoTransaccion;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transacciones_pago")
@Getter
@Setter
public class TransaccionPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_id", nullable = false)
    @JsonIgnore
    private Cuenta cuenta;

    @Column(name = "id_producto", nullable = false)
    private Integer idProducto;

    @Column(name = "id_servicio", nullable = false)
    private Integer idServicio;

    @Column(name = "upc", nullable = false, unique = true, length = 10)
    private String upc;

    @Column(name = "referencia_enviada", length = 120)
    private String referenciaEnviada;

    @Column(name = "telefono_enviado", length = 20)
    private String telefonoEnviado;

    @Column(name = "monto", nullable = false, precision = 15, scale = 2)
    private BigDecimal monto;

    @Column(name = "comision", nullable = false, precision = 15, scale = 2)
    private BigDecimal comision = BigDecimal.ZERO;

    @Column(name = "total_cobrado", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalCobrado;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoTransaccion estado;

    @Column(name = "id_tx_proveedor", length = 100)
    private String idTxProveedor;

    @Column(name = "num_autorizacion", length = 100)
    private String numAutorizacion;

    @Column(name = "pin_producto", length = 255)
    private String pinProducto;

    @Column(name = "folio", length = 100)
    private String folio;

    @Column(name = "leyenda", columnDefinition = "TEXT")
    private String leyenda;

    @Column(name = "codigo_respuesta", length = 20)
    private String codigoRespuesta;

    @Column(name = "mensaje_respuesta", length = 500)
    private String mensajeRespuesta;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @PrePersist
    void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
        this.fechaActualizacion = LocalDateTime.now();
        if (this.comision == null) {
            this.comision = BigDecimal.ZERO;
        }
        if (this.estado == null) {
            this.estado = EstadoTransaccion.PENDIENTE;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.fechaActualizacion = LocalDateTime.now();
    }
}

