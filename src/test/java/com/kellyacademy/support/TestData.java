package com.kellyacademy.support;

import com.kellyacademy.course.entity.*;
import com.kellyacademy.enrollment.entity.Matricula;
import com.kellyacademy.enrollment.enums.EstadoMatricula;
import com.kellyacademy.security.user.CustomUserDetails;
import com.kellyacademy.user.entity.*;
import com.kellyacademy.user.enums.EstadoUsuario;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.Set;
import java.util.UUID;

/** Datos sintéticos para pruebas unitarias, sin conexión a la base local. */
public final class TestData {
    private TestData() {}
    public static Usuario usuario(String role) {
        var rol = new Rol(); rol.setNombre(role);
        var user = new Usuario(); user.setId(UUID.randomUUID());
        user.setNombre("Ana"); user.setApellido("Prueba");
        user.setCorreoElectronico("ana@it.invalid"); user.setEstado(EstadoUsuario.ACTIVO);
        user.setRoles(Set.of(rol));
        return user;
    }
    public static void autenticar(Usuario user) {
        var details = new CustomUserDetails(user);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
    }
    public static Curso curso(Usuario docente) {
        var curso = new Curso(); curso.setId(UUID.randomUUID()); curso.setDocente(docente);
        curso.setTitulo("Curso unitario"); curso.setCapacidadMaxima(20);
        return curso;
    }
    public static Tarea tarea(Curso curso) {
        var unidad = new Unidad(); unidad.setId(UUID.randomUUID()); unidad.setCurso(curso);
        var semana = new Semana(); semana.setId(UUID.randomUUID()); semana.setUnidad(unidad);
        var tarea = new Tarea(); tarea.setId(UUID.randomUUID()); tarea.setSemana(semana);
        tarea.setTitulo("Actividad unitaria"); tarea.setPublicado(true); tarea.setVisible(true);
        return tarea;
    }
    public static Matricula matricula(Curso curso, Usuario user, EstadoMatricula estado) {
        var m = new Matricula(); m.setId(UUID.randomUUID()); m.setCurso(curso); m.setEstudiante(user); m.setEstado(estado);
        return m;
    }
}
