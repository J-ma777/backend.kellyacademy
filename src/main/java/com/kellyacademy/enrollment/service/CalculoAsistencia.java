package com.kellyacademy.enrollment.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Fórmula común para instantáneas y lecturas de asistencia. */
final class CalculoAsistencia {
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);
    private static final BigDecimal PESO_TARDE = new BigDecimal("0.5");

    private CalculoAsistencia() {}

    static BigDecimal porcentaje(long clases, long presentes, long tardes, long justificados) {
        long denominador = clases - justificados;
        if (denominador <= 0) return null;
        return BigDecimal.valueOf(presentes)
                .add(BigDecimal.valueOf(tardes).multiply(PESO_TARDE))
                .multiply(CIEN)
                .divide(BigDecimal.valueOf(denominador), 2, RoundingMode.HALF_UP);
    }
}
