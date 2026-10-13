# Puesto de Frutas

API REST para gestionar el inventario de un puesto de frutas (CRUD de frutas).

## Tecnologías

- Java 17
- Spring Boot 4 (Spring Web MVC, Spring Data JPA, Validation)
- MySQL 8.4 (con Docker)
- H2 en memoria (solo para los tests)
- Maven (incluye Maven Wrapper, no hace falta instalarlo)

## Requisitos

- JDK 17 o superior
- Docker Desktop (o un MySQL 8 instalado localmente)
- Opcional: un cliente de base de datos como DBeaver

## Puesta en marcha

### 1. Variables de entorno

```bash
cp .env.example .env          # Windows (cmd): copy .env.example .env
```

Edita `.env` y define al menos `DB_PASSWORD`. El archivo `.env` es local y no se
sube al repositorio; `.env.example` es la plantilla.

Lo leen tanto Docker Compose (para crear MySQL) como Spring Boot (para
conectarse), así que basta con configurarlo una vez. Si una variable también
está definida en el sistema, gana la del sistema.

| Variable      | Descripción         | Valor por defecto |
| ------------- | ------------------- | ----------------- |
| `DB_HOST`     | Host de MySQL       | `localhost`       |
| `DB_PORT`     | Puerto de MySQL     | `3306`            |
| `DB_NAME`     | Nombre de la base   | `puesto_frutas`   |
| `DB_USER`     | Usuario de MySQL    | `root`            |
| `DB_PASSWORD` | Contraseña de MySQL | (obligatoria)     |

> Si el puerto 3306 ya está ocupado (por ejemplo, por otro MySQL), cambia
>
> puerto y la aplicación se conectará a él, porque ambos leen el mismo valor.

### 2. Levantar MySQL con Docker

Con Docker Desktop en ejecución:

```bash
docker compose up -d
docker compose ps         # esperar a que el estado sea "healthy"
```

Esto crea el contenedor `puesto-frutas-mysql` con la base `puesto_frutas`. Los
datos se guardan en el volumen `mysql_data`, así que no se pierden al detener o
recrear el contenedor.

**Sin Docker (MySQL local):** crea la base con `CREATE DATABASE puesto_frutas;`
y ajusta las variables de `.env` a tu instalación.

### 3. Ejecutar la aplicación

Desde la raíz del proyecto (ahí es donde la aplicación busca el `.env`):

```bash
./mvnw spring-boot:run        # Windows (cmd): mvnw.cmd spring-boot:run
```

No hace falta exportar variables: la aplicación carga el `.env` sola
(`spring.config.import` en `application.properties`).

La API queda en `http://localhost:8080/api/fruits`. Al arrancar, la aplicación
ejecuta `schema.sql` y crea la tabla `fruit` si no existe.

### 4. Probar con Postman

En `postman/` están la colección (`fruit-stand.postman_collection.json`) y el
entorno local (`fruit-stand.local.postman_environment.json`, con
`baseUrl=http://localhost:8080`). Se pueden importar en Postman o correr desde
la terminal con la aplicación levantada:

```bash
postman collection run postman/fruit-stand.postman_collection.json \
  -e postman/fruit-stand.local.postman_environment.json
```

La colección recorre el CRUD completo y los casos de error, y se puede correr
varias veces sobre la misma base.

### Si no arranca

| Síntoma                         | Causa y solución                                                                                                                                                                                                  |
| ------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `Communications link failure`   | MySQL no está corriendo (abrir Docker Desktop y `docker compose up -d`), o la aplicación no encontró el `.env` (no existe o se arrancó fuera de la raíz del proyecto) y está usando el puerto por defecto `3306`. |
| `Access denied for user 'root'` | `DB_PASSWORD` no coincide con la del contenedor (ver la nota de la sección Esquema).                                                                                                                              |
| `Port 8080 was already in use`  | Otra aplicación usa el puerto: cerrarla o arrancar con `--server.port=8081`.                                                                                                                                      |

## Base de datos

### Esquema

El esquema está en `src/main/resources/schema.sql` y se ejecuta en cada
arranque (`CREATE TABLE IF NOT EXISTS`). Hibernate está en modo `validate`:
solo comprueba que las entidades coincidan con las tablas y nunca las modifica.

Tabla `fruit`:

| Columna    | Tipo            | Restricciones                                    |
| ---------- | --------------- | ------------------------------------------------ |
| `id`       | `BIGINT`        | Clave primaria, autoincremental                  |
| `name`     | `VARCHAR(100)`  | Obligatorio, único                               |
| `price`    | `DECIMAL(10,2)` | Obligatorio, mayor o igual que 0                 |
| `quantity` | `DECIMAL(10,3)` | Obligatorio, mayor o igual que 0, por defecto 0  |
| `unit`     | `VARCHAR(20)`   | Obligatorio: `PIECE` o `KG`                      |
| `active`   | `BOOLEAN`       | Obligatorio, por defecto `TRUE` (borrado lógico) |
| `version`  | `BIGINT`        | Obligatorio, por defecto 0 (bloqueo optimista)   |

> Como el script usa `IF NOT EXISTS`, los cambios en `schema.sql` no se aplican
>
> `docker compose down -v`) para que se vuelva a crear.
>
> Lo mismo pasa con la contraseña: MySQL toma `DB_PASSWORD` solo la primera vez
>
> volumen con `docker compose down -v` (borra los datos).

### Conexión con un cliente (DBeaver)

1. Nueva conexión → MySQL.
2. Host `localhost`, puerto el de `DB_PORT` en tu `.env`, base `puesto_frutas`,

   usuario `root` y la contraseña de tu `.env`.
3. En la pestaña *Driver properties* poner `allowPublicKeyRetrieval=true` y

   `useSSL=false` (si no, MySQL 8 responde "Public Key Retrieval is not allowed").

La tabla `fruit` aparece después de arrancar la aplicación por primera vez.

### Consola de MySQL

```bash
docker exec -it puesto-frutas-mysql mysql -uroot -p
```

## Comandos útiles de Docker

| Comando                     | Qué hace                                    |
| --------------------------- | ------------------------------------------- |
| `docker compose up -d`      | Crea y arranca MySQL en segundo plano       |
| `docker compose ps`         | Muestra el estado del contenedor            |
| `docker compose logs mysql` | Muestra el log de MySQL                     |
| `docker compose stop`       | Detiene MySQL conservando los datos         |
| `docker compose start`      | Vuelve a arrancar MySQL                     |
| `docker compose down`       | Borra el contenedor; los datos se conservan |
| `docker compose down -v`    | Borra el contenedor **y los datos**         |

## Tests

Los tests usan una base H2 en memoria en modo MySQL
(`src/test/resources/application.properties`), así que no necesitan Docker:

```bash
./mvnw test
```

## Flujo de trabajo

- `main`: versión estable.
- `develop`: integración; protegida, requiere 1 aprobación en el PR.
- `feature/*`: una rama por funcionalidad, con PR hacia `develop`.