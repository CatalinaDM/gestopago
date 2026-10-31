package com.proyecto.servicios.entity.pago;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.proyecto.servicios.entity.cliente.Cuenta;
import com.proyecto.servicios.enums.TipoMovimientoCuenta;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "movimientos_cuenta")
@Getter
@Setter
public class MovimientoCuenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_id", nullable = false)
    @JsonIgnore
    private Cuenta cuenta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaccion_pago_id", nullable = false)
    @JsonIgnore
    private TransaccionPago transaccionPago;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_movimiento", nullable = false, length = 30)
    private TipoMovimientoCuenta tipoMovimiento;

    @Column(name = "monto", nullable = false, precision = 15, scale = 2)
    private BigDecimal monto;

    @Column(name = "saldo_anterior", nullable = false, precision = 15, scale = 2)
    private BigDecimal saldoAnterior;

    @Column(name = "saldo_posterior", nullable = false, precision = 15, scale = 2)
    private BigDecimal saldoPosterior;

    @Column(name = "descripcion", nullable = false, length = 255)
    private String descripcion;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
    }
}

