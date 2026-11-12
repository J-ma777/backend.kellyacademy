# Dirección IP del cliente

`ClientIpResolver.resolve(HttpServletRequest)` devuelve un `Optional<String>` con un literal IPv4 o IPv6 normalizado. Un `Optional.empty()` significa que no hay una dirección confiable. El consumidor debe conservar ese estado como desconocido; no debe sustituirlo por `X-Forwarded-For` ni por una IP compartida para controles de seguridad.

La configuración `app.client-ip.mode` admite:

- `direct` (valor predeterminado en desarrollo y pruebas): usa exclusivamente la dirección TCP `getRemoteAddr()`, sin confiar en cabeceras. Es el modo para desarrollo, pruebas y tráfico directo.
- `render-public`: usa únicamente `CF-Connecting-IP`, siempre que aparezca una sola vez y contenga un literal válido. Falta, valores repetidos, listas o valores inválidos producen IP desconocida. `X-Forwarded-For`, `Forwarded` y otras cabeceras no intervienen.

El perfil de producción utiliza `render-public` por defecto porque la instalación indicada recibe tráfico exclusivamente por la URL pública de Render. Esta opción **solo es segura si todas las solicitudes alcanzan el servicio mediante el borde público de Render**, que sobrescribe esa cabecera. Si otro servicio puede conectar mediante la red privada de Render o si se expone el puerto por otra vía, revisar la topología antes de habilitar ese tráfico: en esas rutas un cliente podría suministrar la cabecera por sí mismo. El modo `direct` es adecuado para tráfico directamente conectado, pero en una topología mixta puede agrupar a todos los clientes públicos bajo la IP del proxy. Cambios de CDN, proxy o topología requieren revisar la configuración y verificarla contra documentación vigente. Si el borde no proporciona la cabecera, no hay fallback a una IP supuestamente real.

Según la [documentación de Render sobre IP y proxies](https://render.com/articles/host-pocketbase-on-render), Cloudflare sobrescribe `CF-Connecting-IP` para tráfico público de Render y una cadena `X-Forwarded-For` puede incluir valores controlados por el cliente. Las [reglas de IP de Render](https://render.com/docs/inbound-ip-rules) no se aplican al tráfico de la red privada. El procesado automático de cabeceras reenviadas de Spring está desactivado (`server.forward-headers-strategy=none`) para que una cabecera no modifique la dirección TCP usada por el modo directo; esto también afecta a la reconstrucción automática del esquema y host externo.

La auditoría de biblioteca guarda `NULL` si la IP es desconocida. El límite de acceso a login rechaza temporalmente la solicitud si no puede verificar la IP. El limitador guarda un resumen SHA-256 de la IP, sin correos ni contraseñas; no es un anonimizado irreversible, por lo que debe protegerse y eliminarse conforme a la política de datos del servicio.
