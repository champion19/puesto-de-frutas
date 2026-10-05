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
cp .env.example .env
```

Edita `.env` y define al menos `DB_PASSWORD`. El archivo `.env` es local y no se
sube al repositorio; `.env.example` es la plantilla.

| Variable      | Descripción         | Valor por defecto |
| ------------- | ------------------- | ----------------- |
| `DB_HOST`     | Host de MySQL       | `localhost`       |
| `DB_PORT`     | Puerto de MySQL     | `3306`            |
| `DB_NAME`     | Nombre de la base   | `puesto_frutas`   |
| `DB_USER`     | Usuario de MySQL    | `root`            |
| `DB_PASSWORD` | Contraseña de MySQL | (obligatoria)     |

> Si el puerto 3306 ya está ocupado (por ejemplo, por otro MySQL), cambia
> `DB_PORT` en `.env` (p. ej. `3307`). Docker Compose y la aplicación usan ese
> mismo valor.

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

Spring Boot no lee el archivo `.env` por sí solo; hay que exportar las
variables en la terminal antes de arrancar:

```bash
set -a; source .env; set +a
./mvnw spring-boot:run
```

En Windows (cmd), define cada variable y arranca con el wrapper:

```bat
set DB_PORT=3306
set DB_PASSWORD=tu_clave
mvnw.cmd spring-boot:run
```

Al arrancar, la aplicación ejecuta `schema.sql` y crea la tabla `fruta` si no
existe.

## Base de datos

### Esquema

El esquema está en `src/main/resources/schema.sql` y se ejecuta en cada
arranque (`CREATE TABLE IF NOT EXISTS`). Hibernate está en modo `validate`:
solo comprueba que las entidades coincidan con las tablas y nunca las modifica.

Tabla `fruta`:

| Columna    | Tipo            | Restricciones                                   |
| ---------- | --------------- | ----------------------------------------------- |
| `id`       | `BIGINT`        | Clave primaria, autoincremental                 |
| `nombre`   | `VARCHAR(100)`  | Obligatorio, único                              |
| `precio`   | `DECIMAL(10,2)` | Obligatorio, mayor o igual que 0                |
| `cantidad` | `INT`           | Obligatorio, mayor o igual que 0, por defecto 0 |
| `unidad`   | `VARCHAR(20)`   | Obligatorio (p. ej. `unidad`, `kg`)             |

> Como el script usa `IF NOT EXISTS`, los cambios en `schema.sql` no se aplican
> a una tabla que ya existe. En desarrollo, borra la tabla (o el volumen con
> `docker compose down -v`) para que se vuelva a crear.

### Conexión con un cliente (DBeaver)

1. Nueva conexión → MySQL.
2. Host `localhost`, puerto el de `DB_PORT` en tu `.env`, base `puesto_frutas`,
   usuario `root` y la contraseña de tu `.env`.
3. En la pestaña *Driver properties* poner `allowPublicKeyRetrieval=true` y
   `useSSL=false` (si no, MySQL 8 responde "Public Key Retrieval is not allowed").

La tabla `fruta` aparece después de arrancar la aplicación por primera vez.

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
