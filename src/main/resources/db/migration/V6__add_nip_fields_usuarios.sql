-- Migración V6: Campos para NIP transaccional cifrado con BCrypt y control de intentos en usuarios

ALTER TABLE usuarios
    ADD COLUMN IF NOT EXISTS nip_hash VARCHAR(255),
    ADD COLUMN IF NOT EXISTS intentos_fallidos_nip INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS nip_bloqueado_hasta TIMESTAMP,
    ADD COLUMN IF NOT EXISTS fecha_actualizacion_nip TIMESTAMP;

ALTER TABLE usuarios
    ADD CONSTRAINT chk_usuarios_intentos_nip CHECK (intentos_fallidos_nip >= 0);

