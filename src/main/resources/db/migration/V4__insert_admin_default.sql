-- Migración V4: Insertar Usuario Administrador por Defecto
-- Correo: admin@banco.com
-- Contraseña plana: Admin123! (Cifrada con BCrypt)
-- Rol: 1 (Administrador)

INSERT INTO usuarios (cliente_id, correo, password, rol, intentos_fallidos, activo, fecha_creacion, fecha_actualizacion)
VALUES (
    NULL,
    'admin@banco.com',
    '$2a$10$0p8g6E/btl48HAmaHdI9/OF8CIJkujmfD.6caW6X.3YrPbZSWEiEW',
    1,
    0,
    TRUE,
    NOW(),
    NOW()
)
ON CONFLICT (correo) DO NOTHING;
