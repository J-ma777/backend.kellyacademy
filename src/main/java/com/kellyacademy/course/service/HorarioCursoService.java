package com.kellyacademy.course.service;

import com.kellyacademy.course.dto.request.*;
import com.kellyacademy.course.entity.*;
import com.kellyacademy.course.repository.HorarioCursoRepository;
import com.kellyacademy.user.repository.UsuarioRepository;
import com.kellyacademy.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor @Transactional
public class HorarioCursoService {
    private final HorarioCursoRepository horarios;
    private final UsuarioRepository usuarios;
    private final AccesoCursoService acceso;
    public record Plan(LocalDate fechaInicio,LocalDate fechaFin,List<HorarioCursoRequest> horarios) {}
    @Transactional(readOnly=true)
    public Plan obtener(UUID id) {
        var curso=acceso.obtener(id);
        return new Plan(curso.getFechaInicio(),curso.getFechaFin(),curso.getHorarios().stream()
                .sorted(Comparator.comparing(HorarioCurso::getDia).thenComparing(HorarioCurso::getHoraInicio))
                .map(h->new HorarioCursoRequest(h.getDia(),h.getHoraInicio(),h.getHoraFin())).toList());
    }
    public void guardar(UUID id,GuardarHorarioCursoRequest r) {
        var curso=acceso.obtener(id);acceso.validarGestor(curso);
        curso.setFechaInicio(r.fechaInicio());curso.setFechaFin(r.fechaFin());aplicar(curso,r.horarios());
    }
    public void aplicar(Curso curso,List<HorarioCursoRequest> bloques) {
        if(curso.getFechaInicio()==null) throw new BusinessException("INICIO_REQUERIDO","Indica la fecha de inicio para programar las clases");
        if(curso.getFechaFin()!=null && curso.getFechaFin().isBefore(curso.getFechaInicio()))
            throw new BusinessException("FECHAS_INVALIDAS","La fecha de fin no puede ser anterior a la de inicio");
        if(bloques.isEmpty()) throw new BusinessException("HORARIO_REQUERIDO","Añade al menos un día y una franja horaria");
        var ordenados=bloques.stream().sorted(Comparator.comparing(HorarioCursoRequest::dia).thenComparing(HorarioCursoRequest::horaInicio)).toList();
        for(int i=0;i<ordenados.size();i++) {
            var bloque=ordenados.get(i);
            if(!bloque.horaFin().isAfter(bloque.horaInicio())) throw new BusinessException("HORA_INVALIDA","La hora de fin debe ser posterior a la de inicio en el mismo día");
            if(bloque.horaInicio().getSecond()!=0 || bloque.horaFin().getSecond()!=0 || bloque.horaInicio().getNano()!=0 || bloque.horaFin().getNano()!=0)
                throw new BusinessException("HORA_INVALIDA","Usa horas y minutos, sin segundos");
            if(i>0 && bloque.dia()==ordenados.get(i-1).dia() && bloque.horaInicio().isBefore(ordenados.get(i-1).horaFin()))
                throw new BusinessException("HORARIO_SOLAPADO","Hay franjas del mismo curso que se superponen");
        }
        // Una fila del docente serializa las altas de sus cursos para evitar reservas dobles.
        usuarios.findByIdForUpdate(curso.getDocente().getId()).orElseThrow();
        for(var existente:horarios.findActivosDelDocente(curso.getDocente().getId())) {
            if(existente.getCurso().getId().equals(curso.getId())) continue;
            LocalDate desde=curso.getFechaInicio().isAfter(existente.getCurso().getFechaInicio())?curso.getFechaInicio():existente.getCurso().getFechaInicio();
            LocalDate hasta=minimo(curso.getFechaFin(),existente.getCurso().getFechaFin());
            for(var bloque:ordenados) if(bloque.dia()==existente.getDia() && bloque.horaInicio().isBefore(existente.getHoraFin()) && bloque.horaFin().isAfter(existente.getHoraInicio())
                    && !desde.with(TemporalAdjusters.nextOrSame(bloque.dia())).isAfter(hasta))
                throw new BusinessException("DOCENTE_OCUPADO","El docente ya tiene una clase en ese horario: "+existente.getCurso().getTitulo());
        }
        curso.getHorarios().clear();
        for(var bloque:ordenados) {var h=new HorarioCurso();h.setCurso(curso);h.setDia(bloque.dia());h.setHoraInicio(bloque.horaInicio());h.setHoraFin(bloque.horaFin());curso.getHorarios().add(h);}
        var formato=DateTimeFormatter.ofPattern("HH:mm");
        curso.setHorario(ordenados.stream().map(h->dia(h.dia())+" "+h.horaInicio().format(formato)+"–"+h.horaFin().format(formato)).collect(Collectors.joining(" · ")));
    }
    private LocalDate minimo(LocalDate a,LocalDate b){if(a==null)return b==null?LocalDate.MAX:b;return b==null||a.isBefore(b)?a:b;}
    private String dia(DayOfWeek d){return switch(d){case MONDAY->"Lun";case TUESDAY->"Mar";case WEDNESDAY->"Mié";case THURSDAY->"Jue";case FRIDAY->"Vie";case SATURDAY->"Sáb";case SUNDAY->"Dom";};}
}
