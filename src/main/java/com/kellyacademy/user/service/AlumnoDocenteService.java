package com.kellyacademy.user.service;

import com.kellyacademy.user.dto.alumno.AlumnoDtos.*;
import com.kellyacademy.user.entity.Usuario;
import com.kellyacademy.user.enums.EstadoUsuario;
import com.kellyacademy.user.repository.*;
import com.kellyacademy.course.enums.EstadoCurso;
import com.kellyacademy.course.repository.CursoRepository;
import com.kellyacademy.enrollment.enums.EstadoMatricula;
import com.kellyacademy.enrollment.repository.MatriculaRepository;
import com.kellyacademy.shared.config.AppTime;
import com.kellyacademy.shared.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@PreAuthorize("hasRole('DOCENTE')")
public class AlumnoDocenteService {
    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final CursoRepository cursos;
    private final MatriculaRepository matriculas;
    private final PasswordEncoder passwords;
    private static final Set<EstadoMatricula> OCUPAN_CUPO = Set.of(EstadoMatricula.ACTIVA, EstadoMatricula.RIESGO);

    @Transactional(readOnly=true)
    public Directorio listar() {
        var alumnos = usuarios.findAlumnosGestionables();
        var inscripciones = alumnos.isEmpty() ? List.<com.kellyacademy.enrollment.entity.Matricula>of()
                : matriculas.findByEstudianteIdIn(alumnos.stream().map(Usuario::getId).toList());
        var porAlumno = inscripciones.stream().collect(Collectors.groupingBy(m -> m.getEstudiante().getId()));
        // Los cupos incluyen cualquier estudiante del curso, también cuentas con más de un rol.
        var ocupados = matriculas.findAll().stream().filter(m -> OCUPAN_CUPO.contains(m.getEstado()))
                .collect(Collectors.groupingBy(m -> m.getCurso().getId(), Collectors.counting()));
        var opciones = cursos.findAll().stream().sorted(Comparator.comparing(com.kellyacademy.course.entity.Curso::getTitulo, String.CASE_INSENSITIVE_ORDER))
                .map(c -> new Curso(c.getId(), c.getTitulo(), c.getNivelCefr(), c.getEstado(), c.getCapacidadMaxima(), ocupados.getOrDefault(c.getId(),0L))).toList();
        return new Directorio(alumnos.stream().map(u -> respuesta(u,porAlumno.getOrDefault(u.getId(),List.of()))).toList(),opciones);
    }
    public Alumno crear(Crear r) {
        if (r.identificadorCorreo().contains("..")) throw new BusinessException("CORREO_INVALIDO", "El identificador del correo no puede contener puntos consecutivos");
        String correo = r.identificadorCorreo().toLowerCase(Locale.ROOT)+"@kellyacademy.com";
        if (usuarios.existsByCorreoElectronicoIgnoreCase(correo)) throw new CorreoYaRegistradoException(correo);
        if(r.contrasena().getBytes(StandardCharsets.UTF_8).length > 72)
            throw new BusinessException("CONTRASENA_LARGA", "La contraseña no puede superar 72 bytes");
        Usuario u = new Usuario();
        nombre(u,r.nombreCompleto());
        u.setCorreoElectronico(correo); u.setTelefono(r.telefono());
        u.setContrasena(passwords.encode(r.contrasena())); u.setEstado(EstadoUsuario.ACTIVO);
        u.setRoles(Set.of(roles.findByNombre("ESTUDIANTE").orElseThrow(() -> new BusinessException("ROL_INEXISTENTE","No existe el rol estudiante"))));
        return respuesta(usuarios.saveAndFlush(u),List.of());
    }
    public Alumno actualizar(UUID id, Actualizar r) {
        Usuario u = alumno(id);
        String correo = r.correoElectronico().trim().toLowerCase(Locale.ROOT);
        if(usuarios.existsByCorreoElectronicoIgnoreCaseAndIdNot(correo,id)) throw new CorreoYaRegistradoException(correo);
        nombre(u,r.nombreCompleto()); u.setCorreoElectronico(correo); u.setTelefono(r.telefono());
        usuarios.flush();
        return respuesta(u,matriculas.findByEstudianteId(id));
    }
    public Alumno cambiarEstado(UUID id, EstadoCuenta estado) {
        Usuario u = alumno(id);
        u.setEstado(switch(estado){case ACTIVO -> EstadoUsuario.ACTIVO; case INACTIVO -> EstadoUsuario.INACTIVO; case SUSPENDIDO -> EstadoUsuario.BLOQUEADO;});
        return respuesta(u,matriculas.findByEstudianteId(id));
    }
    public void eliminar(UUID id) {
        Usuario u = alumno(id);
        usuarios.delete(u);
        usuarios.flush();
    }
    public void inscribir(UUID id, UUID cursoId) {
        Usuario u = alumno(id);
        var curso = cursos.findByIdForUpdate(cursoId).orElseThrow(() -> new ResourceNotFoundException("Curso","id",cursoId));
        var actual = matriculas.findByCursoIdAndEstudianteId(cursoId,id).orElse(null);
        if(actual != null && actual.getEstado()!=EstadoMatricula.ABANDONADA) return;
        if(curso.getEstado()!=EstadoCurso.ACTIVO) throw new BusinessException("CURSO_NO_ACTIVO","Solo se pueden asignar cursos activos");
        if(matriculas.countByCursoIdAndEstadoIn(cursoId,OCUPAN_CUPO)>=curso.getCapacidadMaxima())
            throw new BusinessException("CURSO_SIN_CUPO","El curso ya no tiene cupos disponibles");
        if(actual==null) {
            actual = new com.kellyacademy.enrollment.entity.Matricula();
            actual.setCurso(curso); actual.setEstudiante(u); actual.setMatriculadoAt(LocalDateTime.now(AppTime.ZONA_NEGOCIO));
        }
        actual.setEstado(EstadoMatricula.ACTIVA);
        matriculas.save(actual);
    }
    public void retirar(UUID id, UUID cursoId) {
        alumno(id);
        if(!cursos.existsById(cursoId)) throw new ResourceNotFoundException("Curso","id",cursoId);
        matriculas.findByCursoIdAndEstudianteId(cursoId,id).ifPresent(m -> m.setEstado(EstadoMatricula.ABANDONADA));
    }
    private Usuario alumno(UUID id) {
        Usuario u = usuarios.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Alumno","id",id));
        if(u.getRoles().size()!=1 || u.getRoles().stream().noneMatch(r -> r.getNombre().equals("ESTUDIANTE")))
            throw new AccessDeniedException("Este módulo solo permite gestionar cuentas de estudiantes");
        return u;
    }
    private void nombre(Usuario u,String valor) {
        String limpio=valor.trim().replaceAll("\\s+"," "); int espacio=limpio.indexOf(' ');
        String nombre=espacio<0?limpio:limpio.substring(0,espacio);
        String apellido=espacio<0?"":limpio.substring(espacio+1);
        if(nombre.isBlank() || nombre.length()>100 || apellido.length()>100)
            throw new BusinessException("NOMBRE_INVALIDO","Revisa el nombre completo: nombre y apellidos admiten hasta 100 caracteres cada uno");
        u.setNombre(nombre); u.setApellido(apellido);
    }
    private Alumno respuesta(Usuario u,List<com.kellyacademy.enrollment.entity.Matricula> inscripciones) {
        EstadoCuenta estado=switch(u.getEstado()){case ACTIVO->EstadoCuenta.ACTIVO;case INACTIVO->EstadoCuenta.INACTIVO;case BLOQUEADO->EstadoCuenta.SUSPENDIDO;};
        return new Alumno(u.getId(),(u.getNombre()+" "+u.getApellido()).trim(),u.getCorreoElectronico(),u.getTelefono(),u.getAvatarUrl(),estado,
                inscripciones.stream().anyMatch(m -> m.getEstado()==EstadoMatricula.RIESGO),u.getFechaCreacion(),
                inscripciones.stream().map(m -> new Matricula(m.getId(),m.getCurso().getId(),m.getEstado(),m.getNotaFinal())).toList());
    }
}
