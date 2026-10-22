# Aula del curso

La ruta Angular `/courses/:courseId` contiene cuatro secciones: Contenido, Alumnos, Anuncios y Calificaciones. Consume el backend Spring Boot y PostgreSQL; las listas vacías representan datos pendientes de crear. No se insertan cursos, alumnos ni recursos de demostración en la base local.

El aula consulta cambios cada 20 segundos mientras está visible y al volver a la ventana. Cada operación guardada vuelve a cargar el estado persistido del servidor. La sincronización se pausa mientras se edita.

## Uso

- **Contenido:** crear módulos y semanas; añadir clases, materiales por enlace y actividades de tipo tarea, evaluación o quiz con instrucciones, plazo y puntaje. Cada recurso tiene indicadores independientes de publicación y visibilidad. Los recursos nuevos se preparan en borrador. El visor abre videos de YouTube/Vimeo, MP4, audio, imágenes y PDF por HTTPS; conserva un enlace para abrir o descargar el original si el proveedor impide incrustarlo.
- **Alumnos:** consultar matrícula, correo, avatar o iniciales, estado, fecha y progreso. El retiro cambia la matrícula a ABANDONADA y bloquea el acceso al aula; conserva las entregas y calificaciones. Los compañeros tienen un directorio sin controles administrativos ni notas de terceros.
- **Anuncios:** editar texto con formato, guardar borradores, publicar, fijar y eliminar. El servidor limpia el HTML con Jsoup. Los fijados aparecen primero y después se ordenan por publicación. Un borrador no envía notificaciones; al publicarlo se notifica a los alumnos activos.
- **Calificaciones:** matriz de alumnos por actividad, incluyendo trabajos sin entregar o vencidos; revisión de archivo enlazado o texto, fecha, retraso, nota y retroalimentación. El alumno envía su propia respuesta y puede reemplazarla hasta ser calificada. Puede consultar únicamente sus entregas. El docente puede registrar una entrega en nombre de un alumno para trabajos grupales o recibidos por otro medio.

Los quizzes y evaluaciones usan el flujo de entrega y calificación docente. No incluyen corrección automática ni un constructor de preguntas.

## Reglas de datos

- El backend comprueba propietario del curso o administrador para las modificaciones.
- Alumnos matriculados reciben solamente recursos publicados y visibles, y anuncios activos y publicados. Estos filtros también protegen las rutas individuales/listados de contenido y el resumen del dashboard.
- Progreso individual = recursos pasivos marcados como revisados + actividades entregadas, dividido entre el contenido publicado y visible. Un recurso se cuenta una sola vez. Si no hay contenido, el progreso se devuelve como `null`.
- El promedio del grupo excluye matrículas retiradas. El docente puede consultar el historial de estas matrículas en el libro de notas.
- La nota final conserva el cálculo existente: suma de notas calificadas dividida entre sus puntajes máximos, multiplicada por 100. Las actividades pendientes no se convierten automáticamente en cero. Una actividad ya calificada conserva su puntaje máximo.
- Las fechas académicas se interpretan en America/Lima. Una entrega posterior al límite conserva su indicador de retraso incluso después de calificarla.
- La migración Flyway `V16__flujo_aula.sql` añade publicación/visibilidad, tipo de actividad, respuesta escrita, anuncios fijados y progreso. Los recursos existentes mantienen su disponibilidad inicial.

## API del aula

| Método | Ruta | Función |
|---|---|---|
| GET | `/api/cursos/{cursoId}/aula` | Datos del aula según el usuario autenticado |
| POST | `/api/cursos/{cursoId}/contenido/{tipo}` | Crear contenido |
| PUT | `/api/cursos/{cursoId}/contenido/{tipo}/{id}` | Editar contenido |
| PATCH | `/api/cursos/{cursoId}/contenido/{tipo}/{id}/publicacion` | Publicar u ocultar |
| POST | `/api/cursos/{cursoId}/contenido/{tipo}/{id}/completar` | Registrar revisión del alumno |
| DELETE | `/api/cursos/{cursoId}/alumnos/{estudianteId}` | Retirar matrícula conservando historial |
| POST | `/api/entregas/enviar` | Envío propio por archivo enlazado, texto o ambos |

`tipo`: MATERIAL, CLASS o TASK. Las actividades se completan con la entrega, no con el marcador de revisión. Estructura, anuncios y calificaciones reutilizan los endpoints existentes.

## Archivos con Cloudinary

Los materiales, guías de actividades y entregas admiten archivos locales: las imágenes se guardan en Cloudinary y los documentos en R2. Los anuncios permiten una imagen opcional. Se conservan los enlaces y respuestas escritas. Las credenciales se configuran exclusivamente en el servidor. Véanse [ARCHIVOS-R2.md](ARCHIVOS-R2.md) y [CLOUDINARY.md](CLOUDINARY.md) para la configuración, formatos, permisos y conservación.

## Validación

Pruebas Angular de contratos HTTP, navegación y las cuatro secciones, matriz con entregas faltantes, permisos de interfaz, respuesta escrita y seguridad del visor. Pruebas HTTP con Spring Boot y H2 aislado para publicación, privacidad, progreso idempotente, HTML seguro, anuncios fijados, envío tardío, calificación y retiro con historial.

```sh
# Desde frontend.kellyAcademy
npm test -- --watch=false
npm run build

# Desde backend.kellyacademy
./mvnw -Dtest=AulaCursoControllerIT,PanelCursoControllerIT,EntregaControllerIT,MaterialServiceTest,ClaseServiceTest,TareaServiceTest,AnuncioServiceTest,EntregaServiceTest,CalculoMatriculaServiceTest test

# Desde Cloud-Academy
docker compose --project-directory backend.kellyacademy up -d --build backend
```

### Resultado local (20 de octubre de 2026)

- 18 pruebas del frontend aprobadas y compilación de producción correcta.
- 121 pruebas distintas del backend aprobadas en las ejecuciones de integración y servicios relacionados.
- Docker: backend y PostgreSQL saludables; Flyway V15 y V16 aplicadas correctamente.
- Verificación HTTP real: frontend 4200 y aula 8080 responden 200; CORS acepta `http://localhost:4200`.
- La cuenta docente existente tiene un curso sin módulos, alumnos ni anuncios. Se conserva ese estado real y se muestran las opciones de creación correspondientes.
- No hubo navegador de automatización disponible para una revisión visual; se verificaron las plantillas con Angular, pruebas de componentes y compilación.

## Gestión general de alumnos

El nuevo módulo `/students`, disponible en **DOCENTES → Alumnos**, permite crear y editar cuentas, preparar sus credenciales para WhatsApp, gestionar matrículas de varios cursos y eliminar definitivamente cuentas con confirmación. Es independiente de la pestaña Alumnos de cada aula. Véase [ALUMNOS-DOCENTE.md](ALUMNOS-DOCENTE.md).

## Planificación de clases

Los cursos admiten sesiones semanales con día, hora de inicio y hora de fin. Se configuran al crear el curso o desde **Configurar horario** dentro del aula y aparecen en el módulo **Horarios** y en las clases del día del dashboard. Véase [HORARIOS-Y-DASHBOARD.md](HORARIOS-Y-DASHBOARD.md).


### Vista previa y nueva entrega

PDF y documentos Office subidos al curso disponen de vista previa privada mediante el backend, PDF.js y Gotenberg. Las imágenes conservan Cloudinary. La actividad del estudiante separa resumen, instrucciones y respuesta, con arrastre de un archivo de hasta 20 MB, contador de palabras y datos reales del plazo, estado y nota. Consulte [ARCHIVOS-R2.md](ARCHIVOS-R2.md) para configuración y límites. Las reglas de entregas tardías y bloqueo al calificar permanecen vigentes.
