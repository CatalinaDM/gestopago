package com.proyecto.servicios.model.cliente;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClienteAccionRequest {

    @NotNull(message = "El id del cliente es obligatorio")
    @io.swagger.v3.oas.annotations.media.Schema(description = "Identificador único del cliente", example = "101", requiredMode = io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED)
    private Integer id;

    @NotNull(message = "La acción es obligatoria: BAJA o REACTIVAR")
    @io.swagger.v3.oas.annotations.media.Schema(description = "Acción a ejecutar: BAJA para desactivar, REACTIVAR para reactivar", example = "BAJA", requiredMode = io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED)
    private com.proyecto.servicios.enums.TipoAccionCliente accion;

    public ClienteAccionRequest(Integer id) {
        this.id = id;
    }
}
