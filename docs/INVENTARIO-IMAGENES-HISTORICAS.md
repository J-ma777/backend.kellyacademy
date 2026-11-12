# Inventario previo de imágenes históricas

Las imágenes de Cloudinary subidas antes del cambio a referencias privadas pueden
conservar URLs firmadas permanentes. Antes de migrarlas, hay que identificar las
referencias y verificar cómo invalidar las URLs ya compartidas. Cambiar una referencia
en PostgreSQL no invalida automáticamente una URL previamente obtenida.

## Consulta de solo lectura

La siguiente consulta está preparada para PostgreSQL después de V20. Devuelve únicamente
cantidades: no imprime URLs, credenciales ni datos de estudiantes. Ejecutarla únicamente
en una conexión autorizada y con una copia de seguridad disponible. La consulta no se
ha ejecutado contra la base desplegada.

```sql
BEGIN TRANSACTION READ ONLY;

WITH antiguas AS (
    SELECT url
    FROM archivos
    WHERE proveedor = 'CLOUDINARY'
      AND resource_type = 'image'
      AND url LIKE 'https://res.cloudinary.com/%'
)
SELECT 'archivos' AS ubicacion, COUNT(*) AS cantidad FROM antiguas
UNION ALL
SELECT 'materiales.url_archivo', COUNT(*) FROM materiales m
WHERE EXISTS (SELECT 1 FROM antiguas a WHERE a.url = m.url_archivo)
UNION ALL
SELECT 'tareas.instrucciones_url', COUNT(*) FROM tareas t
WHERE EXISTS (SELECT 1 FROM antiguas a WHERE a.url = t.instrucciones_url)
UNION ALL
SELECT 'entregas.url_archivo', COUNT(*) FROM entregas e
WHERE EXISTS (SELECT 1 FROM antiguas a WHERE a.url = e.url_archivo)
UNION ALL
SELECT 'anuncios.imagen_url', COUNT(*) FROM anuncios n
WHERE EXISTS (SELECT 1 FROM antiguas a WHERE a.url = n.imagen_url)
UNION ALL
SELECT 'anuncios.cuerpo', COUNT(*) FROM anuncios n
WHERE EXISTS (SELECT 1 FROM antiguas a WHERE POSITION(a.url IN n.cuerpo) > 0)
UNION ALL
SELECT 'biblioteca.url_archivo', COUNT(*) FROM recursos_biblioteca b
WHERE EXISTS (SELECT 1 FROM antiguas a WHERE a.url = b.url_archivo);

ROLLBACK;
```

Este inventario solo detecta coincidencias exactas con registros de `archivos` y el
formato de URL histórico conocido. No identifica enlaces modificados, copiados fuera
de la plataforma o insertados en otros campos de texto. Sus cantidades son una base
para la revisión, no una demostración de que no haya más referencias.

## Trabajo pendiente

1. Comprobar en un entorno aislado que el proveedor acepta la nueva descarga temporal,
   incluida su caducidad, con una imagen sintética y limpieza del recurso creado.
2. Revisar las cantidades anteriores y los campos que almacenan texto enriquecido o
   enlaces externos; preparar una migración que conserve el vínculo al archivo y curso.
3. Probar la migración y su reversión sobre una copia aislada de la base, con validación
   de permisos para docentes, alumnos matriculados y alumnos retirados.
4. Acordar con Cloudinary la invalidación de enlaces firmados ya difundidos y comprobar
   su efecto antes de aplicarla; una URL compartida puede seguir siendo utilizable.
5. Revisar la migración y obtener la autorización correspondiente antes de modificar
   la base o activos publicados.
