-- Migración V7: Tabla de transacciones de pago (sendTx) y movimientos contables de cuenta (cargos y reembolsos)

CREATE TABLE IF NOT EXISTS transacciones_pago (
    id                  SERIAL PRIMARY KEY,
    cuenta_id           INTEGER         NOT NULL,
    id_producto         INTEGER         NOT NULL,
    id_servicio         INTEGER         NOT NULL,
    upc                 VARCHAR(10)     NOT NULL UNIQUE,
    referencia_enviada  VARCHAR(120),
    telefono_enviado    VARCHAR(20),
    monto               NUMERIC(15, 2)  NOT NULL,
    comision            NUMERIC(15, 2)  NOT NULL DEFAULT 0.00,
    total_cobrado       NUMERIC(15, 2)  NOT NULL,
    estado              VARCHAR(20)     NOT NULL,
    id_tx_proveedor     VARCHAR(100),
    num_autorizacion    VARCHAR(100),
    pin_producto        VARCHAR(255),
    folio               VARCHAR(100),
    leyenda             TEXT,
    codigo_respuesta    VARCHAR(20),
    mensaje_respuesta   VARCHAR(500),
    fecha_creacion      TIMESTAMP       NOT NULL DEFAULT NOW(),
    fecha_actualizacion TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_transacciones_pago_cuenta FOREIGN KEY (cuenta_id) REFERENCES cuentas(id),
    CONSTRAINT chk_transacciones_pago_monto_positivo CHECK (monto > 0),
    CONSTRAINT chk_transacciones_pago_comision_no_negativa CHECK (comision >= 0),
    CONSTRAINT chk_transacciones_pago_total_positivo CHECK (total_cobrado > 0),
    CONSTRAINT chk_transacciones_pago_estado CHECK (estado IN ('PENDIENTE', 'EXITOSA', 'FALLIDA', 'EN_REVISION')),
    CONSTRAINT chk_transacciones_pago_upc_longitud CHECK (length(upc) = 10)
);

CREATE INDEX IF NOT EXISTS idx_transacciones_pago_cuenta_id ON transacciones_pago(cuenta_id);
CREATE INDEX IF NOT EXISTS idx_transacciones_pago_estado_fecha ON transacciones_pago(estado, fecha_creacion);

CREATE TABLE IF NOT EXISTS movimientos_cuenta (
    id                  SERIAL PRIMARY KEY,
    cuenta_id           INTEGER         NOT NULL,
    transaccion_pago_id INTEGER         NOT NULL,
    tipo_movimiento     VARCHAR(30)     NOT NULL,
    monto               NUMERIC(15, 2)  NOT NULL,
    saldo_anterior      NUMERIC(15, 2)  NOT NULL,
    saldo_posterior     NUMERIC(15, 2)  NOT NULL,
    descripcion         VARCHAR(255)    NOT NULL,
    fecha_creacion      TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_movimientos_cuenta_cuenta FOREIGN KEY (cuenta_id) REFERENCES cuentas(id),
    CONSTRAINT fk_movimientos_cuenta_tx FOREIGN KEY (transaccion_pago_id) REFERENCES transacciones_pago(id),
    CONSTRAINT chk_movimientos_cuenta_tipo CHECK (tipo_movimiento IN ('CARGO_PAGO', 'REEMBOLSO_PAGO')),
    CONSTRAINT chk_movimientos_cuenta_monto_positivo CHECK (monto > 0)
);

CREATE INDEX IF NOT EXISTS idx_movimientos_cuenta_cuenta_id ON movimientos_cuenta(cuenta_id);
CREATE INDEX IF NOT EXISTS idx_movimientos_cuenta_tx_id ON movimientos_cuenta(transaccion_pago_id);

