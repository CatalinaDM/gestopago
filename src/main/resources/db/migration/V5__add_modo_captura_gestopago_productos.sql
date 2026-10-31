-- Migración V5: Agregar columna modo_captura al catálogo local de productos GestoPago

ALTER TABLE gestopago_productos
    ADD COLUMN IF NOT EXISTS modo_captura VARCHAR(30) NOT NULL DEFAULT 'NO_SOPORTADO';

-- Clasificar registros existentes si ya hubiera datos cargados
UPDATE gestopago_productos
SET modo_captura = CASE
    WHEN id_cat_tipo_servicio IN (10, 11) THEN 'PREPAGO_PIN'
    WHEN tipo_front = 1 THEN 'RECARGA'
    WHEN tipo_front = 2 AND LOWER(COALESCE(tipo_referencia, '')) = 'b' THEN 'PAGO_REFERENCIA'
    WHEN tipo_front = 2 AND LOWER(COALESCE(tipo_referencia, '')) = 'c' THEN 'PAGO_CODIGO_BARRAS'
    ELSE 'NO_SOPORTADO'
END;

CREATE INDEX IF NOT EXISTS idx_gestopago_productos_modo_captura ON gestopago_productos(modo_captura);
CREATE INDEX IF NOT EXISTS idx_gestopago_productos_id_producto ON gestopago_productos(id_producto);
CREATE INDEX IF NOT EXISTS idx_gestopago_productos_categoria ON gestopago_productos(id_cat_tipo_servicio);

