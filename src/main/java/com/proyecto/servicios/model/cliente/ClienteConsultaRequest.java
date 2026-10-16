package com.proyecto.servicios.model.cliente;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClienteConsultaRequest {

    // Identificador único (para consulta puntual por ID)
    private Integer id;

    // Filtros de búsqueda específicos
    private String curp;
    private String rfc;
    private String email;
    private String numeroCuenta;

    // Filtro general de texto (nombre, apellido, curp, etc.)
    private String filtro;

    // Filtro por estado activo (null = todos, true = solo activos, false = solo inactivos)
    private Boolean soloActivos;

    // Rango de fechas de registro
    private LocalDate fechaInicio;
    private LocalDate fechaFin;

    // Paginación opcional
    private Integer pagina;
    private Integer tamano;
}
