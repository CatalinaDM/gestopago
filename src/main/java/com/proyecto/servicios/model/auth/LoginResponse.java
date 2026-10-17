package com.proyecto.servicios.model.auth;

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
public class LoginResponse extends GenericResponse {

    private String token;
    @Builder.Default
    private String tokenType = "Bearer";
    private Integer usuarioId;
    private Integer clienteId;
    private String correo;
    private Integer rol;
}
