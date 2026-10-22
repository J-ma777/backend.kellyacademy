# Documentos en R2 e imágenes en Cloudinary

## Flujo del aula

- **Materiales:** el docente puede subir una imagen o documento, revisar el adjunto y guardar el recurso. El tipo y tamaño se completan con la respuesta real de la subida. Se conservan los enlaces externos.
- **Actividades, tareas, evaluaciones y quizzes:** título, instrucciones escritas, puntaje y plazo; se puede añadir una guía opcional en PDF, Office, TXT o imagen. El adjunto se guarda en `instrucciones_url` y acompaña la actividad. No sustituye la descripción ni modifica el flujo de calificación.
- **Anuncios:** texto obligatorio e imagen opcional. Se puede crear, editar, conservar o quitar la imagen sin hacerla obligatoria. Los borradores conservan las reglas de privacidad existentes.
- **Entregas del estudiante:** archivo de respuesta opcional (documento o imagen), texto o ambos. Se puede revisar el adjunto antes de enviar, reemplazarlo mientras la entrega no esté calificada y consultar la entrega propia. El docente abre el archivo desde Calificaciones y registra su nota y comentarios.

Se admite un adjunto por material, guía de actividad o entrega, y una imagen por anuncio. Elegir otro archivo reemplaza la referencia al guardar. Subir un archivo y guardar el contenido o enviar la entrega son pasos separados.

## Proveedor y persistencia

El backend decide el proveedor a partir del formato validado del archivo, no de una elección manipulable del navegador:

| Formato | Proveedor |
|---|---|
| JPG/JPEG, PNG, GIF y WebP | Cloudinary, tipo `authenticated` |
| PDF, DOC/DOCX, XLS/XLSX, PPT/PPTX y TXT | Cloudflare R2 |

El límite de la aplicación es 20 MB por archivo. Se mantienen las comprobaciones de extensión, cabeceras y estructura básica de Office, además de los límites de descompresión. No se permiten SVG ni ejecutables. Estas comprobaciones no son un antivirus.

La migración **V20** añade `archivos.proveedor` (CLOUDINARY o R2), amplía las finalidades admitidas y añade `anuncios.imagen_url`, nullable. Los registros previos conservan CLOUDINARY y sus enlaces; no se migran ni eliminan automáticamente los activos existentes.

PostgreSQL guarda nombre, MIME, tamaño, curso, autor de la subida, finalidad, proveedor y clave del objeto. No guarda el binario. Para R2, `public_id` contiene la clave del objeto y `url` contiene una referencia estable al backend; no se persiste una URL temporal que luego caduque.

## Acceso a documentos

`GET /api/cursos/{cursoId}/archivos/{archivoId}/acceso`, con JWT, devuelve `{url, nombre, mime, bytes, expiraEnSegundos}` y `Cache-Control: no-store`.

- El propietario del curso o administrador puede revisar sus adjuntos.
- El alumno puede consultar su archivo mientras conserva acceso al curso. Otro alumno no puede abrir ni presentar esa entrega como propia.
- Las guías y materiales de otros autores solo se ofrecen a alumnos matriculados cuando están vinculados a contenido publicado y visible.
- Retirar una matrícula bloquea nuevas solicitudes de acceso. Los enlaces R2 ya emitidos siguen siendo utilizables hasta caducar, como máximo cinco minutos.
- El frontend solicita un enlace nuevo cada vez que se abre el documento. No envía el JWT a R2 ni a Cloudinary.

Las imágenes conservan las URLs firmadas de Cloudinary. Esas URLs no caducan automáticamente y pueden compartirse; la comprobación de matrícula no revoca una URL de imagen que alguien ya haya copiado.

## Vista previa privada de PDF y Office

`GET /api/cursos/{cursoId}/archivos/{archivoId}/vista-previa`, con JWT, verifica los mismos permisos que la descarga y devuelve PDF con `Content-Disposition: inline` y `Cache-Control: no-store`.

- PDF: el backend lee los bytes de R2 y el navegador los dibuja con **PDF.js**, con controles de página, zoom, pantalla completa y lectura del texto de la página.
- Word (DOC/DOCX), Excel (XLS/XLSX), PowerPoint (PPT/PPTX) y TXT: el backend envía los bytes al servicio privado **Gotenberg/LibreOffice** y devuelve la vista PDF. El original permanece intacto en R2; la vista se genera a petición y no se persiste como otro archivo.
- Imágenes: se mantiene el visor de Cloudinary.
- El botón de apertura del original solicita un enlace nuevo de descarga; sigue funcionando si el conversor no está disponible o el documento está dañado o protegido con contraseña.

No hay que habilitar acceso público ni un visor en R2. Como el navegador recibe el PDF desde el backend con JWT, este flujo tampoco requiere CORS del bucket. Los documentos no se envían a visores públicos de Microsoft o Google. Los enlaces externos conservan su apertura original: la conversión de Office se admite para los archivos subidos al curso, evitando que el servidor descargue direcciones arbitrarias.

El contenedor `documentos` no publica puertos y solo comparte una red Docker interna con el backend. Las rutas de Chromium, las descargas de URLs y el acceso de LibreOffice a redes se deshabilitan. Se limitan memoria, CPU, tamaño, concurrencia y tiempo de conversión. Para producción conviene dimensionar este contenedor según la carga. Las fuentes y el diseño de Office pueden variar respecto a Microsoft Office; no se añade un editor de documentos.

Al iniciar el backend con Compose se inicia también el conversor:

```sh
# Desde Cloud-Academy
docker compose --project-directory backend.kellyacademy up -d --build backend
```

Si ejecutas Java desde el IDE, `DOCUMENT_CONVERTER_URL` debe apuntar a una instancia privada de Gotenberg accesible desde Java. El contenedor de Compose no expone un puerto al host por defecto.

## Pantalla de entrega del estudiante

Al abrir una actividad se muestra un resumen con fecha límite en Lima, puntuación máxima y estado real. «Ver instrucciones y preparar entrega» abre el visor y la respuesta en una columna, con detalles y calificación al lado. Se admite texto (con contador de palabras), un archivo por selección o arrastre de hasta 20 MB y revisión del adjunto antes de enviar. El botón de envío permanece bloqueado durante la subida.

El envío persiste al pulsar «Enviar entrega» o «Actualizar entrega»; no se ofrecen borradores guardados inexistentes. Se mantienen las entregas tardías y el bloqueo de modificaciones al calificar. No se muestran intentos restantes ni rúbricas porque estos datos aún no forman parte del modelo.

## Configuración del servidor

En `.env` del backend, sin subirlo a Git:

```dotenv
R2_ACCOUNT_ID=
R2_ACCESS_KEY_ID=
R2_SECRET_ACCESS_KEY=
R2_BUCKET=
R2_ENDPOINT=
R2_REGION=auto
APP_PUBLIC_URL=http://localhost:8080
```

El token debe tener **Object Read & Write**, limitado al bucket. Mantén el bucket privado. Copia el endpoint **S3 API** de Cloudflare; para un bucket normal es `https://<ACCOUNT_ID>.r2.cloudflarestorage.com`. Los buckets con jurisdicción usan su endpoint específico.

`APP_PUBLIC_URL` debe ser la dirección del backend accesible por el frontend. Configúrala para el entorno de destino antes de subir archivos allí. El frontend también debe utilizar ese backend en su configuración de API.

Docker Compose transmite las variables al backend. Después de cambiarlas, recrea el servicio:

```sh
# Desde Cloud-Academy
docker compose --project-directory backend.kellyacademy up -d --build backend
```

La integración usa **AWS SDK for Java v2**, compatible con la API S3 de R2, con transferencias fragmentadas desactivadas y comprobaciones de checksum cuando son requeridas. El backend puede arrancar sin credenciales R2; muestra un error claro si se intenta subir un documento sin configurarlas.

## Conservación y validación

Cancelar un formulario después de subir no elimina automáticamente el archivo ni sus metadatos. Borrar un contenido tampoco elimina automáticamente el objeto remoto; la limpieza de activos sin referencias no está automatizada. No se borran activos previos durante esta integración.

Las pruebas HTTP con H2 aislado verifican el proveedor elegido, persistencia de referencias estables, privacidad entre alumnos y cursos, publicación de guías, imágenes opcionales de anuncios, retiro de matrícula y rechazo de formatos. Las pruebas frontend cubren los formularios, la resolución de enlaces temporales y que el JWT solo se envíe al backend.

### Resultado local — 21 de octubre de 2026

- Configuración `.env`: campos presentes, endpoint HTTPS de R2 correspondiente al Account ID y acceso autenticado al bucket HTTP 200. No se expusieron credenciales.
- 46 pruebas frontend aprobadas; compilación de producción correcta.
- 51 pruebas backend aprobadas en almacenamiento, anuncios, entregas, aula y panel del curso.
- Docker actualizado y migración Flyway V20 aplicada correctamente en PostgreSQL.
- Prueba real a través del endpoint de subida del backend: PDF con destino ENTREGA y ACTIVIDAD, y DOCX con destino MATERIAL se guardaron en R2; PNG con destino ENTREGA y ANUNCIO se guardó en Cloudinary. En los cinco casos: subida HTTP 201, descarga HTTP 200 y contenido idéntico al original.
- Los enlaces R2 indicaron una duración de 300 segundos. El endpoint de acceso sin JWT respondió HTTP 401. La base conserva referencias estables sin la firma temporal.
- Los archivos de esta comprobación y sus metadatos se eliminaron; no quedaron registros temporales. Los casos de envío por alumnos, privacidad entre compañeros y persistencia de anuncios se comprobaron en H2 aislado, sin crear anuncios ni entregas ficticias en los cursos locales.
- No hubo navegador de automatización disponible para una revisión visual; se verificaron los componentes y sus plantillas mediante pruebas y compilación.

```sh
# frontend.kellyAcademy
npm test -- --watch=false
npm run build

# backend.kellyacademy
./mvnw -o -Dtest=ArchivoControllerIT,R2GatewayTest,ArchivoValidationTest,CloudinaryGatewayTest,AnuncioServiceTest,EntregaServiceTest,AulaCursoControllerIT,PanelCursoControllerIT test
```

Referencias: [SDK Java de R2](https://developers.cloudflare.com/r2/examples/aws/aws-sdk-java/), [URLs temporales](https://developers.cloudflare.com/r2/api/s3/presigned-urls/) y [credenciales y permisos](https://developers.cloudflare.com/r2/api/tokens/).


Referencias del visor: [PDF.js](https://mozilla.github.io/pdf.js/examples/), [conversión de Office en Gotenberg](https://gotenberg.dev/docs/convert-with-libreoffice/convert-to-pdf) y [configuración del conversor](https://gotenberg.dev/docs/configuration).


### Validación de vista previa y entrega — 21 de octubre de 2026

- 52 pruebas frontend aprobadas y compilación de producción sin advertencias de presupuesto de estilos.
- 56 pruebas backend aprobadas; incluyen vista previa antes/después de publicar, acceso entre docentes y alumnos, cabeceras de respuesta privada, conversión multipart y disponibilidad del original cuando falla Office.
- Servicios Docker `backend` y `documentos` saludables; Gotenberg 8.38.1 con LibreOffice iniciado.
- Prueba real: PDF, DOCX, XLSX y PPTX subidos con HTTP 201, vista PDF con HTTP 200, `Cache-Control: no-store`, y HTTP 401 sin JWT. Descarga de los cuatro originales idéntica byte a byte. Los cuatro objetos temporales y sus metadatos se eliminaron correctamente.
- Revisión visual en Chrome aislado, escritorio 1440 × 1050 y móvil 390 × 844: visor renderizado y zona de respuesta disponible, sin desbordamiento del contenido. Se utilizaron datos visuales aislados en el navegador, sin crear actividades ni entregas ficticias en PostgreSQL.
- Angular de desarrollo se reinició para servir los recursos de PDF.js en `/assets/pdfjs/`. Tras cambiar `angular.json` o instalar esta dependencia en otra máquina, reiniciar `npm run start`.
