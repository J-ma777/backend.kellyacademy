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

## Resueltos

| # | Deuda | Commit | Fecha |
|---|---|---|---|
| — | — | — | — |