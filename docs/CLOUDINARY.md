# Subida de archivos con Cloudinary

Las imágenes nuevas se almacenan en Cloudinary. Los documentos nuevos se almacenan en R2; véase [ARCHIVOS-R2.md](ARCHIVOS-R2.md). PostgreSQL conserva su identificador, curso, usuario que lo subió, finalidad, nombre original, MIME, tamaño, `public_id`, tipo de recurso y URL firmada en la tabla `archivos` (Flyway V19). Materiales y entregas guardan esa URL en sus campos existentes; no se almacenan binarios en la base de datos.

## Activación

1. Abre tu consola de Cloudinary y selecciona el entorno de producto. En **API Keys**, obtén el Cloud name, API Key y API Secret.
2. Completa estas variables en `backend.kellyacademy/.env`. No compartas el secreto ni lo añadas al frontend o al repositorio:

   ```dotenv
   CLOUDINARY_CLOUD_NAME=
   CLOUDINARY_API_KEY=
   CLOUDINARY_API_SECRET=
   ```

3. Desde la raíz `Cloud-Academy`, actualiza el contenedor:

   ```sh
   docker compose --project-directory backend.kellyacademy up -d --build backend
   ```

   Si ejecutas Spring desde el IDE, reinicia el backend: el perfil `dev` importa su `.env` local.

4. En una cuenta gratuita, comprueba **Settings → Security → Allow delivery of PDF and ZIP files** para permitir entregar PDF. Este ajuste de Cloudinary no amplía la lista de formatos permitidos por la academia.
5. Prueba con un archivo propio: abre un curso, añade un material y selecciona **Subir archivo desde tu dispositivo**. Una vez subido, guarda el recurso. Para alumnos, el mismo control aparece al enviar una actividad.

El backend arranca aunque falten credenciales; al intentar subir muestra un error de configuración. Las credenciales solo salen del servidor hacia la API HTTPS oficial del proveedor.

## Formularios y formatos

- Materiales del curso: subida por el docente propietario o administrador. Se completan automáticamente URL, tamaño y tipo de material.
- Entregas del alumno: subida por un alumno con matrícula activa o en riesgo; después debe pulsar **Enviar entrega**. El docente también puede adjuntar un archivo al registrar una entrega manual.
- Se conservan los enlaces externos y las respuestas escritas existentes.
- Formatos habilitados: JPG/JPEG, PNG, GIF, WebP, PDF, DOC/DOCX, XLS/XLSX, PPT/PPTX y TXT. Máximo 20 MB por archivo; el plan de Cloudinary también puede imponer sus límites.
- El backend comprueba extensión y cabeceras; inspecciona la estructura de los documentos Office modernos con límites de descompresión. Rechaza SVG, ejecutables y formatos no habilitados. Estas comprobaciones no sustituyen un antivirus.
- Las imágenes se suben como `image`. Los documentos nuevos se envían a R2; los documentos históricos que ya estén en Cloudinary conservan sus referencias `raw`. Word, Excel y PowerPoint se descargan/abren con una aplicación compatible; esta integración no los convierte a PDF. Los PDF e imágenes pueden utilizar el visor actual.
- Se bloquean guardado y cierre del formulario mientras termina la subida y se muestran los errores del servidor.

## Acceso y ciclo de vida

Los activos usan el tipo `authenticated` de Cloudinary y URLs firmadas por el backend. No se publica una URL sin firma. La aplicación comprueba la pertenencia al curso y la finalidad antes de subir; las reglas existentes controlan quién recibe los enlaces al consultar materiales o entregas.

Una URL firmada es un enlace de acceso: quien la reciba puede compartirla. Las URLs guardadas no caducan automáticamente y retirar una matrícula no revoca una URL que ya se haya copiado. Para revocación individual y enlaces temporales se necesitaría un flujo de descarga autenticado adicional.

Subir y guardar un material o una entrega son pasos separados. Si se cancela el formulario después de subir, el archivo y sus metadatos permanecen. No se elimina automáticamente el activo de Cloudinary al borrar un material, curso o usuario; PostgreSQL sí limpia los metadatos mediante sus claves foráneas. Esto evita eliminar archivos que puedan estar referenciados en otros recursos. La limpieza de activos huérfanos queda como una operación posterior; no está automatizada.

## Endpoint

`POST /api/cursos/{cursoId}/archivos?destino=MATERIAL|ACTIVIDAD|ANUNCIO|ENTREGA`

Requiere el JWT del usuario y un cuerpo `multipart/form-data` con el campo `archivo`. Devuelve HTTP 201 con `{id, nombre, mime, bytes, url}`. El navegador no llama directamente a Cloudinary ni recibe la API Secret.

## Verificación

Pruebas de permisos por curso y matrícula, persistencia de metadatos, errores del proveedor, validación de formatos Office/imágenes y firma de la petición y URL de entrega contra un servidor HTTP simulado. Las pruebas no suben archivos a una cuenta real. La verificación contra la cuenta del usuario requiere sus credenciales locales.

### Verificación real — 21 de octubre de 2026

- Las tres credenciales del `.env` son válidas: la consulta autenticada a Cloudinary respondió HTTP 200.
- El contenedor conservaba un Cloud name anterior, aunque la API Key y API Secret coincidían. Se recargó con `docker compose up -d backend`, conservando los datos.
- Imagen PNG: subida HTTP 201; descarga mediante URL firmada HTTP 200; contenido idéntico al original; URL sin firma HTTP 401.
- Word DOCX: subida HTTP 201; descarga firmada HTTP 200; contenido idéntico al original; URL sin firma HTTP 401.
- PDF: subida HTTP 201 y metadatos correctos; descarga firmada HTTP 401 con `X-Cld-Error: deny or ACL failure`. La descarga de PDF todavía no está habilitada/verificada satisfactoriamente. Revisar **Settings → Security → Allow delivery of PDF and ZIP files**, documentado por Cloudinary para cuentas gratuitas; si ya está habilitado, revisar las restricciones de entrega del entorno.
- Todos los archivos temporales creados por estas pruebas y sus metadatos se eliminaron. No se modificaron los materiales ni las entregas existentes.
- Se corrigió `.gitignore`: `storage/` ignoraba accidentalmente el paquete Java y sus pruebas. `/storage/` limita la exclusión a los archivos locales de la raíz y permite versionar el código de integración. `.env` sigue excluido.

Las pruebas automatizadas previas cubren permisos, persistencia, formatos y firmas. La verificación de esta fecha comprueba la cuenta y el backend reales; no incluye revisión visual del navegador.

### Recomendación para documentos

Para separar el almacenamiento de documentos académicos, se recomienda **Cloudflare R2**, con bucket privado, claves de objeto persistidas y enlaces temporales generados por el backend después de comprobar la matrícula o propiedad del curso. Su API compatible con S3 permite integrarlo con AWS SDK for Java v2. PostgreSQL debe guardar proveedor, clave del objeto, nombre, MIME, tamaño y relación con el material o entrega; no debe guardar los archivos completos ni usar una URL temporal como referencia permanente.

Cloudinary continúa siendo adecuado para las imágenes y ya admite los documentos como `raw`; R2 ya está integrado para las subidas nuevas, como se describe en [ARCHIVOS-R2.md](ARCHIVOS-R2.md). Para visualizar Word o PowerPoint en el navegador se necesitaría además una conversión o visor compatible: almacenar el archivo no proporciona esa función automáticamente.

Referencias: [URLs temporales R2](https://developers.cloudflare.com/r2/api/s3/presigned-urls/) y [SDK Java compatible](https://developers.cloudflare.com/r2/examples/aws/aws-sdk-java/).

Documentación oficial: [carga y tipos de archivo](https://cloudinary.com/documentation/upload_parameters), [firmas de entrega](https://cloudinary.com/documentation/delivery_url_signatures) y [restricciones de PDF en cuentas gratuitas](https://cloudinary.com/documentation/image_delivery_options).
