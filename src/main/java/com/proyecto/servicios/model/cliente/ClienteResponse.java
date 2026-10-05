package com.proyecto.servicios.model.cliente;

import com.proyecto.servicios.model.cuenta.CuentaResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClienteResponse {

    private Integer id;
    private String nombre;
    private String segundoNombre;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private String nombreCompleto;
    private LocalDate fechaNacimiento;
    private String curp;
    private String rfc;
    private String sexo;
    private String nacionalidad;
    private String estadoCivil;
    private String email;
    private String telefonoMovil;
    private String telefonoAlternativo;
    private String ocupacion;
    private String empresa;
    private BigDecimal ingresoMensual;
    private Boolean activo;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
    private DomicilioDTO domicilio;
    private List<CuentaResponse> cuentas;
}
