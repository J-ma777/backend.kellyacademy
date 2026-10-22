-- Una calificacion perfecta normalizada es 100.00.
-- NUMERIC(4,2) solo permite dos digitos enteros y rechazaba ese resultado.
ALTER TABLE matriculas ALTER COLUMN nota_final TYPE NUMERIC(5, 2);

-- Las tareas admiten hasta 1000 puntos: una entrega de 1000/1000 tambien debe caber.
ALTER TABLE entregas ALTER COLUMN nota TYPE NUMERIC(6, 2);
