package com.proyecto.servicios.entity.gestopago;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "gestopago_productos")
@Getter
@Setter
public class GestoPagoProducto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "id_servicio", nullable = false)
    private Integer idServicio;

    @Column(name = "id_producto", nullable = false)
    private Integer idProducto;

    @Column(name = "id_cat_tipo_servicio")
    private Integer idCatTipoServicio;

    @Column(name = "producto")
    private String producto;

    @Column(name = "servicio")
    private String servicio;

    @Column(name = "tipo_front")
    private Integer tipoFront;

    @Column(name = "has_digito_verificador")
    private Boolean hasDigitoVerificador;

    @Column(name = "tipo_referencia")
    private String tipoReferencia;

    @Column(name = "precio")
    private String precio;

    @Column(name = "show_ayuda")
    private Boolean showAyuda;

    @Column(name = "legend", columnDefinition = "TEXT")
    private String legend;
}