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
| `APP_CLIENT_IP_MODE` | `render-public` en Render público o `direct` para acceso directo | No |
| `APP_AUTH_RATE_LIMIT_ENABLED` | Activar límite de inicio de sesión en producción | No; `true` por defecto en prod |
| `APP_AUTH_RATE_LIMIT_MAX_ATTEMPTS` | Intentos de login por IP y ventana | No; 120 por defecto |
| `APP_AUTH_RATE_LIMIT_WINDOW_SECONDS` | Duración de la ventana del límite de login | No; 900 segundos por defecto |
| `APP_PUBLIC_URL` | Origen público del backend usado en referencias de archivos | Recomendado en producción |
| `APP_BOOTSTRAP_ADMIN_PASSWORD` | Contraseña segura de la cuenta histórica inicial, solo cuando aún conserva la contraseña conocida | Condicional en producción |

En producción `POST /auth/login` admite por defecto 120 intentos por IP en 15 minutos.
El contador usa PostgreSQL para coordinar instancias y devuelve `429` con `Retry-After`
al superar el límite. No limita biblioteca ni subidas. Si la IP no es confiable o la
base de datos no está disponible, el inicio de sesión devuelve `503` en lugar de
desactivar silenciosamente la protección. Las ventanas antiguas se eliminan
automáticamente. La confianza de la IP depende de la topología descrita en
[IP-CLIENTE.md](docs/IP-CLIENTE.md); el modo de producción presupone acceso solo por
la URL pública de Render.

El límite cuenta todos los intentos, incluso exitosos, y lo comparten personas tras
una misma IP pública. Usa una ventana fija, que permite ráfagas alrededor del cambio
de ventana. Todas las instancias deben usar la misma base y los mismos límites durante
un despliegue; ajustar los valores si el tráfico legítimo de una red compartida lo
requiere. Las respuestas ya bloqueadas se recuerdan temporalmente por instancia para
reducir consultas repetidas a la base. Este control protege el login en la aplicación;
no sustituye las medidas de protección del borde frente a tráfico masivo.

Para el despliegue indicado, configura `APP_PUBLIC_URL=https://kelly-academy-backend.onrender.com`
y agrega a `APP_CORS_ALLOWED_ORIGINS` el dominio exacto que Vercel proporcione.
Antes de publicar, comprueba que `APP_CLIENT_IP_MODE` conserve `render-public` y que
no existan accesos por red privada o por otro proxy que puedan manipular la cabecera.
La imagen Docker establece `SPRING_PROFILES_ACTIVE=prod` por defecto; verifica que
ninguna variable del servicio de Render la sobrescriba con `dev`.

`DB_DRIVER` aparece en `.env.example`, pero no se lee como variable: el driver está definido directamente como `org.postgresql.Driver` en las properties.

### Cuenta administradora inicial y sesiones

La migración histórica `V10__seed_base_data.sql` crea una cuenta administradora con una contraseña fija conocida. Esta migración también se ejecuta en instalaciones nuevas de producción; desactivar el seeder de desarrollo no evita su ejecución.

En el perfil `prod`, antes de atender solicitudes, el backend comprueba esa cuenta. Si aún usa la contraseña histórica, la sustituye por `APP_BOOTSTRAP_ADMIN_PASSWORD`. Define esa variable secreta en Render antes de desplegar: debe tener entre 12 y 72 bytes, mayúsculas, minúsculas, números y símbolos, sin espacios. Si falta o no cumple la política, **el arranque falla** y solicita configurarla. Si la cuenta ya tiene otra contraseña o no existe, se conserva su estado y no se exige esa variable. La comprobación usa un bloqueo en la base de datos para coordinar instancias que arranquen a la vez.

No compartas la contraseña ni la guardes en el repositorio. La comprobación protege esta versión al iniciarse; revisa y rota cuanto antes la credencial en instalaciones que todavía ejecuten una versión anterior. No edites una migración Flyway ya aplicada: sus verificaciones de integridad deben seguir coincidiendo con las instalaciones existentes.

Los JWT nuevos incorporan una marca derivada de la contraseña almacenada. Al cambiarla o restablecerla, los tokens anteriores dejan de ser válidos. **Tras desplegar esta versión, los tokens emitidos por versiones anteriores también se rechazan** y todos los usuarios deben iniciar sesión de nuevo. Desactivar y reactivar una cuenta, por sí solo, no revoca permanentemente un token todavía vigente.

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

### Comprobación de migraciones con PostgreSQL

El perfil opcional `postgres-it` ejecuta pruebas aisladas contra contenedores temporales de PostgreSQL 18. Comprueba las migraciones Flyway, la validación de esquema JPA, el arranque del perfil de producción y la protección de la cuenta administradora inicial. Requiere Docker disponible, acceso inicial a Maven Central y al registro de imágenes; no usa las credenciales ni la base de datos de desarrollo o producción:

```bash
./mvnw -Ppostgres-it test
```

En Windows, usa `mvnw.cmd -Ppostgres-it test`. La imagen `postgres:18-alpine` y las dependencias de Testcontainers se descargan si no están disponibles localmente. El contenedor se elimina al finalizar. Esta opción no sustituye la suite H2. Los resultados se generan en `target-postgres-it`.

### Pruebas HTTP existentes con PostgreSQL

El perfil optativo `postgres-suite` ejecuta los tests `*ControllerIT` existentes con PostgreSQL 18 en un contenedor temporal y aplica las migraciones Flyway reales. La base es exclusiva de las pruebas: la preparación de cada caso borra sus tablas de negocio, conserva `flyway_schema_history` y comprueba que el motor sea PostgreSQL. El contenedor permanece disponible durante la ejecución y se elimina al terminar la JVM. Se requieren Docker, las dependencias de Testcontainers y la imagen indicados arriba.

```bash
./mvnw -Ppostgres-suite test
```

Se puede seleccionar un caso concreto con `-Dtest=UsuarioControllerIT#cambiarContrasena_revocaTokenAnteriorYPermiteNuevaSesion`. La selección validada cubre ocho casos de usuarios, cursos, asistencia masiva, tutorías y mensajes; **no implica que se haya ejecutado o aprobado toda la suite**. Los tests de integración que no terminan en `ControllerIT` todavía requieren su propia adaptación y validación contra PostgreSQL. El flujo predeterminado continúa usando H2, y el perfil PostgreSQL genera resultados en `target-postgres-suite`.
