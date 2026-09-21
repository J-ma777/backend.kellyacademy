-- =========================================
-- V10 - Datos base del sistema
-- =========================================
-- Inserta: permisos, roles, rol_permiso, usuario admin, usuario_rol
-- Espeja exactamente lo que hace DataSeeder en perfil dev,
-- pero como migracion Flyway versionada para prod.
--
-- Idempotente: usa ON CONFLICT DO NOTHING con IDs fijos.
-- Seguro de re-ejecutar (aunque Flyway no lo hara).
-- =========================================

-- -----------------------------------------
-- 1. Permisos (espeja enum PermisoSistema)
-- -----------------------------------------
INSERT INTO permisos (id, nombre, descripcion, fecha_creacion, fecha_actualizacion)
VALUES
    ('00000000-0000-0000-0000-000000000001', 'GESTIONAR_USUARIOS',    'Administrar usuarios del sistema',        NOW(), NOW()),
    ('00000000-0000-0000-0000-000000000002', 'CREAR_CURSO',           'Crear nuevos cursos',                     NOW(), NOW()),
    ('00000000-0000-0000-0000-000000000003', 'EDITAR_CURSO',          'Editar cursos existentes',                NOW(), NOW()),
    ('00000000-0000-0000-0000-000000000004', 'ELIMINAR_CURSO',        'Eliminar cursos',                         NOW(), NOW()),
    ('00000000-0000-0000-0000-000000000005', 'VER_CURSOS',            'Ver listado y detalle de cursos',         NOW(), NOW()),
    ('00000000-0000-0000-0000-000000000006', 'CREAR_TAREA',           'Crear tareas',                            NOW(), NOW()),
    ('00000000-0000-0000-0000-000000000007', 'CALIFICAR_TAREA',       'Calificar tareas entregadas',             NOW(), NOW()),
    ('00000000-0000-0000-0000-000000000008', 'ENTREGAR_TAREA',        'Entregar tareas',                         NOW(), NOW()),
    ('00000000-0000-0000-0000-000000000009', 'VER_CALIFICACIONES',    'Ver calificaciones',                      NOW(), NOW()),
    ('00000000-0000-0000-0000-000000000010', 'ENVIAR_MENSAJES',       'Enviar mensajes en el sistema',           NOW(), NOW()),
    ('00000000-0000-0000-0000-000000000011', 'GESTIONAR_BIBLIOTECA',  'Gestionar recursos de biblioteca',        NOW(), NOW())
    ON CONFLICT (id) DO NOTHING;

-- -----------------------------------------
-- 2. Roles
-- -----------------------------------------
INSERT INTO roles (id, nombre, descripcion, fecha_creacion, fecha_actualizacion)
VALUES
    ('10000000-0000-0000-0000-000000000001', 'ADMINISTRADOR', 'Acceso total al sistema',  NOW(), NOW()),
    ('10000000-0000-0000-0000-000000000002', 'DOCENTE',       'Gestion academica',        NOW(), NOW()),
    ('10000000-0000-0000-0000-000000000003', 'ESTUDIANTE',    'Acceso estudiantil',       NOW(), NOW())
    ON CONFLICT (id) DO NOTHING;

-- -----------------------------------------
-- 3. Rol - Permisos
-- -----------------------------------------
-- ADMINISTRADOR: todos los permisos
INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT '10000000-0000-0000-0000-000000000001', id FROM permisos
    ON CONFLICT DO NOTHING;

-- DOCENTE: CREAR_TAREA, CALIFICAR_TAREA, VER_CURSOS, ENVIAR_MENSAJES
INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT '10000000-0000-0000-0000-000000000002', id
FROM permisos
WHERE nombre IN ('CREAR_TAREA', 'CALIFICAR_TAREA', 'VER_CURSOS', 'ENVIAR_MENSAJES')
    ON CONFLICT DO NOTHING;

-- ESTUDIANTE: VER_CURSOS, ENTREGAR_TAREA, ENVIAR_MENSAJES
INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT '10000000-0000-0000-0000-000000000003', id
FROM permisos
WHERE nombre IN ('VER_CURSOS', 'ENTREGAR_TAREA', 'ENVIAR_MENSAJES')
    ON CONFLICT DO NOTHING;

-- -----------------------------------------
-- 4. Usuario administrador
-- -----------------------------------------
-- Password: Admin123*  |  BCrypt strength 12
-- Hash generado con BCryptPasswordEncoder(12) sobre "Admin123*"
INSERT INTO usuarios (
    id, nombre, apellido, correo_electronico, contrasena, avatar_url,
    estado, fecha_creacion, fecha_actualizacion
)
VALUES (
           '20000000-0000-0000-0000-000000000001',
           'Administrador',
           'Sistema',
           'admin@kellyacademy.com',
           '$2a$12$klZvA4PK805KJx3i8CAooupoJBEPAz2lFF9hjmPziSuOFDXIgnX0m',
           NULL,
           'ACTIVO',
           NOW(),
           NOW()
       )
    ON CONFLICT (id) DO NOTHING;

-- -----------------------------------------
-- 5. Usuario - Rol
-- -----------------------------------------
INSERT INTO usuario_rol (usuario_id, rol_id)
VALUES (
           '20000000-0000-0000-0000-000000000001',
           '10000000-0000-0000-0000-000000000001'
       )
    ON CONFLICT DO NOTHING;