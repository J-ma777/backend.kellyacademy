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
| 12 | Endpoint administrativo para cambiar docente de un curso | course | FASE 5 | Pendiente |
| 13 | Endpoint administrativo para cambiar estado de curso (con maquina de estados) | course | FASE 5 | Pendiente |
| 16 | Reordenar `numero` de unidades o semanas — requiere endpoint de operacion masiva (no `PUT` individual) por restriccion `UNIQUE(curso_id, numero)` y `UNIQUE(unidad_id, numero)` | course | FASE 5 | Pendiente |
| 19 | Cambiar `semanaId` de `Clase`, `Material` o `Tarea` (mover entre semanas) — requiere endpoint dedicado | course | FASE 5 | Pendiente |
| 20 | Tests con H2 + `create-drop` no validan que las migraciones Flyway coincidan con las entidades. Cobertura real requiere Testcontainers con Postgres. | testing | FASE 6 | Pendiente |
| 21 | Endpoint `PATCH /entregas/{id}/calificar` — setea `nota`, `retroalimentacion` y pasa `estado` a `CALIFICADA` con validacion de puntaje maximo de la `Tarea` | enrollment | FASE 5 | Pendiente |
| 22 | Endpoint administrativo para cambiar `estado` de `Matricula` (maquina de estados: ACTIVA -> COMPLETADA / RIESGO / ABANDONADA) | enrollment | FASE 5 | Pendiente |
| 23 | Calculo automatico de `notaFinal` y `asistenciaPorcentaje` de `Matricula` a partir de entregas y asistencias | enrollment | FASE 6 | Pendiente |
| 28 | `Conversacion` unique constraint no normaliza orden de participantes — (A,B) y (B,A) son filas distintas. Mitigacion actual: servicio normaliza orden por UUID antes de crear. Solucion robusta: indice funcional Postgres con LEAST/GREATEST (requiere Testcontainers). | communication | FASE 6 | Pendiente |
| 34 | Endpoint dedicado `PATCH /conversaciones/{id}/asunto` si se necesita editar asunto post-creacion. | communication | FASE 5 | Pendiente |
| 43 | Endpoint `PATCH /tutorias/{id}/estado` con validacion de transiciones (PENDIENTE -> CONFIRMADA / CANCELADA; CONFIRMADA -> COMPLETADA / CANCELADA; COMPLETADA y CANCELADA terminales). | calendar | FASE 5 | Pendiente |
| 47 | Validar en servicio que el docente tenga disponibilidad (`DisponibilidadTutoria`) en el bloque solicitado al crear `Tutoria`. Requiere cruzar `DayOfWeek` + rango horario. | calendar | FASE 6 | Pendiente |
| 48 | Validar en servicio que no exista solapamiento con otras tutorias CONFIRMADAS del mismo docente o estudiante. | calendar | FASE 6 | Pendiente |
| 50 | Endpoint `POST /recursos/{id}/descargar` que incremente `contadorDescargas` y retorne la URL. Requiere `@Modifying` query o `@Transactional` con incremento atomico. | library | FASE 5 | Pendiente |
| 54 | Documentar en README los dos flujos de arranque: (a) IDE con `.env` inyectado, (b) terminal con `./mvnw spring-boot:run` que carga `.env` via `spring.config.import`. | infrastructure | FASE 5 | Pendiente |
| 55 | Auditar uso de `APP_CORS_ALLOWED_ORIGINS` — confirmar que `SecurityConfig` lo lee desde properties y no esta hardcodeado. | security | FASE 4 | Pendiente |
| 56 | `@EntityGraph(attributePaths = {"docente"})` en `CursoRepository.findAll(Specification, Pageable)` y `findWithDocenteById` carga la entidad `Usuario` completa, incluyendo `contrasena`, para exponer solo 6 campos escalares en `UsuarioResumenResponse`. Optimizable con proyeccion. Riesgo teorico: solo si se activa `org.hibernate.orm.jdbc.bind=TRACE` en produccion, los valores bind (incluido el hash) se imprimen en logs. | course | FASE 6 | Pendiente |
| 57 | IntegrationTests levantan el contexto Spring completo (~20s por clase). Spring no reutiliza el contexto entre `UnidadControllerIT` y `SemanaControllerIT` pese a compartir configuracion. Optimizacion: revisar por que no se cachea, o migrar a `RestTestClient` (Spring Boot 4) que tiene mejor soporte. | testing | FASE 6 | Pendiente |
| 58 | `Entrega` no distingue "asignacion del docente" de "envio del estudiante". Hoy se mezclan en un mismo registro (`enviadoAt` + `urlArchivo`). Si el dominio requiere separar los dos eventos (Submission con historial de intentos), FASE 6+. | enrollment | FASE 6 | Pendiente |
| 59 | `EntregaService.eliminar` (ADMIN) no valida que la entrega no este `CALIFICADA`. Borrar una entrega calificada deja inconsistente el `notaFinal` futuro de la `Matricula`. | enrollment | FASE 5 | Pendiente |
| 60 | `CrearEntregaRequest.urlArchivo` es `@NotBlank`. No permite pre-asignar tareas sin archivo inicial. Relajar a `@Nullable` + endpoint separado de subida cuando se implemente el flujo "docente asigna tarea sin archivo, estudiante sube despues". | enrollment | FASE 5 | Pendiente |
| 61 | `Clase.fechaHora` nullable impide aplicar la validacion `CLASE_NO_IMPARTIDA` (#27) a clases sin fecha. Requiere decidir si `fechaHora` pasa a obligatoria o si se modela "clase impartida" con un flag explicito. | attendance | FASE 5 | Pendiente |
| 62 | `Asistencia.estado` no dispara `Notificacion` al estudiante cuando se registra AUSENTE / TARDE / JUSTIFICADO. Depende de #36. | attendance | FASE 5 | Pendiente |
| 63 | No hay endpoint de registro masivo de asistencia por clase (`POST /api/asistencias/masivo` con lista de estudiantes). Hoy se registra uno por uno. | attendance | FASE 5 | Pendiente |
| 64 | `IntegrationTestBase.limpiarTablas()` escala manualmente: cada entidad nueva requiere agregar su `deleteAll` en orden inverso a las FKs. Refactor a `TRUNCATE ... CASCADE` o limpieza dinamica basada en metadatos de Hibernate. | testing | FASE 5 | Pendiente |
| 65 | Inconsistencia entre `Specifications`: `CursoSpecifications` y `NotificacionSpecifications` usan `Specification.unrestricted()` para match-all; `MatriculaSpecifications`, `EntregaSpecifications` y `AnuncioSpecifications` devuelven `null` en el predicado. Unificar convencion. | shared | FASE 5 | Pendiente |
| 66 | `AnuncioService.crear` notifica a estudiantes matriculados uno por uno dentro de un mismo `@Transactional`. Con cursos grandes (>100 estudiantes) esto genera N inserts secuenciales. Considerar batch insert o job asincrono. | communication | FASE 6 | Pendiente |
| 67 | `ConversacionRepository.findByCursoIsNullAndParticipante1IdAndParticipante2Id` no está cubierto por índice único funcional (solo el caso con curso). Dos hilos concurrentes podrían crear conversaciones duplicadas sin curso. Mitigación actual: el servicio normaliza orden. Solución robusta: índice funcional Postgres con LEAST/GREATEST + `curso_id NULLS NOT DISTINCT`. Requiere Testcontainers. | communication | FASE 6 | Pendiente |
| 68 | `MensajeController.listar` pagina mensajes con sort `enviadoAt` ascendente. Conversaciones largas obligan al cliente a paginar hacia adelante. Considerar endpoint alternativo de "últimos N mensajes" para carga inicial del chat. | communication | FASE 5 | Pendiente |


## Resueltos

| #  | Deuda | Commit | Fecha |
|----|---|--|---|
| 1  | `Usuario.roles` con `FetchType.EAGER` — riesgo N+1 en listados paginados | refactor/user-lazy-fetching | 2026-09-18 |
| 2  | `Rol.permisos` con `FetchType.EAGER` — agrava el punto 1 | refactor/user-lazy-fetching | 2026-09-18 |
| 10 | RolResponse anida permisos — revisar cuando Rol.permisos pase a LAZY | a5887b1 | 2026-09-19 |
| 11 | `CursoResponse` embebe `UsuarioResumenResponse` — dispara EAGER de `Usuario.roles` y `Rol.permisos` | e459b96 | 2026-09-19 |
| 14 | Validar que `docenteId` tenga rol DOCENTE antes de asignarlo a un curso | e459b96 | 2026-09-19 |
| 15 | `esActual` de `Semana` no se puede cambiar via `PUT` — requiere endpoint `PATCH /semanas/{id}/marcar-actual` | 69e4b1b | 2026-09-20 |
| 17 | `Material` permite crear sin `urlArchivo` ni `urlExterno` — validar "al menos una URL" en servicio | PR #6 | 2026-09-20 |
| 18 | `Clase.urlVivo` y `urlGrabacion` — no hay validacion de formato de URL (solo longitud) | PR #6 | 2026-09-20 |
| 24 | Validar en servicio que no se pueda re-subir archivo de `Entrega` si `estado = CALIFICADA` | PR #13 | 2026-09-20 |
| 25 | `Entrega.estado` (PENDIENTE / TARDE) se calcula comparando `enviadoAt` con `Tarea.fechaLimite` en el servicio de creacion | PR #13 | 2026-09-20 |
| 26 | Validar en servicio que `estudianteId` este matriculado en el curso de la `Clase` antes de registrar `Asistencia` | 1e443a8 | 2026-09-20 |
| 27 | Validar en servicio que `claseId` corresponda a una clase ya impartida (`fechaHora <= now()`) antes de registrar asistencia | 1e443a8 | 2026-09-20 |
| 29 | Validar en servicio que ambos participantes de una `Conversacion` pertenezcan al `Curso` referenciado (docente del curso o estudiante matriculado) | 11004db | 2026-09-20 |
| 30 | Validar en servicio que `otroParticipanteId != usuarioAutenticado.id` al crear conversacion | 11004db | 2026-09-20 |
| 31 | Endpoint dedicado `PATCH /anuncios/{id}/archivar` para cambiar `activo` | 66f364c | 2026-09-20 |
| 32 | Endpoint dedicado `PATCH /mensajes/{id}/leer` y `PATCH /conversaciones/{id}/leer-todos` para marcar `leido`. Nota: la ruta real quedo como `PATCH /conversaciones/{cid}/mensajes/{mid}/leer` para mantener consistencia REST anidada | 11004db | 2026-09-20 |
| 33 | Validar en servicio que el usuario autenticado sea participante de la `Conversacion` antes de insertar `Mensaje` | 11004db | 2026-09-20 |
| 35 | Endpoint `PATCH /notificaciones/{id}/leer` y `PATCH /notificaciones/leer-todas` con validacion de que la notificacion pertenece al usuario autenticado | 150ded9 | 2026-09-20 |
| 36 | `NotificacionService.crear(...)` interno para que otros servicios generen notificaciones. Sin endpoint publico de creacion | 150ded9 | 2026-09-20 |
| 37 | Endpoints `GET /notificaciones`, `GET /notificaciones/no-leidas` y `GET /notificaciones/count-no-leidas` filtrados por usuario autenticado | 150ded9 | 2026-09-20 |
| 38 | Validar en servicio que `Evento.fin > Evento.inicio` cuando `fin != null`. | ca9084f | 2026-09-20 |
| 39 | Validar en servicio que `DisponibilidadTutoria.horaFin > horaInicio`. | 5d7739f | 2026-09-20 |
| 40 | Validar en servicio que no se solapen bloques de disponibilidad del mismo docente y dia. Requiere query de interseccion. | 5d7739f | 2026-09-20 |
| 41 | Validar en servicio que el usuario autenticado sea el docente dueno o ADMIN al crear/modificar `DisponibilidadTutoria`. | 5d7739f | 2026-09-20 |
| 42 | Validar en servicio que al crear `Evento`, si `cursoId != null`, el usuario pertenezca al curso (docente o estudiante matriculado). | ca9084f | 2026-09-20 |
| 44 | Validar en servicio que `fecha` y `duracionMinutos` de `Tutoria` solo sean editables cuando `estado = PENDIENTE`. | a772ec6 | 2026-09-20 |
| 45 | Validar en servicio que `Tutoria.fecha > now()` al crear. | a772ec6 | 2026-09-20 |
| 46 | Validar en servicio que el usuario autenticado sea el estudiante, el docente o ADMIN al crear/modificar `Tutoria`. | a772ec6 | 2026-09-20 |
| 49 | Validar en servicio que `RecursoBiblioteca` tenga al menos `urlArchivo` o `urlExterno`. Sin ninguna URL el recurso no es descargable. | 8ca397b | 2026-09-20 |
| 51 | Validar en servicio que `urlExterno` tenga formato de URL valido (no solo longitud). | 8ca397b | 2026-09-20 |
| 52 | Auditar `RolResponse` ahora que `Rol.permisos` es LAZY | a5887b1 | 2026-09-19 |falta 
| 53 | Auditar mappers que accedan a `Usuario.roles` o `Rol.permisos` | a5887b1 | 2026-09-19 |
| 67 | `AnuncioService` no notifica al editar un anuncio (solo al crear). Decision de producto: ¿editar y re-notificar? | 66f364c | 2026-09-20 |


### Decisiones por diseno (no son deuda)

- `Matricula` no tiene `ActualizarMatriculaRequest`. No se edita via `PUT`. Todo cambio va por endpoints dedicados (estado, nota final, asistencia). Esto es intencional: evita mutaciones indebidas sobre relaciones inmutables (`curso`, `estudiante`) y campos derivados (`notaFinal`, `asistenciaPorcentaje`).
- `Entrega` se crea al momento del envio, no pre-generada al matricular. `CrearEntregaRequest` exige `urlArchivo`. Si en el futuro se permite pre-generar entregas en `PENDIENTE`, se relaja a nullable y se agrega endpoint separado para subir archivo.
- `Entrega` es creada por el DOCENTE dueno del curso de la tarea (o ADMIN), no por el estudiante. El estudiante solo modifica la suya (`PUT`) mientras no este `CALIFICADA`. El endpoint `PUT` valida que el autenticado sea el estudiante dueno o ADMIN.
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
- Los mappers que acceden a colecciones LAZY (`UsuarioMapper.toResponse` lee `Usuario.roles`, `RolMapper.toResponse` lee `Rol.permisos`) se invocan **exclusivamente** desde servicios `@Transactional(readOnly = true)` con entidades cargadas via `@EntityGraph` explicito (`UsuarioRepository.findWithRolesById`, `RolRepository.findWithPermisosById`). Nunca se invocan desde controllers ni desde metodos fuera de transaccion. Esta es la regla que cierra las deudas #10, #52 y #53.
- `Asistencia` no expone endpoint `PATCH` de estado. El `PUT` cubre la correccion de asistencia porque `estado` es dato operativo editable (docente corrige), no campo administrativo. Alineado con la decision de `Matricula` (sin PUT) y `Entrega` (PUT limitado a `urlArchivo`).
- `AsistenciaService.crear` valida `clase.fechaHora <= now()` con `AppTime.ZONA_NEGOCIO` (America/Lima). No usa `LocalDateTime.now()` sin zona para evitar divergencia con servidores en UTC.
- `AsistenciaSpecifications` devuelve `Specification.unrestricted()` cuando el parametro es null, alineado con `CursoSpecifications` (no con `MatriculaSpecifications` / `EntregaSpecifications` que devuelven `null` en el predicado — inconsistencia preexistente de #65, ver Pendientes).
- La autorizacion de `AsistenciaService` reutiliza `SecurityUtils.validarDocenteDuenoOAdmin`, consistente con el resto de features.
- `Anuncio.activo` es soft-delete editable via `PATCH /anuncios/{id}/archivar`. No hay `PUT` para cambiar `activo`: alineado con la regla "estados administrativos por endpoint dedicado".
- `AnuncioService.crear` notifica a estudiantes matriculados ACTIVOS en el curso, excluyendo al autor. Al editar no re-notifica: la edicion de un anuncio existente no debe generar ruido. Si en el futuro se requiere, se agrega un flag explicito al `ActualizarAnuncioRequest`.
- `NotificacionService.validarUrl` acepta dos formatos de `link`: ruta relativa interna que empieza con `/` (ej. `/api/anuncios/<uuid>`, formato que usan los servicios internos) o URL absoluta con scheme + host. Rechaza texto suelto y URLs mal formadas.
- `NotificacionService.crear(...)` es infraestructura pura: no valida auto-notificacion ni permisos. El llamador decide. `AnuncioService` excluye al autor; otros servicios haran lo propio.
- `AnuncioService` no expone endpoint de creacion de `Notificacion`: las notificaciones se generan como efecto secundario de acciones de negocio (crear anuncio, enviar mensaje, calificar entrega).
- - `ConversacionService.crear` es idempotente: si ya existe una conversacion con los mismos participantes (y mismo curso, o ambos sin curso), retorna la existente. El frontend puede llamar sin miedo a duplicar.
- `ConversacionService.crear` normaliza el orden de participantes por UUID (`p1.id < p2.id`). Esto evita duplicados (A,B) vs (B,A). Mitigacion en servicio de la deuda #28; solucion robusta (indice funcional) queda para FASE 6 con Testcontainers (#67).
- `ConversacionService` valida "pertenece al curso" asi: docente dueno del curso o estudiante con matricula ACTIVA. No basta con estar matriculado (puede estar ABANDONADA/RIESGO/COMPLETADA). Reutiliza `MatriculaRepository.findByCursoIdAndEstudianteId` y compara estado en servicio.
- `MensajeService.crear` actualiza `Conversacion.ultimoMensajeAt` con la misma marca temporal del mensaje (`enviadoAt`). No hay update asincrono: se hace en la misma transaccion para evitar race conditions.
- `MensajeService.crear` notifica al otro participante via `NotificacionService.crear` con tipo `MENSAJE`. NO notifica al remitente. Si la conversacion tiene un solo participante (caso imposible por validacion de auto-conversacion), no notifica.
- `Mensaje` no se edita ni se borra por el usuario. Solo ADMIN puede eliminar. Alineado con la decision de `Notificacion`: el mensaje es un registro de comunicacion, no un campo mutable.