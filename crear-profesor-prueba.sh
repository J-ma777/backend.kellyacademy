#!/bin/bash
# ==============================================================================
# Kelly Academy LMS - Script de Creación de Docente / Profesor de Prueba
# ==============================================================================
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR"

CONTAINER_NAME="kelly-academy-postgres"
DB_NAME="kelly_academy"
DB_USER="postgres"

DOCENTE_EMAIL="profesor@kellyacademy.com"
DOCENTE_PASSWORD="Profesor123*"
DOCENTE_NOMBRE="Kelly"
DOCENTE_APELLIDO="Morales"
DOCENTE_ID="20000000-0000-0000-0000-000000000002"
ROL_DOCENTE_ID="10000000-0000-0000-0000-000000000002"

echo "=========================================================="
echo " Creando usuario profesor de prueba en Kelly Academy LMS  "
echo "=========================================================="

SQL_SCRIPT=$(cat <<EOF
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 1. Asegurar que el rol DOCENTE exista
INSERT INTO roles (id, nombre, descripcion, fecha_creacion, fecha_actualizacion)
VALUES ('$ROL_DOCENTE_ID', 'DOCENTE', 'Gestion academica', NOW(), NOW())
ON CONFLICT (nombre) DO NOTHING;

-- 2. Insertar o actualizar el usuario profesor
INSERT INTO usuarios (
    id,
    nombre,
    apellido,
    correo_electronico,
    contrasena,
    avatar_url,
    estado,
    fecha_creacion,
    fecha_actualizacion
)
VALUES (
    '$DOCENTE_ID',
    '$DOCENTE_NOMBRE',
    '$DOCENTE_APELLIDO',
    '$DOCENTE_EMAIL',
    crypt('$DOCENTE_PASSWORD', gen_salt('bf', 12)),
    NULL,
    'ACTIVO',
    NOW(),
    NOW()
)
ON CONFLICT (correo_electronico) DO UPDATE
SET contrasena = crypt('$DOCENTE_PASSWORD', gen_salt('bf', 12)),
    estado = 'ACTIVO',
    fecha_actualizacion = NOW();

-- 3. Vincular con el rol DOCENTE
INSERT INTO usuario_rol (usuario_id, rol_id)
SELECT u.id, r.id
FROM usuarios u, roles r
WHERE u.correo_electronico = '$DOCENTE_EMAIL'
  AND r.nombre = 'DOCENTE'
ON CONFLICT DO NOTHING;
EOF
)

if docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    echo "Conectando al contenedor Docker '$CONTAINER_NAME'..."
    docker exec -i "$CONTAINER_NAME" psql -U "$DB_USER" -d "$DB_NAME" -q <<EOF
$SQL_SCRIPT
EOF
else
    echo "Contenedor '$CONTAINER_NAME' no detectado. Intentando con psql local..."
    if command -v psql &> /dev/null; then
        PGPASSWORD="P.0.5#7.%.$.X" psql -h localhost -p 5432 -U "$DB_USER" -d "$DB_NAME" -q <<EOF
$SQL_SCRIPT
EOF
    else
        echo "Error: No se encontró el contenedor Docker '$CONTAINER_NAME' ni el comando local 'psql'."
        echo "Asegúrate de que la base de datos esté levantada: docker compose up -d postgres"
        exit 1
    fi
fi

echo ""
echo "=========================================================="
echo " ¡Usuario Profesor creado / actualizado con éxito!"
echo "=========================================================="
echo " Correo:      $DOCENTE_EMAIL"
echo " Contraseña:  $DOCENTE_PASSWORD"
echo " Rol:         DOCENTE"
echo " Nombre:      $DOCENTE_NOMBRE $DOCENTE_APELLIDO"
echo "=========================================================="
echo " Puedes iniciar sesión en el frontend: http://localhost:4200/login"
echo "=========================================================="
