# Deuda Tecnica — Kelly Academy LMS Backend

Registro de decisiones postergadas a proposito. Actualizar el estado
cuando se resuelva, indicando el commit.

## Pendientes

| #  | Deuda | Feature  | Resolver en | Estado |
|----|---|----------|-------------|---|
| 3  | Endpoint administrativo para cambiar `estado` de usuario | user     | FASE 5      | Pendiente |
| 4  | Endpoint de cambio de contrasena con validacion de contrasena actual | user     | FASE 5      | Pendiente |
| 5  | Endpoint de cambio de correo con verificacion por email | user     | FASE 7      | Pendiente |
| 6  | Endpoint administrativo para asignar/quitar roles | user     | FASE 5      | Pendiente |
| 7  | Warning de API deprecada en `JwtAuthenticationFilter` | security | FASE 6      | Pendiente |
| 8  | Warning de Spring Security sobre `AuthenticationProvider` manual | security | FASE 6      | Pendiente |
| 9  | Warning de Mockito self-attaching | testing  | FASE 6      | Pendiente |
| 10 | RolResponse anida permisos — revisar cuando Rol.permisos pase a LAZY | user     | FASE 4      | Pendiente |
| 11 | `CursoResponse` embebe `UsuarioResumenResponse` — dispara EAGER de `Usuario.roles` y `Rol.permisos` | course | FASE 4 | Pendiente |
| 12 | Endpoint administrativo para cambiar docente de un curso | course | FASE 5 | Pendiente |
| 13 | Endpoint administrativo para cambiar estado de curso (con maquina de estados) | course | FASE 5 | Pendiente |
| 14 | Validar que `docenteId` tenga rol DOCENTE antes de asignarlo a un curso | course | FASE 4 | Pendiente |
| 15 | `esActual` de `Semana` no se puede cambiar via `PUT` — requiere endpoint `PATCH /semanas/{id}/marcar-actual` con logica transaccional (desmarcar la anterior) | course | FASE 4 | Pendiente |
| 16 | Reordenar `numero` de unidades o semanas — requiere endpoint de operacion masiva (no `PUT` individual) por restriccion `UNIQUE(curso_id, numero)` y `UNIQUE(unidad_id, numero)` | course | FASE 5 | Pendiente |
| 17 | `Material` permite crear sin `urlArchivo` ni `urlExterno` — validar "al menos una URL" en servicio | course | FASE 4 | Pendiente |
| 18 | `Clase.urlVivo` y `urlGrabacion` — no hay validacion de formato de URL (solo longitud) | course | FASE 4 | Pendiente |
| 19 | Cambiar `semanaId` de `Clase`, `Material` o `Tarea` (mover entre semanas) — requiere endpoint dedicado | course | FASE 5 | Pendiente |
| 20 | Tests con H2 + `create-drop` no validan que las migraciones Flyway coincidan con las entidades. Cobertura real requiere Testcontainers con Postgres. | testing | FASE 6 | Pendiente |
| 21 | Endpoint `PATCH /entregas/{id}/calificar` — setea `nota`, `retroalimentacion` y pasa `estado` a `CALIFICADA` con validacion de puntaje maximo de la `Tarea` | enrollment | FASE 5 | Pendiente |
| 22 | Endpoint administrativo para cambiar `estado` de `Matricula` (maquina de estados: ACTIVA -> COMPLETADA / RIESGO / ABANDONADA) | enrollment | FASE 5 | Pendiente |
| 23 | Calculo automatico de `notaFinal` y `asistenciaPorcentaje` de `Matricula` a partir de entregas y asistencias | enrollment | FASE 6 | Pendiente |
| 24 | Validar en servicio que no se pueda re-subir archivo de `Entrega` si `estado = CALIFICADA` | enrollment | FASE 4 | Pendiente |
| 25 | `Entrega.estado` (PENDIENTE / TARDE) se calcula comparando `enviadoAt` con `Tarea.fechaLimite` en el servicio de creacion. Sin tests aun. | enrollment | FASE 6 | Pendiente |
| 26 | Validar en servicio que `estudianteId` este matriculado en el curso de la `Clase` antes de registrar `Asistencia` | attendance | FASE 4 | Pendiente |
| 27 | Validar en servicio que `claseId` corresponda a una clase ya impartida (`fechaHora <= now()`) antes de registrar asistencia | attendance | FASE 4 | Pendiente |
| 28 | `Conversacion` unique constraint no normaliza orden de participantes — (A,B) y (B,A) son filas distintas. Mitigacion actual: servicio normaliza orden por UUID antes de crear. Solucion robusta: indice funcional Postgres con LEAST/GREATEST (requiere Testcontainers). | communication | FASE 6 | Pendiente |
| 29 | Validar en servicio que ambos participantes de una `Conversacion` pertenezcan al `Curso` referenciado (docente del curso o estudiante matriculado). | communication | FASE 4 | Pendiente |
| 30 | Validar en servicio que `otroParticipanteId != usuarioAutenticado.id` al crear conversacion. | communication | FASE 4 | Pendiente |
| 31 | Endpoint dedicado `PATCH /anuncios/{id}/archivar` para cambiar `activo`. | communication | FASE 5 | Pendiente |
| 32 | Endpoint dedicado `PATCH /mensajes/{id}/leer` y `PATCH /conversaciones/{id}/leer-todos` para marcar `leido`. | communication | FASE 5 | Pendiente |
| 33 | Validar en servicio que el usuario autenticado sea participante de la `Conversacion` antes de insertar `Mensaje`. | communication | FASE 4 | Pendiente |
| 34 | Endpoint dedicado `PATCH /conversaciones/{id}/asunto` si se necesita editar asunto post-creacion. | communication | FASE 5 | Pendiente |
| 35 | Endpoint `PATCH /notificaciones/{id}/leer` y `PATCH /notificaciones/leer-todas` con validacion de que la notificacion pertenece al usuario autenticado. | communication | FASE 5 | Pendiente |
| 36 | `NotificacionService.crear(...)` interno para que otros servicios (calificaciones, mensajes, anuncios) generen notificaciones. Sin endpoint publico de creacion. | communication | FASE 5 | Pendiente |
| 37 | Endpoints `GET /notificaciones` y `GET /notificaciones/no-leidas` y `GET /notificaciones/count-no-leidas` filtrados por usuario autenticado. | communication | FASE 5 | Pendiente |
| 38 | Validar en servicio que `Evento.fin > Evento.inicio` cuando `fin != null`. | calendar | FASE 4 | Pendiente |
| 39 | Validar en servicio que `DisponibilidadTutoria.horaFin > horaInicio`. | calendar | FASE 4 | Pendiente |
| 40 | Validar en servicio que no se solapen bloques de disponibilidad del mismo docente y dia. Requiere query de interseccion. | calendar | FASE 4 | Pendiente |
| 41 | Validar en servicio que el usuario autenticado sea el docente dueno o ADMIN al crear/modificar `DisponibilidadTutoria`. | calendar | FASE 4 | Pendiente |
| 42 | Validar en servicio que al crear `Evento`, si `cursoId != null`, el usuario pertenezca al curso (docente o estudiante matriculado). | calendar | FASE 4 | Pendiente |
| 43 | Endpoint `PATCH /tutorias/{id}/estado` con validacion de transiciones (PENDIENTE -> CONFIRMADA / CANCELADA; CONFIRMADA -> COMPLETADA / CANCELADA; COMPLETADA y CANCELADA terminales). | calendar | FASE 5 | Pendiente |
| 44 | Validar en servicio que `fecha` y `duracionMinutos` de `Tutoria` solo sean editables cuando `estado = PENDIENTE`. | calendar | FASE 4 | Pendiente |
| 45 | Validar en servicio que `Tutoria.fecha > now()` al crear. | calendar | FASE 4 | Pendiente |
| 46 | Validar en servicio que el usuario autenticado sea el estudiante, el docente o ADMIN al crear/modificar `Tutoria`. | calendar | FASE 4 | Pendiente |
| 47 | Validar en servicio que el docente tenga disponibilidad (`DisponibilidadTutoria`) en el bloque solicitado al crear `Tutoria`. Requiere cruzar `DayOfWeek` + rango horario. | calendar | FASE 6 | Pendiente |
| 48 | Validar en servicio que no exista solapamiento con otras tutorias CONFIRMADAS del mismo docente o estudiante. | calendar | FASE 6 | Pendiente |
| 49 | Validar en servicio que `RecursoBiblioteca` tenga al menos `urlArchivo` o `urlExterno`. Sin ninguna URL el recurso no es descargable. | library | FASE 4 | Pendiente |
| 50 | Endpoint `POST /recursos/{id}/descargar` que incremente `contadorDescargas` y retorne la URL. Requiere `@Modifying` query o `@Transactional` con incremento atomico. | library | FASE 5 | Pendiente |
| 51 | Validar en servicio que `urlExterno` tenga formato de URL valido (no solo longitud). | library | FASE 4 | Pendiente |
| 52 | Auditar `RolResponse` (deuda #10) ahora que `Rol.permisos` es LAZY: todo mapper que itere `permisos` debe invocarse dentro de `@Transactional(readOnly = true)` o con entidades cargadas via `@EntityGraph`. | user | FASE 4 | Pendiente |
| 53 | Auditar todos los mappers que accedan a `Usuario.roles` o `Rol.permisos` (`UsuarioMapper.toResponse`, `RolMapper.toResponse`). Asegurar que se invoquen dentro de `@Transactional(readOnly = true)` o con `@EntityGraph`. | user | FASE 4 | Pendiente |
| 54 | Documentar en README los dos flujos de arranque: (a) IDE con `.env` inyectado, (b) terminal con `./mvnw spring-boot:run` que carga `.env` via `spring.config.import`. | infrastructure | FASE 5 | Pendiente |
| 55 | Auditar uso de `APP_CORS_ALLOWED_ORIGINS` — confirmar que `SecurityConfig` lo lee desde properties y no esta hardcodeado. | security | FASE 4 | Pendiente |


## Resueltos

| # | Deuda | Commit | Fecha |
|---|---|---|---|
## Resueltos

| # | Deuda | Commit | Fecha |
|---|---|---|---|
| 1 | `Usuario.roles` con `FetchType.EAGER` — riesgo N+1 en listados paginados | refactor/user-lazy-fetching | 2026-09-18 |
| 2 | `Rol.permisos` con `FetchType.EAGER` — agrava el punto 1 | refactor/user-lazy-fetching | 2026-09-18 |


### Decisiones por diseno (no son deuda)

- `Matricula` no tiene `ActualizarMatriculaRequest`. No se edita via `PUT`. Todo cambio va por endpoints dedicados (estado, nota final, asistencia). Esto es intencional: evita mutaciones indebidas sobre relaciones inmutables (`curso`, `estudiante`) y campos derivados (`notaFinal`, `asistenciaPorcentaje`).
- `Entrega` se crea al momento del envio, no pre-generada al matricular. `CrearEntregaRequest` exige `urlArchivo`. Si en el futuro se permite pre-generar entregas en `PENDIENTE`, se relaja a nullable y se agrega endpoint separado para subir archivo.
- `Asistencia.estado` SI se permite editar via `PUT` porque es dato operativo editable (el docente corrige asistencia). La regla "estados por endpoint dedicado" aplica a estados administrativos (rol, estado de cuenta, estado de curso), no a datos operativos.
- `Asistencia.registradoAt` no se recalcula en `actualizarDesdeRequest`. Es la marca original del registro; para "ultima modificacion" ya existe `fechaActualizacion` de `BaseEntity`.
- `Conversacion.mensajesNoLeidos` no es campo de entidad; se calcula via `MensajeRepository.countByConversacionIdAndRemitenteIdNotAndLeidoFalse`. El mapper tiene dos metodos sobrecargados: `toResponse(Conversacion)` (sin conteo, `null`) y `toResponse(Conversacion, long)` (con conteo).
- `Mensaje` no tiene `ActualizarMensajeRequest`: un mensaje enviado no se edita.
- `Notificacion` no expone endpoint de creacion publica: se genera desde servicios internos para evitar auto-notificacion y spam.
- `Conversacion.crear` recibe solo `otroParticipanteId`; el `participante1` se resuelve del SecurityContext para evitar suplantacion.
- `Evento.usuarioId` no va en `CrearEventoRequest`: el servicio lo resuelve del SecurityContext. Un usuario solo gestiona sus propios eventos. Vista admin se cubre con `GET /eventos?usuarioId=X` en FASE 5.
- `Evento.curso` inmutable tras creacion. Vincular un evento personal a un curso despues cambia el contexto semantico del evento; se borra y recrea si es necesario.
- `DisponibilidadTutoria.bloqueada` editable via PUT: flag operativo del docente, sin maquina de estados. Distinto de `Anuncio.activo` (soft-delete) y `Tutoria.estado` (maquina de estados).
- `Tutoria.estado` no va en `CrearTutoriaRequest` (se inicializa en PENDIENTE) ni en `ActualizarTutoriaRequest` (va por PATCH dedicado con maquina de estados).
- `Tutoria.curso` inmutable tras creacion: la tutoria es de un curso o no lo es; cambiar de contexto requiere borrar y recrear.
- `Tutoria.notas` editable siempre, sin restriccion de estado: es informacion, no operacion critica.
- `CrearTutoriaRequest` recibe `estudianteId` y `docenteId` explicitos; el servicio valida que el usuario autenticado sea uno de los dos (o ADMIN). Un solo DTO para los dos flujos.
- `RecursoBiblioteca.contadorDescargas` no va en requests; lo maneja el servicio via endpoint dedicado de descarga.
- `RecursoResumenResponse` no incluye URLs ni descripcion: en listados de catalogo solo se muestran tarjetas con titulo, categoria, nivel, tipo y popularidad. El detalle completo se consulta aparte.
- `RecursoBiblioteca` reutiliza `NivelCefr` y `TipoMaterial` de `course`. No se duplican enums para evitar divergencia.
- `Usuario.roles` y `Rol.permisos` son `FetchType.LAZY`. Los metodos que necesitan cargar el grafo completo usan `@EntityGraph` explicito: `UsuarioRepository.findByCorreoElectronico` carga `{"roles", "roles.permisos"}` para que `CustomUserDetails` funcione; `RolRepository.findByNombre` carga `{"permisos"}` para que `RolMapper.toResponse` funcione. `open-in-view=false` en `application.properties` garantiza que ningun acceso LAZY fuera de transaccion pase silenciosamente.
- El archivo `.env` de desarrollo local se carga via `spring.config.import=optional:file:./.env[.properties]` en `application-dev.properties`. El prefijo `optional:` evita fallo en produccion, donde las variables vienen del orquestador y `.env` no existe.
- `spring.profiles.active=${SPRING_PROFILES_ACTIVE:dev}` en `application.properties`: produccion debe definir `SPRING_PROFILES_ACTIVE=prod` desde el orquestador. El default `dev` es solo para arranque local.
- 