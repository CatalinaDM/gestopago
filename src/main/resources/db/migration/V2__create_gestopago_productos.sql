CREATE TABLE gestopago_productos (
    id SERIAL PRIMARY KEY,

    id_servicio INTEGER NOT NULL,
    id_producto INTEGER NOT NULL,
    id_cat_tipo_servicio INTEGER,

    producto VARCHAR(255),
    servicio VARCHAR(255),

    tipo_front INTEGER,

    has_digito_verificador BOOLEAN,

    tipo_referencia VARCHAR(50),

    precio VARCHAR(50),

    show_ayuda BOOLEAN,

    legend TEXT,

    CONSTRAINT uk_gestopago_producto_servicio
        UNIQUE (id_servicio, id_producto)
);