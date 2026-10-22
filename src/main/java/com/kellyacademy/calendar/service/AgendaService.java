package com.kellyacademy.calendar.service;

import com.kellyacademy.calendar.enums.EstadoTutoria;
import com.kellyacademy.calendar.repository.*;
import com.kellyacademy.course.enums.EstadoCurso;
import com.kellyacademy.course.repository.*;
import com.kellyacademy.enrollment.enums.EstadoMatricula;
import com.kellyacademy.enrollment.repository.MatriculaRepository;
import com.kellyacademy.shared.config.AppTime;
import com.kellyacademy.shared.exception.BusinessException;
import com.kellyacademy.shared.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class AgendaService {
    private final CursoRepository cursos;
    private final MatriculaRepository matriculas;
    private final HorarioCursoRepository horarios;
    private final ClaseRepository clases;
    private final TutoriaRepository tutorias;
    private final EventoRepository eventos;
    public record Item(UUID id,String tipo,UUID cursoId,String titulo,String cursoTitulo,String docenteNombre,
                       OffsetDateTime inicio,OffsetDateTime fin,String url,String estado) {}

    public List<Item> obtener(LocalDate desde,LocalDate hasta) {
        if(desde==null)desde=LocalDate.now(AppTime.ZONA_NEGOCIO);
        if(hasta==null)hasta=desde.plusDays(27);
        if(hasta.isBefore(desde)||ChronoUnit.DAYS.between(desde,hasta)>61)
            throw new BusinessException("RANGO_INVALIDO","Consulta un intervalo de entre 1 y 62 días");
        var usuario=SecurityUtils.getUsuarioAutenticado();UUID id=usuario.getUsuario().getId();boolean admin=SecurityUtils.esAdmin();
        boolean docente=usuario.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_DOCENTE"));
        Set<UUID> cursoIds=new HashSet<>();
        if(admin)cursos.findAll().forEach(c->cursoIds.add(c.getId()));
        else if(docente)cursos.findByDocenteId(id).forEach(c->cursoIds.add(c.getId()));
        else matriculas.findByEstudianteId(id).stream().filter(m->m.getEstado()!=EstadoMatricula.ABANDONADA).forEach(m->cursoIds.add(m.getCurso().getId()));
        LocalDateTime inicio=desde.atStartOfDay(),fin=hasta.atTime(LocalTime.MAX);var resultado=new ArrayList<Item>();var concretas=new HashSet<String>();
        for(var clase:clases.findByFechaHoraBetween(inicio,fin)) {
            var curso=clase.getSemana().getUnidad().getCurso();
            if(!cursoIds.contains(curso.getId())||(!admin&&!docente&&(!Boolean.TRUE.equals(clase.getPublicado())||!Boolean.TRUE.equals(clase.getVisible()))))continue;
            LocalDateTime termina=clase.getDuracionMinutos()==null?null:clase.getFechaHora().plusMinutes(clase.getDuracionMinutos());
            resultado.add(item(clase.getId(),"CLASE",curso.getId(),clase.getTitulo(),curso.getTitulo(),nombre(curso.getDocente()),clase.getFechaHora(),termina,clase.getUrlVivo(),null));
            concretas.add(curso.getId()+":"+clase.getFechaHora());
        }
        if(!cursoIds.isEmpty()) for(var horario:horarios.findByCursoIdIn(cursoIds)) {
            var curso=horario.getCurso();if(curso.getEstado()!=EstadoCurso.ACTIVO)continue;
            LocalDate primer=curso.getFechaInicio()!=null&&curso.getFechaInicio().isAfter(desde)?curso.getFechaInicio():desde;
            LocalDate ultimo=curso.getFechaFin()!=null&&curso.getFechaFin().isBefore(hasta)?curso.getFechaFin():hasta;
            for(LocalDate fecha=primer.with(TemporalAdjusters.nextOrSame(horario.getDia()));!fecha.isAfter(ultimo);fecha=fecha.plusWeeks(1)) {
                LocalDateTime comienza=fecha.atTime(horario.getHoraInicio());if(concretas.contains(curso.getId()+":"+comienza))continue;
                UUID ocurrencia=UUID.nameUUIDFromBytes((horario.getId()+":"+fecha).getBytes(StandardCharsets.UTF_8));
                resultado.add(item(ocurrencia,"CURSO",curso.getId(),curso.getTitulo(),curso.getTitulo(),nombre(curso.getDocente()),comienza,fecha.atTime(horario.getHoraFin()),null,null));
            }
        }
        for(var tutoria:tutorias.findByFechaBetween(inicio,fin)) {
            if(tutoria.getEstado()==EstadoTutoria.CANCELADA||(!admin&&!tutoria.getDocente().getId().equals(id)&&!tutoria.getEstudiante().getId().equals(id)))continue;
            var curso=tutoria.getCurso();resultado.add(item(tutoria.getId(),"TUTORIA",curso==null?null:curso.getId(),"Tutoría",curso==null?null:curso.getTitulo(),nombre(tutoria.getDocente()),tutoria.getFecha(),tutoria.getFecha().plusMinutes(tutoria.getDuracionMinutos()),null,tutoria.getEstado().name()));
        }
        for(var evento:eventos.findByInicioBetween(inicio,fin)) {
            if(!admin&&!evento.getUsuario().getId().equals(id))continue;
            var curso=evento.getCurso();resultado.add(item(evento.getId(),"EVENTO",curso==null?null:curso.getId(),evento.getTitulo(),curso==null?null:curso.getTitulo(),curso==null?null:nombre(curso.getDocente()),evento.getInicio(),evento.getFin(),null,null));
        }
        return resultado.stream().sorted(Comparator.comparing(Item::inicio).thenComparing(Item::titulo)).toList();
    }
    private Item item(UUID id,String tipo,UUID cursoId,String titulo,String curso,String docente,LocalDateTime inicio,LocalDateTime fin,String url,String estado) {
        var ahora=LocalDateTime.now(AppTime.ZONA_NEGOCIO);
        String situacion=estado;
        if(!"PENDIENTE".equals(estado)) {
            if(fin!=null&&!ahora.isBefore(inicio)&&ahora.isBefore(fin))situacion="EN_CURSO";
            else if((fin!=null&&!fin.isAfter(ahora))||(fin==null&&inicio.isBefore(ahora)))situacion="FINALIZADA";
            else if(estado==null)situacion="PROGRAMADA";
        }
        return new Item(id,tipo,cursoId,titulo,curso,docente,inicio.atZone(AppTime.ZONA_NEGOCIO).toOffsetDateTime(),fin==null?null:fin.atZone(AppTime.ZONA_NEGOCIO).toOffsetDateTime(),url,situacion);
    }
    private String nombre(com.kellyacademy.user.entity.Usuario u){return (u.getNombre()+" "+u.getApellido()).trim();}
}
