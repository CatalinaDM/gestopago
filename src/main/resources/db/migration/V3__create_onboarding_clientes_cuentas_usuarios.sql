-- Migración V3: Tablas para Onboarding de Clientes Personas Físicas, Domicilios, Cuentas y Usuarios

CREATE TABLE IF NOT EXISTS clientes (
    id                      SERIAL PRIMARY KEY,
    nombre                  VARCHAR(50)     NOT NULL,
    segundo_nombre          VARCHAR(50),
    apellido_paterno        VARCHAR(50)     NOT NULL,
    apellido_materno        VARCHAR(50)     NOT NULL,
    fecha_nacimiento        DATE            NOT NULL,
    curp                    CHAR(18)        NOT NULL UNIQUE,
    rfc                     CHAR(13)        NOT NULL UNIQUE,
    sexo                    CHAR(1)         NOT NULL,
    nacionalidad            VARCHAR(50)     NOT NULL,
    estado_civil            VARCHAR(50)     NOT NULL,
    email                   VARCHAR(100)    NOT NULL UNIQUE,
    telefono_movil          CHAR(10)        NOT NULL,
    telefono_alternativo    CHAR(10),
    ocupacion               VARCHAR(100)    NOT NULL,
    empresa                 VARCHAR(100)    NOT NULL,
    ingreso_mensual         NUMERIC(15, 2)  NOT NULL,
    activo                  BOOLEAN         NOT NULL DEFAULT TRUE,
    fecha_creacion          TIMESTAMP       NOT NULL DEFAULT NOW(),
    fecha_actualizacion     TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_clientes_curp_longitud CHECK (length(curp) = 18),
    CONSTRAINT chk_clientes_rfc_longitud CHECK (length(rfc) = 13),
    CONSTRAINT chk_clientes_sexo CHECK (sexo IN ('H', 'M', 'X')),
    CONSTRAINT chk_clientes_telefono_movil_longitud CHECK (length(telefono_movil) = 10),
    CONSTRAINT chk_clientes_ingreso_positivo CHECK (ingreso_mensual > 0)
);

CREATE INDEX IF NOT EXISTS idx_clientes_activo ON clientes(activo);
CREATE INDEX IF NOT EXISTS idx_clientes_fecha_creacion ON clientes(fecha_creacion);

CREATE TABLE IF NOT EXISTS domicilios (
    id                  SERIAL PRIMARY KEY,
    cliente_id          INTEGER         NOT NULL UNIQUE,
    calle               VARCHAR(100)    NOT NULL,
    numero_exterior     VARCHAR(20)     NOT NULL,
    numero_interior     VARCHAR(20),
    colonia             VARCHAR(100)    NOT NULL,
    municipio           VARCHAR(100)    NOT NULL,
    estado              VARCHAR(100)    NOT NULL,
    codigo_postal       CHAR(5)         NOT NULL,
    pais                VARCHAR(50)     NOT NULL,

    CONSTRAINT fk_domicilios_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id) ON DELETE CASCADE,
    CONSTRAINT chk_domicilios_cp_longitud CHECK (length(codigo_postal) = 5)
);

CREATE TABLE IF NOT EXISTS cuentas (
    id                  SERIAL PRIMARY KEY,
    cliente_id          INTEGER         NOT NULL,
    numero_cuenta       CHAR(10)        NOT NULL UNIQUE,
    saldo               NUMERIC(15, 2)  NOT NULL DEFAULT 0.00,
    activo              BOOLEAN         NOT NULL DEFAULT TRUE,
    fecha_creacion      TIMESTAMP       NOT NULL DEFAULT NOW(),
    fecha_actualizacion TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_cuentas_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id) ON DELETE CASCADE,
    CONSTRAINT chk_cuentas_saldo_no_negativo CHECK (saldo >= 0),
    CONSTRAINT chk_cuentas_numero_cuenta_longitud CHECK (length(numero_cuenta) = 10)
);

CREATE INDEX IF NOT EXISTS idx_cuentas_cliente_id ON cuentas(cliente_id);
CREATE INDEX IF NOT EXISTS idx_cuentas_activo ON cuentas(activo);

CREATE TABLE IF NOT EXISTS usuarios (
    id                  SERIAL PRIMARY KEY,
    cliente_id          INTEGER         UNIQUE,
    correo              VARCHAR(100)    NOT NULL UNIQUE,
    password            VARCHAR(255)    NOT NULL,
    rol                 INTEGER         NOT NULL DEFAULT 2,
    intentos_fallidos   INTEGER         NOT NULL DEFAULT 0,
    activo              BOOLEAN         NOT NULL DEFAULT TRUE,
    fecha_creacion      TIMESTAMP       NOT NULL DEFAULT NOW(),
    fecha_actualizacion TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_usuarios_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id) ON DELETE CASCADE,
    CONSTRAINT chk_usuarios_rol CHECK (rol IN (1, 2)),
    CONSTRAINT chk_usuarios_intentos CHECK (intentos_fallidos >= 0)
);

CREATE INDEX IF NOT EXISTS idx_usuarios_correo ON usuarios(correo);
CREATE INDEX IF NOT EXISTS idx_usuarios_activo ON usuarios(activo);
