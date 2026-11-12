-- Una ventana por cliente, compartida entre instancias de la aplicación.
-- La clave es un resumen de la IP; no se guardan correos ni contraseñas.
CREATE TABLE auth_rate_limits (
    client_key CHAR(64) PRIMARY KEY,
    window_started_at TIMESTAMPTZ NOT NULL,
    attempts INTEGER NOT NULL CHECK (attempts > 0)
);
CREATE INDEX idx_auth_rate_limits_window ON auth_rate_limits (window_started_at);

-- Una dirección que no puede verificarse debe quedar como desconocida.
ALTER TABLE recurso_descargas ALTER COLUMN ip DROP NOT NULL;
