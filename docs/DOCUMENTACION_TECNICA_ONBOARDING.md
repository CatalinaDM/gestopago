# Documentación Técnica: Proyecto Integrador - Onboarding de Clientes Personas Físicas

## 1. Introducción y Objetivo
El proyecto implementa un sistema bancario integral de **Onboarding de Clientes Personas Físicas** bajo arquitectura de microservicios / API REST con **Java 17**, **Spring Boot 3**, **Spring Data JPA**, **Flyway**, **MapStruct**, **BCrypt** y **PostgreSQL**.

Permite capturar y validar rigurosamente la información del cliente, generar automáticamente su cuenta bancaria de 10 dígitos con saldo inicial, aprovisionar su usuario de acceso con contraseña cifrada y roles, gestionar sesiones mediante **JWT**, aplicar baja lógica en cascada y aplicar bloqueo de seguridad tras 3 intentos fallidos.

---

## 2. Decisiones de Diseño y Buenas Prácticas

### 2.1. Tipos de Datos y Optimización de Memoria (SQL & JPA)
Siguiendo las mejores prácticas de bases de datos para rendimiento e integridad:
- **Longitud fija (`CHAR(n)`)**: Se emplean para campos con formato exacto e inmutable:
  - `CURP`: `CHAR(18)`
  - `RFC`: `CHAR(13)`
  - `telefono_movil` y `telefono_alternativo`: `CHAR(10)`
  - `numero_cuenta`: `CHAR(10)`
  - `codigo_postal`: `CHAR(5)`
  - `sexo`: `CHAR(1)` (`'H'`, `'M'`, `'X'`)
- **Montos Monetarios (`NUMERIC(15,2)` y `BigDecimal`)**: Garantizan precisión decimal exacta sin los errores de redondeo inherentes a `Double` o `Float`.
- **Estatus Booleano Binario (`activo BOOLEAN NOT NULL DEFAULT TRUE`)**: Simplifica y optimiza el estado en `cuentas` y `usuarios` en lugar de cadenas de texto variables.
- **Roles Numéricos (`rol INTEGER NOT NULL DEFAULT 2`)**:
  - `1` = **ADMIN** (Acceso a paginación, desbloqueo de usuarios, consultas globales).
  - `2` = **CLIENTE** (Rol asignado automáticamente al registrarse).

### 2.2. Estandarización del Campo Sexo (`H`, `M`, `X`)
En lugar de cadenas libres como `"Hombre"` o `"Mujer"`, se estandarizó a 1 caracter:
- `'H'`: Hombre / Masculino
- `'M'`: Mujer / Femenino
- `'X'`: No binario / Prefiero no especificar
- **Validación**: Validado a nivel API con `@Pattern(regexp = "^[HMX]$")` y a nivel BD con `CHECK (sexo IN ('H', 'M', 'X'))`.

### 2.3. Sanitización Declarativa con MapStruct
Para evitar código espagueti con múltiples `if (cadena != null) trim()`, se implementó el componente [StringSanitizer.java](file:///c:/Users/marco/Downloads/10/aplicaciones_web_progresivas/backend/prueba/src/main/java/com/proyecto/servicios/mapper/StringSanitizer.java) con `@Named`:
- `@Named("trim")`: Limpia espacios en blanco.
- `@Named("trimUpper")`: Limpia espacios y convierte a mayúsculas (CURP, RFC, Sexo).
- `@Named("trimLower")`: Limpia espacios y convierte a minúsculas (Email).

---

## 3. Diagrama Entidad-Relación (ER)

```mermaid
erDiagram
    CLIENTES ||--|| DOMICILIOS : "1:1 (reside en)"
    CLIENTES ||--|{ CUENTAS : "1:N (posee)"
    CLIENTES ||--|| USUARIOS : "1:1 (autentica con)"

    CLIENTES {
        int id PK
        varchar(50) nombre
        varchar(50) segundo_nombre
        varchar(50) apellido_paterno
        varchar(50) apellido_materno
        date fecha_nacimiento
        char(18) curp UK
        char(13) rfc UK
        char(1) sexo
        varchar(50) nacionalidad
        varchar(50) estado_civil
        varchar(100) email UK
        char(10) telefono_movil
        char(10) telefono_alternativo
        varchar(100) ocupacion
        varchar(100) empresa
        numeric(15_2) ingreso_mensual
        boolean activo
        timestamp fecha_creacion
        timestamp fecha_actualizacion
    }

    DOMICILIOS {
        int id PK
        int cliente_id FK,UK
        varchar(100) calle
        varchar(20) numero_exterior
        varchar(20) numero_interior
        varchar(100) colonia
        varchar(100) municipio
        varchar(100) estado
        char(5) codigo_postal
        varchar(50) pais
    }

    CUENTAS {
        int id PK
        int cliente_id FK
        char(10) numero_cuenta UK
        numeric(15_2) saldo
        boolean activo
        timestamp fecha_creacion
        timestamp fecha_actualizacion
    }

    USUARIOS {
        int id PK
        int cliente_id FK,UK
        varchar(100) correo UK
        varchar(255) password
        int rol
        int intentos_fallidos
        boolean activo
        timestamp fecha_creacion
        timestamp fecha_actualizacion
    }
```

---

## 4. Seguridad, JWT y Reglas de Bloqueo

### 4.1. Intentos Fallidos y Bloqueo de Cuenta
- **Límite**: Al registrarse, el usuario inicia con `intentos_fallidos = 0` y `activo = true`.
- **Fallo de contraseña**: Cada intento erróneo incrementa el contador (`1/3`, `2/3`).
- **Al 3er fallo consecutivo**:
  - El sistema actualiza `activo = false` e `intentos_fallidos = 3`.
  - Se deniega el acceso con `HTTP 403 FORBIDDEN` y código `AUTH-002`:
    > *"Acceso bloqueado: ha excedido el límite de 3 intentos fallidos de contraseña. Por favor, contacte al administrador para desbloquear su cuenta."*
- **Desbloqueo**: El administrador invoca `PATCH /usuarios/{id}/desbloquear`, restableciendo `activo = true` e `intentos_fallidos = 0`.
- **Éxito**: Un inicio de sesión correcto resetea `intentos_fallidos = 0`.

### 4.2. Baja Lógica en Cascada
Al dar de baja a un cliente mediante `DELETE /clientes/{id}`:
1. `cliente.activo = false`
2. Todas sus cuentas vinculadas pasan a `cuenta.activo = false`.
3. Su usuario de acceso pasa a `usuario.activo = false`.
4. Si el usuario intenta hacer login, la API rechaza el acceso (`HTTP 403 Forbidden`).

---

## 5. Catálogo de Endpoints de la API REST

### 5.1. Autenticación (`/auth`)
| Método | Endpoint | Acceso | Descripción |
| :--- | :--- | :--- | :--- |
| `POST` | `/auth/login` | Público | Autentica con correo y contraseña. Devuelve token JWT, rol y datos de sesión. |

#### Ejemplo Request `/auth/login`:
```json
{
  "correo": "juan.perez@example.com",
  "password": "Password123!"
}
```

#### Ejemplo Response 200 OK:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "usuarioId": 1,
  "clienteId": 10,
  "correo": "juan.perez@example.com",
  "rol": 2
}
```

---

### 5.2. Clientes (`/clientes`)
| Método | Endpoint | Acceso | Descripción |
| :--- | :--- | :--- | :--- |
| `POST` | `/clientes` | Público | **Onboarding completo**: Registra cliente, domicilio, genera cuenta bancaria (10 dígitos) y usuario (`rol = 2`). |
| `GET` | `/clientes/me` | Autenticado | Obtiene la información del cliente asociado al token JWT actual. |
| `GET` | `/clientes` | Autenticado | Lista todos los clientes activos o filtra por `nombre`, `curp`, `rfc`, `email`. |
| `GET` | `/clientes/paginados` | Admin | Consulta paginada (`page`, `size`, `sort`) para paneles administrativos. |
| `GET` | `/clientes/activos` | Autenticado | Lista todos los clientes con `activo = true`. |
| `GET` | `/clientes/{id}` | Autenticado | Obtiene detalle de un cliente por ID. |
| `GET` | `/clientes/curp/{curp}` | Autenticado | Búsqueda parcial de clientes por CURP. |
| `GET` | `/clientes/rfc/{rfc}` | Autenticado | Búsqueda parcial de clientes por RFC. |
| `GET` | `/clientes/correo/{correo}` | Autenticado | Búsqueda parcial de clientes por correo electrónico. |
| `GET` | `/clientes/cuenta/{numeroCuenta}` | Autenticado | Obtiene el cliente titular del número de cuenta. |
| `GET` | `/clientes/rango-fechas?fechaInicio=...&fechaFin=...` | Autenticado | Filtra clientes dados de alta en un rango de fechas (`YYYY-MM-DD`). |
| `GET` | `/clientes/buscar?filtro=texto` | Autenticado | Búsqueda abierta en nombres, CURP, RFC, email o cuenta. |
| `PUT` | `/clientes/{id}` | Autenticado | Actualiza datos personales y de domicilio (CURP y RFC inmutables). |
| `DELETE` | `/clientes/{id}` | Autenticado | **Baja lógica en cascada** (desactiva cliente, cuentas y usuario). |

#### Ejemplo Request `POST /clientes`:
```json
{
  "nombre": "Juan",
  "segundoNombre": "Carlos",
  "apellidoPaterno": "Perez",
  "apellidoMaterno": "Lopez",
  "fechaNacimiento": "1992-08-14",
  "curp": "PELJ920814HDFRN01",
  "rfc": "PELJ920814AB1",
  "sexo": "H",
  "nacionalidad": "Mexicana",
  "estadoCivil": "Soltero",
  "email": "juan.perez@example.com",
  "telefonoMovil": "5512345678",
  "telefonoAlternativo": "5587654321",
  "ocupacion": "Ingeniero de Software",
  "empresa": "Tech Solutions",
  "ingresoMensual": 38000.00,
  "password": "Password123!",
  "domicilio": {
    "calle": "Av. Paseo de la Reforma",
    "numeroExterior": "222",
    "numeroInterior": "Piso 5",
    "colonia": "Juarez",
    "municipio": "Cuauhtemoc",
    "estado": "Ciudad de Mexico",
    "codigoPostal": "06600",
    "pais": "Mexico"
  }
}
```

---

### 5.3. Cuentas (`/cuentas`)
| Método | Endpoint | Acceso | Descripción |
| :--- | :--- | :--- | :--- |
| `GET` | `/cuentas/{numeroCuenta}` | Autenticado | Consulta la cuenta bancaria por su número de 10 dígitos. |
| `GET` | `/cuentas/{numeroCuenta}/saldo` | Autenticado | Consulta el saldo actual y estado booleano de la cuenta. |
| `GET` | `/cuentas/activas` | Autenticado | Lista todas las cuentas activas en el sistema. |

---

### 5.4. Usuarios (`/usuarios`)
| Método | Endpoint | Acceso | Descripción |
| :--- | :--- | :--- | :--- |
| `GET` | `/usuarios/{id}` | Autenticado | Obtiene la información del usuario (ID, correo, rol, intentos fallidos, activo). |
| `PUT` | `/usuarios/{id}/password` | Autenticado | Cambia la contraseña (requiere `passwordActual` y `passwordNuevo` con BCrypt). Valida IDOR. |
| `PATCH` | `/usuarios/{id}/desbloquear` | Admin | Desbloquea una cuenta bloqueada por 3 intentos fallidos (`activo = true`, `intentos = 0`). |

---

## 6. Formato Estructurado de Errores y Validaciones

Cuando una petición no cumple las restricciones (por ejemplo, `POST /clientes` con datos erróneos), [`GlobalExceptionHandler.java`](file:///c:/Users/marco/Downloads/10/aplicaciones_web_progresivas/backend/prueba/src/main/java/com/proyecto/servicios/exception/GlobalExceptionHandler.java) devuelve un JSON estructurado con el desglose exacto de cada campo fallido:

#### Respuesta HTTP 400 Bad Request (`VALIDACION-001`):
```json
{
  "codigo": "VALIDACION-001",
  "mensaje": "El sexo debe ser 'H' (Hombre), 'M' (Mujer) o 'X' (No binario); El RFC para persona física debe cumplir el formato oficial AAAA000000XXX (13 caracteres)",
  "detalles": {
    "sexo": "El sexo debe ser 'H' (Hombre), 'M' (Mujer) o 'X' (No binario)",
    "rfc": "El RFC para persona física debe cumplir el formato oficial AAAA000000XXX (13 caracteres)",
    "telefonoMovil": "El teléfono móvil debe contener exactamente 10 dígitos numéricos"
  },
  "timestamp": "2026-10-06T00:45:00",
  "path": "/clientes"
}
```

### Tabla de Códigos de Excepciones de Negocio
| Código | HTTP Status | Excepción | Motivo |
| :--- | :--- | :--- | :--- |
| `VALIDACION-001` | `400 BAD REQUEST` | `MethodArgumentNotValidException` | Error en campos del DTO (formato, tamaño, regex). |
| `VALIDACION-002` | `400 BAD REQUEST` | `ValidacionException` | El cliente es menor de 18 años. |
| `VALIDACION-003` | `400 BAD REQUEST` | `ValidacionException` | El saldo inicial es negativo. |
| `CLIENTE-002` | `409 CONFLICT` | `CurpDuplicadaException` | La CURP ya se encuentra registrada. |
| `CLIENTE-003` | `409 CONFLICT` | `RfcDuplicadoException` | El RFC ya se encuentra registrado. |
| `CLIENTE-004` | `409 CONFLICT` | `CorreoDuplicadoException` | El correo electrónico ya pertenece a otro cliente/usuario. |
| `CLIENTE-005` | `404 NOT FOUND` | `ClienteNoEncontradoException` | No existe cliente con el ID, CURP o RFC consultado. |
| `CUENTA-001` | `404 NOT FOUND` | `CuentaNoEncontradaException` | No se encontró la cuenta con el número especificado. |
| `AUTH-001` | `404 NOT FOUND` | `UsuarioNoEncontradoException` | Usuario no localizado por ID o correo. |
| `AUTH-002` | `403 FORBIDDEN` | `UsuarioInactivoException` | Usuario inactivo o bloqueado tras 3 intentos fallidos. |
| `AUTH-003` | `401 UNAUTHORIZED` | `CredencialesInvalidasException` | Contraseña incorrecta. |
| `AUTH-004` | `400 BAD REQUEST` | `ContrasenaInvalidaException` | Contraseña actual errónea o nueva contraseña repetida. |

---

## 7. Evidencia y Cobertura de Pruebas Unitarias

Se cuenta con **más de 118 casos de prueba automatizados** ejecutados mediante JUnit 5 y Mockito:
- [ClienteValidationTest.java](file:///c:/Users/marco/Downloads/10/aplicaciones_web_progresivas/backend/prueba/src/test/java/com/proyecto/servicios/validation/ClienteValidationTest.java): Pruebas parametrizadas de todas las restricciones negativas (RFCs de 12/14 dígitos, teléfonos con letras, nombres con números/puntos, contraseñas débiles, etc.).
- [ClienteCicloVidaTest.java](file:///c:/Users/marco/Downloads/10/aplicaciones_web_progresivas/backend/prueba/src/test/java/com/proyecto/servicios/service/Impl/ClienteCicloVidaTest.java): Pruebas de integración de ciclo de vida completo: Registro -> Login exitoso -> Baja lógica -> Rechazo de login post-baja -> Bloqueo al 3er fallo -> Desbloqueo por Administrador.
- [ClienteServiceImplTest.java](file:///c:/Users/marco/Downloads/10/aplicaciones_web_progresivas/backend/prueba/src/test/java/com/proyecto/servicios/service/Impl/ClienteServiceImplTest.java): Pruebas de lógica de servicios, inmutabilidad de CURP/RFC y filtros.
- [AuthServiceImplTest.java](file:///c:/Users/marco/Downloads/10/aplicaciones_web_progresivas/backend/prueba/src/test/java/com/proyecto/servicios/service/Impl/AuthServiceImplTest.java): Pruebas de login y JWT.
- [CuentaServiceImplTest.java](file:///c:/Users/marco/Downloads/10/aplicaciones_web_progresivas/backend/prueba/src/test/java/com/proyecto/servicios/service/Impl/CuentaServiceImplTest.java): Pruebas de consulta de saldos y números de cuenta.
- [UsuarioServiceImplTest.java](file:///c:/Users/marco/Downloads/10/aplicaciones_web_progresivas/backend/prueba/src/test/java/com/proyecto/servicios/service/Impl/UsuarioServiceImplTest.java): Pruebas de cambio de contraseña y desbloqueo.

**Estado de Ejecución**: `./gradlew.bat test` -> **`BUILD SUCCESSFUL (0 fallos, 0 errores)`**.
