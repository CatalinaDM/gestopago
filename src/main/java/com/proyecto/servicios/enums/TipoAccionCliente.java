package com.proyecto.servicios.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Acción a realizar sobre el ciclo de vida del cliente: BAJA (desactiva cliente y productos) o REACTIVAR (reactiva cliente y productos)")
public enum TipoAccionCliente {
    BAJA,
    REACTIVAR
}
