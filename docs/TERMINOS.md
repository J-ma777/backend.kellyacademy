# Términos y condiciones de uso

La aplicación solicita a cada cuenta autenticada que revise y acepte los términos vigentes antes de utilizar las funciones protegidas. La tarjeta incluye condiciones comunes y apartados para todos los roles de la cuenta. Puede volver a consultarse desde el perfil. La persona puede cerrar la sesión sin aceptar.

El backend registra la versión, los roles mostrados y la fecha de aceptación. Conserva las aceptaciones anteriores mientras exista la cuenta. Si cambia la versión o el conjunto de roles, se requiere una nueva aceptación. El servidor comprueba el estado en las solicitudes protegidas; el inicio de sesión, la consulta y la aceptación de términos siguen disponibles. La aceptación de términos es un registro de uso, no sustituye por sí sola las autorizaciones o avisos de tratamiento de datos que puedan corresponder.

## Actualizar los términos

1. Conserva una copia fechada del texto anterior, incluidos los apartados de cada rol.
2. Actualiza el texto de la tarjeta en el frontend y asigna una nueva versión tanto en `DISPLAYED_TERMS_VERSION` como en `TerminosService.VERSION`.
3. Publica coordinadamente backend y frontend, y verifica la aceptación para los diferentes roles. Si las versiones no coinciden, el frontend no permitirá aceptar un texto distinto del exigido por el servidor.

La migración `V23__aceptaciones_terminos.sql` crea la tabla de historial y se aplica con Flyway. Al instalar esta versión, incluso las cuentas existentes tendrán que aceptar los términos. No se eliminan tokens automáticamente; la API restringe las operaciones protegidas mientras la aceptación esté pendiente.

## Información pendiente

La academia atiende a estudiantes menores y mayores de edad. Debe revisar el contenido, completar su identificación legal, domicilio, canales de contacto y políticas de tratamiento de información, y definir y comprobar el procedimiento de autorización aplicable a menores. Está pendiente confirmar si hay estudiantes menores de 14 años. El botón de aceptación no verifica la identidad o representación de madres, padres o tutores y no constituye por sí mismo un mecanismo de autorización para menores.
