#!/bin/sh
# Publica únicamente el puerto del backend, sin conectar el backend a Internet.
set -eu
printf '#!/bin/sh\nexec nc backend 8080\n' > /tmp/kelly-forward
chmod 0500 /tmp/kelly-forward
exec nc -lk -p 8080 -e /tmp/kelly-forward
