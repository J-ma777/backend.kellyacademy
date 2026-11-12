# Despliegue en Render

Estos pasos describen un backend Docker y un frontend Angular como sitio estático. Usa las URL reales asignadas por Render o tus dominios propios; las direcciones del ejemplo no son valores para producción.

## Backend

1. Crea un **Web Service** desde el repositorio del backend y selecciona Docker. Si el repositorio conectado contiene ambos proyectos, ajusta el directorio raíz a `backend.kellyacademy`. La imagen activa el perfil `prod`; comprueba que ninguna variable de Render lo sobrescriba con `dev`.
2. Configura las variables de entorno en el panel de Render, sin guardar secretos en el repositorio:

   | Variable | Valor |
   | --- | --- |
   | `SPRING_PROFILES_ACTIVE` | `prod` |
   | `DB_URL` | URL JDBC, por ejemplo `jdbc:postgresql://HOST:5432/BASE?sslmode=require` |
   | `DB_USERNAME`, `DB_PASSWORD` | Usuario y contraseña de PostgreSQL |
   | `JWT_SECRET` | Secreto Base64 aleatorio; se puede generar con `openssl rand -base64 64` |
   | `JWT_EXPIRATION` | Vigencia en milisegundos, por ejemplo `3600000` |
   | `APP_BOOTSTRAP_ADMIN_PASSWORD` | Contraseña inicial segura cuando la cuenta histórica aún usa la contraseña conocida; ver [README](../README.md#cuenta-administradora-inicial-y-sesiones) |
   | `APP_CORS_ALLOWED_ORIGINS` | Orígenes exactos del frontend separados por comas, por ejemplo `https://academia.onrender.com,https://academia.ejemplo.com` |
   | `APP_PUBLIC_URL` | Origen HTTPS público del backend, por ejemplo `https://api-academia.onrender.com` |

   La URL JDBC no es la URL `postgres://usuario:contraseña@...` que entregan algunos proveedores: usa el host, puerto y base para formar la URL y coloca usuario y contraseña en sus variables respectivas. Usa TLS según tu proveedor. Para Render Postgres, utiliza la dirección interna cuando ambos servicios estén en la misma región y cuenta. Flyway ejecuta las migraciones al arrancar; conserva una copia de seguridad de una base existente antes de actualizarla.

3. Configura la ruta HTTP de salud de Render en `/actuator/health`. Render proporciona `PORT`; Spring y el healthcheck del contenedor utilizan ese puerto. Revisa los registros de arranque y confirma que el servicio esté `Live`.
4. Para imágenes y documentos, configura `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`, `R2_ENDPOINT`, `R2_ACCESS_KEY_ID`, `R2_SECRET_ACCESS_KEY` y `R2_BUCKET`. El arranque puede funcionar sin estas credenciales, pero las funciones de archivos correspondientes fallarán. Para la vista previa de Office y TXT, configura `DOCUMENT_CONVERTER_URL` con un conversor Gotenberg privado accesible desde el backend; el Dockerfile del backend por sí solo no crea ese servicio. Consulta [ARCHIVOS-R2.md](ARCHIVOS-R2.md) y [CLOUDINARY.md](CLOUDINARY.md).

El origen CORS contiene esquema, dominio y puerto cuando corresponda, sin ruta ni barra final. Incluye cada dominio real desde el que se abre el frontend; el dominio de la API no sustituye al del frontend. Si se habilitan previsualizaciones con dominios variables, permite explícitamente cada origen que corresponda. La configuración de IP y su supuesto de acceso por el borde público se describen en [IP-CLIENTE.md](IP-CLIENTE.md).

## Frontend estático

1. Crea un **Static Site** desde el repositorio del frontend. Si ambos proyectos están en el mismo repositorio conectado, establece `frontend.kellyAcademy` como directorio raíz.
2. Usa `npm ci && npm run build && cp dist/frontend.kellyAcademy/browser/index.csr.html dist/frontend.kellyAcademy/browser/index.html` como comando de compilación y `dist/frontend.kellyAcademy/browser` como directorio de publicación. La copia proporciona el `index.html` que sirve la raíz del sitio estático.
3. En Redirects/Rewrites, crea una regla **Rewrite** de `/*` a `/index.html` para que las rutas de Angular funcionen al recargar la página. Añade una cabecera para `/runtime-config.js` con `Cache-Control: no-store`.
4. El archivo público `public/runtime-config.js` configura el origen de la API. Si el backend utiliza otra URL, modifica el archivo antes de compilar:

   ```js
   window.__KELLY_CONFIG__ = { apiUrl: 'https://api-academia.onrender.com' };
   ```

   No pongas claves ni contraseñas en este archivo: el navegador lo descarga. Publica el frontend y agrega su origen HTTPS exacto a `APP_CORS_ALLOWED_ORIGINS` del backend. Guarda los cambios y vuelve a desplegar el backend cuando sea necesario.

## Comprobación después de publicar

Comprueba `/actuator/health` desde el navegador y recarga una ruta interna del frontend. Para revisar la respuesta de preflight, sustituye ambos dominios por los reales:

```sh
curl -i -X OPTIONS 'https://api-academia.onrender.com/auth/login' \
  -H 'Origin: https://academia.onrender.com' \
  -H 'Access-Control-Request-Method: POST' \
  -H 'Access-Control-Request-Headers: authorization,content-type'
```

La respuesta debe incluir `Access-Control-Allow-Origin` con el origen enviado y permitir el método y las cabeceras solicitadas. A continuación verifica el inicio de sesión y una consulta de datos desde el navegador, incluyendo la pestaña de red. Prueba también la subida y apertura de los tipos de archivo que utilizarán los usuarios. Un preflight correcto no demuestra por sí solo que PostgreSQL, las credenciales de proveedores ni el flujo autenticado funcionen.

Referencias de Render: [Web Services](https://render.com/docs/web-services), [health checks](https://render.com/docs/health-checks), [PostgreSQL](https://render.com/docs/postgresql-creating-connecting), [rewrites](https://render.com/docs/redirects-rewrites) y [cabeceras de sitios estáticos](https://render.com/docs/static-site-headers).
