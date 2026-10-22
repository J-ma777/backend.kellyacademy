package com.kellyacademy.user.dto.alumno;

import com.kellyacademy.course.enums.EstadoCurso;
import com.kellyacademy.course.enums.NivelCefr;
import com.kellyacademy.enrollment.enums.EstadoMatricula;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class AlumnoDtos {
    private AlumnoDtos() {}
    public record Crear(
            @NotBlank @Size(max=200) String nombreCompleto,
            @NotBlank @Pattern(regexp="[A-Za-z0-9](?:[A-Za-z0-9._-]{0,62}[A-Za-z0-9])?", message="Introduce solo el identificador del correo institucional") String identificadorCorreo,
            @NotBlank @Pattern(regexp="[0-9]{9}", message="El teléfono debe tener 9 dígitos") String telefono,
            @NotBlank @Size(min=8,max=72) String contrasena) {}
    public record Actualizar(
            @NotBlank @Size(max=200) String nombreCompleto,
            @NotBlank @Email @Size(max=255) String correoElectronico,
            @NotBlank @Pattern(regexp="[0-9]{9}", message="El teléfono debe tener 9 dígitos") String telefono) {}
    public enum EstadoCuenta { ACTIVO, INACTIVO, SUSPENDIDO }
    public record CambiarEstado(@NotNull EstadoCuenta estado) {}
    public record Inscribir(@NotNull UUID cursoId) {}
    public record Matricula(UUID id, UUID cursoId, EstadoMatricula estado, BigDecimal notaFinal) {}
    public record Alumno(UUID id, String nombreCompleto, String correoElectronico, String telefono,
                         String avatarUrl, EstadoCuenta estado, boolean enRiesgo, LocalDateTime creadoAt, List<Matricula> matriculas) {}
    public record Curso(UUID id, String titulo, NivelCefr nivelCefr, EstadoCurso estado, int capacidadMaxima, long inscritos) {}
    public record Directorio(List<Alumno> alumnos, List<Curso> cursos) {}
}
