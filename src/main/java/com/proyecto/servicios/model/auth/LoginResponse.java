package com.proyecto.servicios.model.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String token;
    @Builder.Default
    private String tokenType = "Bearer";
    private Integer usuarioId;
    private Integer clienteId;
    private String correo;
    private Integer rol;
}
