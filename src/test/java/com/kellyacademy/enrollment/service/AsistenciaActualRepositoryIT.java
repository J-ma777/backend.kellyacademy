package com.kellyacademy.enrollment.service;

import com.kellyacademy.attendance.entity.Asistencia;
import com.kellyacademy.attendance.enums.EstadoAsistencia;
import com.kellyacademy.course.entity.Clase;
import com.kellyacademy.course.entity.Curso;
import com.kellyacademy.course.entity.Semana;
import com.kellyacademy.course.entity.Unidad;
import com.kellyacademy.course.enums.EstadoCurso;
import com.kellyacademy.course.enums.NivelCefr;
import com.kellyacademy.enrollment.entity.Matricula;
import com.kellyacademy.enrollment.enums.EstadoMatricula;
import com.kellyacademy.user.entity.Usuario;
import com.kellyacademy.user.enums.EstadoUsuario;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(AsistenciaActualService.class)
@ActiveProfiles("test")
@Transactional
class AsistenciaActualRepositoryIT {
    @Autowired EntityManager em;
    @Autowired AsistenciaActualService servicio;

    @Test
    void consultasRealesExcluyenAsistenciasDeClasesFuturasYReflejanPasoDelTiempo() {
        Usuario docente = usuario("docente@prueba.invalid");
        Usuario alumno = usuario("alumno@prueba.invalid");
        em.persist(docente); em.persist(alumno);
        Curso curso = new Curso(); curso.setDocente(docente); curso.setTitulo("Curso");
        curso.setNivelCefr(NivelCefr.A1); curso.setEstado(EstadoCurso.ACTIVO); em.persist(curso);
        Unidad unidad = new Unidad(); unidad.setCurso(curso); unidad.setNumero(1); unidad.setTitulo("Unidad"); em.persist(unidad);
        Semana semana = new Semana(); semana.setUnidad(unidad); semana.setNumero(1); semana.setTitulo("Semana"); em.persist(semana);
        LocalDateTime ahora = LocalDateTime.of(2026, 11, 10, 10, 0);
        Clase clase = new Clase(); clase.setSemana(semana); clase.setTitulo("Clase"); clase.setFechaHora(ahora.plusHours(1)); em.persist(clase);
        Matricula matricula = new Matricula(); matricula.setCurso(curso); matricula.setEstudiante(alumno);
        matricula.setEstado(EstadoMatricula.ACTIVA); matricula.setMatriculadoAt(ahora); em.persist(matricula);
        Asistencia asistencia = new Asistencia(); asistencia.setClase(clase); asistencia.setEstudiante(alumno);
        asistencia.setEstado(EstadoAsistencia.PRESENTE); asistencia.setRegistradoAt(ahora); em.persist(asistencia);
        em.flush();

        assertThat(servicio.calcularEn(List.of(matricula), ahora)).containsEntry(matricula.getId(), null);
        assertThat(servicio.calcularEn(List.of(matricula), ahora.plusHours(2)).get(matricula.getId()))
                .isEqualByComparingTo("100.00");
        assertThat(matricula.getAsistenciaPorcentaje()).isNull();
    }

    private Usuario usuario(String correo) {
        Usuario u = new Usuario(); u.setNombre("Usuario"); u.setApellido("Prueba");
        u.setCorreoElectronico(correo); u.setContrasena("hash-de-prueba"); u.setEstado(EstadoUsuario.ACTIVO);
        return u;
    }
}
