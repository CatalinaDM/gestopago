package com.proyecto.servicios.model.pago;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.proyecto.servicios.model.GenericResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AutorizacionNipResponse extends GenericResponse {

    private String nipTransaccional;
    private String mascaraVisual;
}

