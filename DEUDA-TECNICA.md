# Deuda Tecnica — Kelly Academy LMS Backend

Registro de decisiones postergadas a proposito. Actualizar el estado
cuando se resuelva, indicando el commit.

## Pendientes

| #  | Deuda | Feature  | Resolver en | Estado |
|----|---|----------|-------------|---|
| 1  | `Usuario.roles` con `FetchType.EAGER` — riesgo N+1 en listados paginados | user     | FASE 4      | Pendiente |
| 2  | `Rol.permisos` con `FetchType.EAGER` — agrava el punto 1 | user     | FASE 4      | Pendiente |
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

## Resueltos

| # | Deuda | Commit | Fecha |
|---|---|---|---|
| — | — | — | — |