# Kelly Academy LMS Backend

Backend REST del sistema Kelly Academy LMS.

## Requisitos previos

- Java 21.
- Maven 3.9 o superior. El proyecto incluye Maven Wrapper.
- PostgreSQL 18.
- Una base de datos PostgreSQL accesible desde el entorno de ejecución.

## Variables de entorno

Crea un archivo `.env` a partir de `.env.example` y completa los valores reales. No publiques ni comitees `.env`.

| Variable | Uso | Requerida |
| --- | --- | --- |
| `DB_URL` | URL JDBC de PostgreSQL usada por JPA y Flyway | Sí |
| `DB_USERNAME` | Usuario de PostgreSQL | Sí |
| `DB_PASSWORD` | Contraseña de PostgreSQL | Sí |
| `JWT_SECRET` | Secreto usado para firmar los JWT | Sí |
| `JWT_EXPIRATION` | Duración del JWT en milisegundos | Sí |
| `SPRING_PROFILES_ACTIVE` | Perfil de Spring; por defecto `dev` | No |
| `PORT` | Puerto HTTP en producción; por defecto `8080` | No |
| `APP_CORS_ALLOWED_ORIGINS` | Orígenes CORS separados por comas | No |

`DB_DRIVER` aparece en `.env.example`, pero no se lee como variable: el driver está definido directamente como `org.postgresql.Driver` en las properties.

La contraseña inicial de la cuenta administradora se obtiene de la configuración/seed local y no se documenta aquí. Consulta el `.env` y las instrucciones de despliegue del entorno correspondiente.

## Flujo A: IDE

### IntelliJ IDEA

1. Copia `.env.example` como `.env` y completa sus valores.
2. Abre **Run/Debug Configurations**.
3. Selecciona la configuración de Spring Boot del proyecto.
4. Define las variables en **Environment variables** o habilita la carga del archivo `.env` mediante el soporte/plugin de archivos de entorno disponible en tu instalación.
5. Ejecuta la clase principal `com.kellyacademy.LmsBackendApplication`.

### Visual Studio Code

1. Copia `.env.example` como `.env` y completa sus valores.
2. Crea o ajusta la configuración Java de ejecución en `.vscode/launch.json`.
3. Define las variables en la propiedad `env` de esa configuración, o usa una extensión de archivos de entorno compatible con Java.
4. Ejecuta `com.kellyacademy.LmsBackendApplication` desde **Run and Debug**.

En un IDE, verifica que las variables estén disponibles para el proceso Java. La importación automática de `.env` descrita en el flujo B ocurre cuando se inicia Spring Boot con el perfil `dev` desde la raíz del proyecto.

## Flujo B: terminal

Desde la raíz del proyecto:

```powershell
./mvnw.cmd spring-boot:run
```

En Linux/macOS:

```bash
./mvnw spring-boot:run
```

El perfil `dev` importa `.env` mediante:

```text
spring.config.import=optional:file:./.env[.properties]
```

También puedes seleccionar explícitamente el perfil:

```powershell
$env:SPRING_PROFILES_ACTIVE="dev"
./mvnw.cmd spring-boot:run
```

## Comandos de verificación

```powershell
./mvnw.cmd clean compile
./mvnw.cmd clean test
./mvnw.cmd spring-boot:run
```

En Linux/macOS, sustituye `./mvnw.cmd` por `./mvnw`.

## URLs de desarrollo

- API: http://localhost:8080
- Swagger: http://localhost:8080/swagger-ui.html
- Actuator health: http://localhost:8080/actuator/health
