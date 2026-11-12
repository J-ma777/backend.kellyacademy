# Biblioteca

La biblioteca comparte un catálogo entre usuarios autenticados. Los estudiantes pueden buscar, filtrar, consultar detalles y solicitar recursos. Los docentes y administradores también pueden agregar y editar recursos; solamente los administradores pueden eliminarlos. Estas reglas se aplican en el backend.

## Archivos y enlaces

Al crear o editar un recurso, un docente o administrador puede subir un archivo de hasta 20 MB o agregar un enlace externo. Se admiten imágenes JPG, PNG, GIF y WebP; PDF; Word, Excel y PowerPoint; y texto. Los recursos de audio y video pueden compartirse mediante enlaces externos.

Las imágenes nuevas se guardan en Cloudinary y los documentos en R2. Ambos proveedores deben estar configurados en el backend. Los archivos de biblioteca se gestionan por separado de los archivos privados de cursos, por lo que no requieren que el estudiante pertenezca a un curso concreto. Los recursos históricos pueden conservar enlaces anteriores.

Al solicitar un recurso se registra la operación con el usuario, la fecha y la IP cuando se puede determinar de forma confiable. Para un archivo nuevo de biblioteca se entrega un enlace temporal de cinco minutos. El registro indica que se solicitó el enlace: no permite confirmar que el navegador descargó el archivo. Una vez entregados, los enlaces externos pueden utilizarse directamente y no es posible auditar sus accesos posteriores desde la aplicación.

La eliminación de un recurso elimina también su historial de descargas por la relación existente en la base de datos. No borra automáticamente el archivo en el proveedor. Si una carga se cancela o el guardado del recurso falla después de subirlo, puede quedar un archivo sin vincular; el formulario permite reintentar el guardado sin repetir la subida mientras permanece abierto. La limpieza de archivos sin uso requiere un procedimiento específico que compruebe sus referencias antes de borrarlos.

## Puesta en marcha

La migración Flyway `V22__archivos_biblioteca.sql` crea la tabla para los archivos de biblioteca. La aplicación debe actualizarse junto con el frontend. En Docker Compose, reconstruye el backend con `docker compose up -d --build` desde la carpeta del backend; Flyway aplica las migraciones pendientes al arrancar. Confirma la configuración de R2 y Cloudinary sin compartir sus credenciales.

Los permisos y el flujo de carga y descarga se comprueban con almacenamiento simulado. Es necesario verificar las operaciones con los proveedores reales y los datos de cada entorno antes de darlas por aceptadas.
