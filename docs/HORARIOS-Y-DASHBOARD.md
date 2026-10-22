# Horarios de cursos y dashboard

## Uso

Al crear un curso, **Horario de clases** solicita el día de la semana, la hora de inicio y la hora de fin. Se pueden añadir varias sesiones semanales. La fecha de inicio del curso es obligatoria y la fecha de fin es opcional. Todas las horas académicas corresponden a `America/Lima`.

Para un curso existente, el docente abre su aula y pulsa **Configurar horario**. El formulario carga las sesiones guardadas y permite reemplazar su planificación. Los textos de horarios antiguos se conservan hasta configurar las sesiones; no se interpretan automáticamente porque pueden ser ambiguos.

El módulo **Horarios**, en `/calendar`, muestra una semana y permite avanzar, retroceder, volver a la semana actual o elegir una fecha. Integra las sesiones semanales, las clases concretas del aula, las tutorías y los eventos personales que el usuario puede consultar. Los alumnos ven las clases de sus cursos matriculados; los docentes ven las de sus propios cursos. Solamente se ofrece un enlace de acceso cuando existe uno real.

## Persistencia y reglas

- Flyway `V18__horarios_curso.sql` crea `horarios_curso` y amplía el resumen de horario del curso. Cada sesión guarda su curso, día, hora de inicio y hora de fin.
- Se admiten hasta 21 sesiones semanales por curso, con precisión de minutos. La hora de fin debe ser posterior a la de inicio dentro del mismo día.
- El servidor rechaza sesiones superpuestas del mismo curso y cruces con otros cursos activos del docente durante períodos coincidentes. Dos sesiones consecutivas son válidas. Un bloqueo de la fila del docente evita altas simultáneas con reservas dobles.
- Las sesiones se proyectan semanalmente para cursos activos, dentro de sus fechas de inicio y fin. La consulta del calendario admite intervalos de hasta 62 días.
- Una clase concreta del aula que coincide en curso, fecha y hora reemplaza la sesión proyectada en el calendario, conservando su título y enlace. Modificar la planificación semanal no cambia las fechas de esas clases concretas.
- Los alumnos solo reciben clases concretas publicadas y visibles. Las comprobaciones de matrícula y propiedad se realizan en el backend.
- Guardar el curso y sus sesiones es una operación transaccional. Las proyecciones semanales no crean recursos ficticios ni filas de eventos repetidas.

## Dashboard

El contador del alumno usa sus cursos distintos con matrícula no retirada, de acuerdo con **Mis cursos**. Incluye matrículas completadas y en riesgo; excluye las abandonadas. El docente ve el total de cursos creados en la academia. Sus clases, tareas y alumnos mantienen su ámbito correspondiente.

El frontend obtiene el resumen mediante una única petición por actualización, sin conservar una respuesta compartida entre sesiones. Se actualiza al recuperar el foco y cada 30 segundos mientras la ventana está visible. Si falla la API, muestra el error y permite reintentar. **Clases de hoy** incorpora las sesiones semanales autorizadas.

## API

| Método | Ruta | Función |
|---|---|---|
| POST | `/api/cursos` | Crear el curso con `horarios: [{dia, horaInicio, horaFin}]` |
| GET | `/api/cursos/{id}/horarios` | Consultar fechas y sesiones del curso autorizado |
| PUT | `/api/cursos/{id}/horarios` | Reemplazar fechas y sesiones como propietario o administrador |
| GET | `/api/calendario?desde=YYYY-MM-DD&hasta=YYYY-MM-DD` | Consultar la agenda autorizada por intervalo |
| GET | `/api/dashboard/resumen` | Consultar las métricas del usuario autenticado |

`dia` utiliza los valores ISO `MONDAY` a `SUNDAY`; las horas se envían como `HH:mm`. La actualización del horario recibe `fechaInicio`, `fechaFin` y `horarios`.

## Validación local

- 38 pruebas de frontend aprobadas y compilación de producción correcta.
- 33 pruebas de backend aprobadas en los servicios de cursos y las integraciones de aula, horarios y dashboard.
- Caso comprobado: un alumno matriculado en 1 de 10 cursos recibe un contador de 1; el docente recibe 10.
- Se verifican persistencia, límites de fechas, zona horaria, conflictos, edición por propietario, privacidad y sustitución de sesiones por clases concretas.
- Los datos sintéticos de las pruebas se crean en H2 aislado; no se añaden cursos ni matrículas de prueba a PostgreSQL local.
- No hubo navegador de automatización disponible para comprobar visualmente la interfaz.

### Integración con Docker y PostgreSQL (20 de octubre de 2026)

Backend reconstruido y saludable; Flyway V18 aplicada correctamente en PostgreSQL. Se comprobaron mediante HTTP las cuentas de desarrollo existentes: el contador docente coincide con los 3 cursos creados y el contador del alumno coincide con sus 0 matrículas actuales. El escenario de 1 matrícula entre 10 cursos se comprobó en la prueba de integración aislada.

La agenda devuelve HTTP 200 para ambos usuarios, el horario del curso existente se consulta correctamente y CORS permite `http://localhost:4200`. Las rutas del frontend `/courses`, `/calendar` y `/dashboard` responden HTTP 200. No se modificaron los cursos ni las matrículas locales para estas comprobaciones.

```sh
# Desde frontend.kellyAcademy
npm test -- --watch=false
npm run build

# Desde backend.kellyacademy
./mvnw -o -Dtest=HorariosDashboardIT,CursoServiceTest,AulaCursoControllerIT,PanelCursoControllerIT test

# Desde Cloud-Academy
docker compose --project-directory backend.kellyacademy up -d --build backend
```
