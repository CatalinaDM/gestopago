# Documentacion tecnica
## Integracion GestoPago, catalogo local, PostgreSQL y Redis

## 1. Proposito de la solucion

El backend integra el servicio externo GestoPago para obtener el catalogo autorizado de productos y servicios. La aplicacion no expone GestoPago directamente al frontend. En su lugar, mantiene una copia local del catalogo en PostgreSQL y utiliza Redis como cache para responder rapidamente las consultas frecuentes.

La solucion separa dos responsabilidades:

1. Actualizacion del catalogo: se ejecuta de forma periodica y controlada contra GestoPago.
2. Consulta del catalogo: atiende las solicitudes del frontend usando Redis y PostgreSQL como respaldo.

Esta separacion evita realizar una llamada a GestoPago por cada peticion del usuario y reduce la carga sobre el servicio externo.

## 2. Arquitectura implementada

```text
                         Actualizacion programada
                                      |
                                      v
+----------------+       +-------------------------+
| GestoPago      | ----> | GestoPagoProductClient |
| autenticacion  |       +------------+------------+
+-------+--------+                    |
        |                             v
        |                    +---------------------+
        +------------------> | CatalogoServiceImpl |
                             +----------+----------+
                                        |
                         +--------------+--------------+
                         |                             |
                         v                             v
                 +---------------+             +---------------+
                 | PostgreSQL    |             | Redis/Upstash  |
                 | fuente local |             | cache          |
                 +---------------+             +---------------+

Frontend/Flutter
       |
       v
GET /catalogo/productos
       |
       v
CatalogoController
       |
       v
CatalogoConsultaService
       |
       +---- Redis HIT: devuelve el catalogo
       |
          +---- Redis MISS o caido: consulta PostgreSQL,
                                                          guarda en Redis y devuelve
                                                          |
                                                          +-- PostgreSQL caido: consulta GestoPago,
                                                                 guarda en Redis y devuelve
```

El frontend solo consume el backend. No conoce las credenciales de GestoPago, PostgreSQL ni Redis.

## 3. Capas y responsabilidades

### 3.1 Capa de configuracion

Archivo principal:

```text
src/main/resources/application.properties
```

Contiene las propiedades necesarias para configurar:

- PostgreSQL.
- URL base de GestoPago.
- Identificador del distribuidor.
- Codigo del dispositivo.
- Cron de sincronizacion.
- Timeouts de OpenFeign.
- Conexion a Redis.

Las credenciales se obtienen desde variables de entorno:

```text
DB_USERNAME
DB_PASSWORD
GESTOPAGO_BASE_URL
GESTOPAGO_AUTH_PASSWORD
REDIS_URL
```

Ejemplo de propiedades:

```properties
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
gestopago.auth.url=${GESTOPAGO_BASE_URL:https://gestopago.portalventas.net}
gestopago.product.url=${GESTOPAGO_BASE_URL:https://gestopago.portalventas.net}
gestopago.auth.password=${GESTOPAGO_AUTH_PASSWORD}
spring.data.redis.url=${REDIS_URL}
```

La expresion `${VARIABLE:valor-defecto}` significa que Spring utiliza la variable de entorno si existe y, en caso contrario, utiliza el valor indicado despues de los dos puntos. La contrasena de GestoPago, PostgreSQL y Redis no tiene valor por defecto.

### 3.2 Cliente de autenticacion

Clase:

```text
com.proyecto.servicios.client.GestoPagoAuthClient
```

Responsabilidades:

- Consumir el endpoint de autenticacion de GestoPago.
- Enviar `idDistribuidor`.
- Enviar `codigoDispositivo`.
- Enviar la contrasena configurada.
- Recibir la respuesta con el token.

Endpoint externo utilizado:

```text
POST /sistema/app/jwt-gp/authenticate/
```

El cliente no contiene tokens ni contrasenas fijas.

### 3.3 Servicio de token

Clases:

```text
com.proyecto.servicios.service.GestoPagoTokenService
com.proyecto.servicios.service.Impl.GestoPagoTokenServiceImpl
```

El servicio:

1. Solicita el token a GestoPago.
2. Busca si ya existe un registro para el distribuidor y dispositivo.
3. Actualiza el registro existente o crea uno nuevo.
4. Guarda el token en PostgreSQL.
5. Expone el token activo al servicio de catalogo.

El token nunca se registra en logs. Para consultar productos se construye internamente:

```text
Authorization: Bearer <TOKEN>
```

### 3.4 Cliente de productos

Clase:

```text
com.proyecto.servicios.client.GestoPagoProductClient
```

Consume:

```text
GET /sistema/service/getProductList.do
```

La URL final se construye con:

```text
gestopago.product.url
+
/sistema/service/getProductList.do
```

El resultado se recibe como `String` porque GestoPago devuelve XML. La respuesta no se transforma automaticamente a JSON en Feign; primero se procesa mediante JAXB.

El cliente utiliza un timeout de conexion y lectura de 15 segundos:

```properties
gestopago.productos.timeout-ms=15000
```

### 3.5 DTOs de autenticacion y XML

#### GestoPagoAuthResponse

Representa la respuesta del endpoint de autenticacion. Contiene el token, tipo de token, expiracion, mensaje y estado.

#### GestoPagoProductResponse

Representa el nodo raiz `RESPONSE` del XML. Contiene el mensaje de respuesta y la lista de productos.

#### GestoPagoMensaje

Representa el nodo `MENSAJE`. Contiene:

- `codigo`: codigo funcional devuelto por GestoPago.
- `texto`: descripcion de la operacion.

El codigo funcional considerado exitoso es `01`.

#### GestoPagoProducto

Representa cada elemento XML `producto`. Contiene los atributos del catalogo, como:

- `idServicio`.
- `idProducto`.
- `idCatTipoServicio`.
- `producto`.
- `servicio`.
- `tipoFront`.
- `hasDigitoVerificador`.
- `tipoReferencia`.
- `precio`.
- `showAyuda`.
- `legend`.

Se utiliza JAXB mediante las anotaciones XML de los DTOs.

### 3.6 Mapper de productos

Clase:

```text
com.proyecto.servicios.mapper.GestoPagoProductoMapper
```

Se utiliza MapStruct para convertir el modelo de producto recibido desde el XML
de GestoPago en la entidad `GestoPagoProducto`. Tambien actualiza los campos de
negocio de una entidad existente.

El mapper ignora:

- `id`, porque lo genera PostgreSQL;
- `idServicio` e `idProducto` durante una actualizacion, porque forman la
       identidad del producto.

La comparacion para saber si un producto cambio permanece en
`CatalogoServiceImpl`, porque es una regla de negocio y permite guardar solo
productos nuevos o modificados.

## 4. Actualizacion periodica del catalogo

Clases relacionadas:

```text
com.proyecto.servicios.App
com.proyecto.servicios.service.Impl.CatalogoServiceImpl
```

`App` habilita el scheduling mediante `@EnableScheduling`.

`CatalogoServiceImpl` tiene el metodo `actualizarCatalogo()` anotado con `@Scheduled`.

El cron actual es:

```properties
gestopago.productos.sync.cron=1 58 0 * * *
```

Esto representa todos los dias a las `00:58:01`.

Flujo de actualizacion:

1. Se inicia la ejecucion programada.
2. Se obtiene el token activo desde PostgreSQL.
3. Se construye el encabezado Bearer.
4. Se solicita el XML a GestoPago.
5. Se convierte el XML mediante JAXB.
6. Se verifica que exista `MENSAJE`.
7. Se verifica que el codigo sea `01`.
8. Se verifica que existan productos.
9. Se carga el catalogo actual con una sola consulta `findAll()`.
10. Se crea un `Map` indexado por `idServicio-idProducto`.
11. Se recorren los productos recibidos.
12. Se identifican productos nuevos y modificados.
13. Se detectan productos obsoletos.
14. Se persisten los cambios en PostgreSQL.
15. Se guarda el catalogo completo actualizado en Redis.

## 5. Reglas de negocio del catalogo

La identidad de un producto se define mediante:

```text
(idServicio, idProducto)
```

`idCatTipoServicio` representa una categoria y puede repetirse. Por eso no se utiliza como identificador unico.

Reglas aplicadas:

- Una respuesta sin productos no actualiza el catalogo.
- Si el nuevo catalogo tiene menos elementos que el actual, no se actualiza.
- Si tiene la misma cantidad o mas, se permite la comparacion.
- Los productos nuevos se insertan.
- Los productos modificados se actualizan.
- Los productos sin cambios no se agregan a `saveAll()`.
- Los productos obsoletos se eliminan cuando la actualizacion fue permitida.
- Redis se actualiza despues de procesar PostgreSQL.

La regla de no aceptar un catalogo menor protege contra una respuesta incompleta o un problema temporal del proveedor.

## 6. Persistencia en PostgreSQL

Entidad:

```text
com.proyecto.servicios.entity.gestopago.GestoPagoProducto
```

Repository:

```text
com.proyecto.servicios.repositorys.gestopago.GestoPagoProductoRepository
```

Migracion:

```text
src/main/resources/db/migration/V2__create_gestopago_productos.sql
```

La tabla es:

```text
gestopago_productos
```

Tiene una restriccion unica para:

```text
(id_servicio, id_producto)
```

La estrategia de persistencia evita el problema N+1:

- Se consulta la tabla una sola vez con `findAll()`.
- Se indexan los productos en memoria con un `Map`.
- Se guarda solo lo nuevo o modificado con `saveAll()`.
- No se ejecuta `saveAll()` cuando no hay cambios.

Hibernate tiene configurado batching JDBC:

```properties
spring.jpa.properties.hibernate.jdbc.batch_size=50
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true
```

## 7. Redis y Upstash

Configuracion:

```text
com.proyecto.servicios.config.RedisConfig
```

Servicio de cache:

```text
com.proyecto.servicios.service.CatalogoCacheService
com.proyecto.servicios.service.Impl.CatalogoCacheServiceImpl
```

Dependencia utilizada:

```gradle
implementation 'org.springframework.boot:spring-boot-starter-data-redis'
```

Redis utiliza la clave:

```text
gestopago:productos
```

El valor es la lista completa del catalogo serializada como JSON. Se configuraron serializers para que las claves sean texto y los valores sean JSON.

Redis no reemplaza a PostgreSQL como fuente persistente. Se utiliza como cache de lectura rapida.

Cuando el scheduler actualiza correctamente PostgreSQL, actualiza tambien Redis. Si Redis no esta disponible, el error se registra y la informacion persistente de PostgreSQL no se pierde.

## 8. Consulta local para el frontend

Controller:

```text
com.proyecto.servicios.controller.CatalogoController
```

Endpoint:

```http
GET http://localhost:8081/catalogo/productos
```

Servicios:

```text
com.proyecto.servicios.service.CatalogoConsultaService
com.proyecto.servicios.service.Impl.CatalogoConsultaServiceImpl
```

Flujo de lectura:

```text
1. La peticion llega al backend.
2. Se consulta Redis.
3. Si existe el catalogo, se devuelve inmediatamente.
4. Si Redis devuelve MISS o no esta disponible, se consulta PostgreSQL.
5. Si PostgreSQL responde, el resultado se guarda en Redis y se devuelve.
6. Si PostgreSQL no esta disponible, se consulta GestoPago mediante
       `GET /sistema/service/getProductList.do`.
7. El resultado de GestoPago se guarda en Redis y se devuelve al cliente.
8. Si tambien falla GestoPago, se propaga el error y no se inventa un catalogo.
```

En condiciones normales el endpoint no realiza llamadas a GestoPago: Redis y
PostgreSQL atienden la solicitud. La llamada externa es un fallback de ultimo
recurso y no sustituye la actualizacion programada del catalogo persistente.

## 9. Manejo de errores

Los errores se clasifican mediante codigos operativos para facilitar el monitoreo.

### Errores de catalogo

- `CATALOGO-001`: timeout o error de conexion con GestoPago.
- `CATALOGO-002`: GestoPago rechazo autenticacion con HTTP 401 o 403.
- `CATALOGO-003`: error de PostgreSQL al guardar, actualizar o eliminar.
- `CATALOGO-004`: GestoPago respondio con otro error HTTP.
- `CATALOGO-005`: error general de comunicacion.
- `CATALOGO-006`: XML invalido.
- `CATALOGO-007`: respuesta sin la seccion `MENSAJE`.
- `CATALOGO-008`: respuesta con codigo funcional distinto de `01`.
- `CATALOGO-009`: respuesta valida pero sin productos.
- `CATALOGO-999`: error inesperado.

### Errores de autenticacion

- `AUTH-001`: timeout o error de conexion al renovar token.
- `AUTH-002`: credenciales rechazadas con HTTP 401 o 403.
- `AUTH-003`: otro error HTTP de autenticacion.
- `AUTH-004`: error al guardar el token en PostgreSQL.
- `AUTH-005`: no existe un token activo.
- `AUTH-999`: error inesperado durante la renovacion.

### Errores de Redis

- `REDIS-001`: no se pudo leer el catalogo; se usa PostgreSQL como fallback.
- `REDIS-002`: no se pudo guardar el catalogo en Redis.

### Respuesta de errores para el frontend

Los errores del endpoint `GET /catalogo/productos` se manejan mediante
`GlobalExceptionHandler`. El frontend recibe el codigo HTTP y un cuerpo JSON
con este formato:

```json
{
       "codigo": "CATALOGO-003",
       "mensaje": "No fue posible consultar PostgreSQL ni obtener el catálogo externo"
}
```

Los estados HTTP principales son:

| Situacion | HTTP |
| --- | --- |
| Catalogo obtenido correctamente | `200 OK` |
| PostgreSQL o GestoPago no disponible | `503 Service Unavailable` |
| GestoPago devolvio una respuesta invalida o rechazo la operacion | `502 Bad Gateway` |
| Error inesperado del backend | `500 Internal Server Error` |
| Ruta inexistente | `404 Not Found` |

Los codigos `CATALOGO-*` y `AUTH-*` proporcionan el detalle operativo para que
el frontend pueda actuar de forma especifica. Los errores del scheduler no se
devuelven por HTTP; se registran en los logs porque no pertenecen a una
peticion activa del frontend.

Los bloques de manejo de errores no registran tokens, contrasenas ni el cuerpo
completo de respuestas externas. En errores HTTP se registra el codigo de
estado y en errores tecnicos el tipo de excepcion.

## 10. Logs y monitoreo

Durante la actualizacion del catalogo se registran:

- inicio de la ejecucion;
- respuesta recibida de GestoPago;
- cantidad de productos recibidos;
- comparacion entre catalogo actual y nuevo;
- errores clasificados;
- actualizacion correcta de PostgreSQL y Redis;
- finalizacion de la ejecucion.

El log de finalizacion se ejecuta en un bloque `finally`, por lo que se registra tanto en exito como en error.

No se deben registrar:

- tokens;
- contrasenas;
- URLs con credenciales;
- respuestas completas que puedan contener datos sensibles.

## 11. Pruebas unitarias

Las pruebas se encuentran en:

```text
src/test/java/com/proyecto/servicios/service/Impl
```

### CatalogoServiceImplTest

Cubre:

- respuesta exitosa;
- respuesta sin productos;
- catalogo nuevo menor;
- error de comunicacion;
- XML invalido;
- producto nuevo;
- producto sin cambios;
- producto modificado;
- respuesta sin `MENSAJE` (`CATALOGO-007`);
- codigo funcional rechazado (`CATALOGO-008`);
- respuesta sin productos (`CATALOGO-009`);
- ausencia de token (`AUTH-005`).

### CatalogoCacheServiceImplTest

Cubre:

- lectura HIT desde Redis;
- escritura en Redis;
- Redis no disponible durante la lectura;
- Redis no disponible durante la escritura.

### CatalogoConsultaServiceImplTest

Cubre:

- respuesta desde Redis sin consultar PostgreSQL;
- MISS en Redis;
- fallback a PostgreSQL;
- repoblacion de Redis;
- fallback a GestoPago cuando PostgreSQL no esta disponible.

### GestoPagoProductoMapperTest

Cubre:

- conversion del modelo XML a entidad;
- ignorar el `id` generado por PostgreSQL;
- actualizacion de campos de negocio;
- conservacion de la identidad del producto.

Las pruebas unitarias utilizan Mockito. No requieren una base PostgreSQL real, una instancia real de Redis ni acceso a GestoPago.

## 12. Ejecucion de pruebas

Para ejecutar toda la suite:

```powershell
.\gradlew.bat test
```

Para ejecutar solo las pruebas del sincronizador:

```powershell
.\gradlew.bat test --tests com.proyecto.servicios.service.Impl.CatalogoServiceImplTest
```

Para ejecutar las pruebas del cache:

```powershell
.\gradlew.bat test --tests com.proyecto.servicios.service.Impl.CatalogoCacheServiceImplTest
```

Para ejecutar las pruebas del fallback:

```powershell
.\gradlew.bat test --tests com.proyecto.servicios.service.Impl.CatalogoConsultaServiceImplTest
```

Para ejecutar las pruebas del mapper:

```powershell
.\gradlew.bat test --tests com.proyecto.servicios.mapper.GestoPagoProductoMapperTest
```

El resultado esperado es:

```text
BUILD SUCCESSFUL
```

## 13. Prueba real con Upstash y Postman

Las pruebas unitarias no validan la conexion real. Para una prueba de integracion local se deben configurar las variables de entorno en la misma consola donde se ejecutara Spring Boot.

No se deben incluir credenciales reales en este documento ni en `application.properties`.

Ejemplo de PowerShell:

```powershell
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="TU_PASSWORD_POSTGRES"
$env:GESTOPAGO_BASE_URL="https://gestopago.portalventas.net"
$env:GESTOPAGO_AUTH_PASSWORD="TU_PASSWORD_GESTOPAGO"
$env:REDIS_URL="rediss://default:TU_PASSWORD@TU_HOST:6379"

.\gradlew.bat bootRun
```

Cuando la aplicacion arranque, se prueba el endpoint local desde Postman:

```http
GET http://localhost:8081/catalogo/productos
```

La primera consulta funciona asi:

```text
Redis MISS
-> PostgreSQL
-> guardar en Redis
-> responder catalogo
```

Las siguientes consultas funcionan asi:

```text
Redis HIT
-> responder catalogo
```

Si Redis no tiene datos y PostgreSQL no esta disponible, se utiliza el ultimo
recurso:

```text
Redis MISS
-> PostgreSQL no disponible
-> autenticar u obtener token de respaldo
-> GET /sistema/service/getProductList.do
-> guardar en Redis
-> responder catalogo
```

En condiciones normales la sincronizacion con GestoPago ocurre mediante el
scheduler configurado. La consulta externa desde el endpoint solo se ejecuta
como fallback cuando Redis y PostgreSQL no pueden atender la solicitud.

## 14. Breve descripción de las decisiones técnicas tomadas

### OpenFeign

Se utiliza OpenFeign para separar la comunicacion HTTP externa de la logica de negocio y mantener clientes declarativos.

### JAXB

Se utiliza JAXB porque GestoPago entrega XML. Los DTOs tienen anotaciones que representan la estructura de `RESPONSE`, `MENSAJE` y `PRODUCTOS`.

### PostgreSQL

PostgreSQL es la fuente local persistente. Permite conservar el catalogo aunque Redis no este disponible.

### Redis

Redis se utiliza como cache remoto de lectura rapida para evitar consultas repetitivas a PostgreSQL.

### Redis como primera opcion

Las consultas frecuentes no deben llegar siempre a PostgreSQL. Por eso se intenta primero Redis y se utiliza PostgreSQL unicamente como fallback.

Si tambien falla PostgreSQL, el servicio consulta GestoPago como ultimo recurso
y guarda la respuesta en Redis. Esta respuesta no se persiste en PostgreSQL
porque la base de datos no esta disponible.

### MapStruct

MapStruct genera la implementacion de `GestoPagoProductoMapper` durante la
compilacion. Esto evita asignaciones manuales repetitivas en el servicio, pero
no reemplaza las validaciones ni la comparacion de cambios del catalogo.

### Scheduler

La consulta a GestoPago se limita a una ejecucion programada. Esto evita realizar llamadas externas por cada usuario y respeta los limites del proveedor.

### Map en memoria

El uso de un `Map` evita buscar cada producto individualmente en PostgreSQL y elimina el problema N+1 durante la sincronizacion.

### SaveAll y batching

Se utiliza `saveAll()` unicamente para productos nuevos o modificados. Las propiedades de Hibernate permiten agrupar operaciones JDBC cuando se procesan muchos registros.

### Seguridad

Las credenciales se extraen del entorno. El archivo `.env.example` documenta los nombres esperados, pero no contiene valores reales. Las credenciales expuestas anteriormente deben revocarse y regenerarse en el proveedor correspondiente.


Flutter no consulta GestoPago directamente, Redis no se utiliza para llamar al proveedor externo y PostgreSQL funciona como persistencia y fallback cuando Redis no esta disponible.
