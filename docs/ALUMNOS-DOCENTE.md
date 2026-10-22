# Alumnos: gestión general para docentes

Ruta del frontend: `/students`. El enlace **Alumnos** aparece en el bloque **DOCENTES** del menú lateral. La ruta Angular y todos los endpoints exigen el rol DOCENTE; ESTUDIANTE y ADMINISTRADOR sin rol DOCENTE reciben acceso denegado.

Se muestran las cuentas cuyo único rol es ESTUDIANTE, con los datos reales de usuarios y matrículas de la academia. El módulo no permite modificar cuentas administrativas, docentes ni cuentas con varios roles.

## Flujos implementados

1. **Alta:** nombre completo, identificador institucional, teléfono peruano de 9 dígitos y contraseña inicial de al menos 8 caracteres. El backend normaliza el correo a minúsculas, añade `@kellyacademy.com`, valida duplicados con respuesta 409 y guarda la contraseña con el BCrypt de costo 12 existente. El teléfono se almacena sin espacios; la interfaz lo presenta como `999 159 716`.
2. **WhatsApp:** después del alta aparece una tarjeta flotante. Su botón abre `https://wa.me/51…` con el nombre, correo, contraseña inicial y la dirección `/login` del origen donde está abierta la aplicación. El docente revisa y envía el mensaje desde WhatsApp. No se envía automáticamente. El backend nunca devuelve la contraseña y el frontend no la guarda en localStorage/sessionStorage. Al cerrar la tarjeta o salir del módulo se descarta su enlace con las credenciales.
3. **Matrículas:** el modal lista todos los cursos persistidos, con su nivel y una referencia derivada de su UUID real, ya que el modelo actual no tiene un campo de código comercial. Los cursos inscritos aparecen seleccionados. Solo se pueden añadir cursos activos con cupos disponibles. El guardado inicia las altas y retiros en paralelo; si alguna operación falla, recarga el estado real, conserva la selección deseada y muestra el error para reintentar lo pendiente. Los bloqueos de base de datos impiden que las altas concurrentes de este módulo sobrepasen los cupos.
4. **Retiro de un curso:** cambia la matrícula a ABANDONADA y bloquea el acceso a ese curso, conservando entregas y notas. Una reasignación reactiva la misma matrícula y conserva su historial. La eliminación definitiva de la cuenta es una operación separada.
5. **Edición:** actualiza nombre, correo y teléfono; después aplica el cambio de estado, si corresponde. Los estados son Activo, Inactivo y Suspendido. Suspendido se traduce al estado BLOQUEADO que ya usa el backend. Las cuentas inactivas o suspendidas reciben 401 al intentar utilizar incluso un JWT todavía vigente. Cambiar el correo invalida los tokens asociados al correo anterior.
6. **Eliminación definitiva:** `DeleteConfirmModal` exige marcar la confirmación antes de habilitar el botón. El servidor elimina la cuenta y las relaciones dependientes mediante cascada. La fila solo inicia su transición de salida tras la respuesta de éxito y se retira del DOM después de 220 ms. Se respeta la preferencia de movimiento reducido.

## Filtros

La búsqueda por nombre y correo y los filtros Todos, Activo, En riesgo, Inactivo y Suspendido operan sobre el directorio cargado, sin peticiones por cada pulsación. El pie de la tabla recalcula “Mostrando X alumnos”.

**En riesgo** no es un estado de cuenta inventado: indica que una cuenta activa tiene al menos una matrícula en RIESGO. El filtro Activo muestra las cuentas activas sin ese indicador; la métrica “Con acceso activo” incluye también las activas en riesgo.

Los usuarios antiguos sin teléfono muestran “Sin teléfono”. No se rellenan números, matrículas ni alumnos de ejemplo.

## API

Base: `/api/docente/alumnos`.

| Método | Ruta relativa | Función |
|---|---|---|
| GET | `/` | Directorio y cursos disponibles |
| POST | `/` | Crear cuenta de estudiante |
| PATCH | `/{id}` | Editar datos básicos |
| PATCH | `/{id}/estado` | Cambiar acceso a la cuenta |
| POST | `/{id}/cursos` | Inscribir o reactivar una matrícula |
| DELETE | `/{id}/cursos/{cursoId}` | Retirar conservando historial |
| DELETE | `/{id}` | Eliminar definitivamente la cuenta |

La migración `V17__gestion_alumnos_docente.sql` añade el teléfono opcional a `usuarios` y ajusta las claves foráneas necesarias para la eliminación en cascada. No elimina cuentas existentes.

## Verificación

Las pruebas HTTP usan una base H2 aislada. Cubren contraseña BCrypt 12, correo duplicado, permisos por rol, protección de cuentas privilegiadas, bloqueo de sesiones, edición, multicurso, cupos concurrentes y eliminación con datos académicos y mensajes relacionados. Las pruebas Angular cubren los contratos HTTP, filtros, guard de ruta, formato telefónico, mensaje de WhatsApp, guardado parcial y confirmación de borrado.

```sh
# Desde frontend.kellyAcademy
npm test -- --watch=false
npm run build

# Desde backend.kellyacademy
./mvnw -Dtest=AlumnoDocenteControllerIT,UsuarioControllerIT,UsuarioServiceTest,AulaCursoControllerIT test
```

No hubo navegador de automatización disponible para una revisión visual. Se validaron las plantillas con Angular, pruebas de componentes y compilación de producción.

### Resultado local: 20 de octubre de 2026

- 31 pruebas Angular aprobadas y compilación de producción sin advertencias de presupuesto.
- 66 pruebas del backend aprobadas en las suites de alumnos, usuarios y aula.
- Verificación real: `/api/docente/alumnos` responde 200 para el docente existente y acepta el origen `http://localhost:4200`; `/students` responde desde Angular.
- Backend y PostgreSQL saludables en Docker; Flyway V17 aplicada y las ocho claves foráneas modificadas reportan `ON DELETE CASCADE`.
- La comprobación de la base local fue de lectura. No se crearon ni eliminaron cuentas reales para validar el módulo.
