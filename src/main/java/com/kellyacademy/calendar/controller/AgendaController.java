package com.kellyacademy.calendar.controller;
import com.kellyacademy.calendar.service.AgendaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
@RestController @RequestMapping("/api/calendario") @RequiredArgsConstructor
public class AgendaController {
    private final AgendaService agenda;
    @GetMapping public List<AgendaService.Item> obtener(@RequestParam(required=false) LocalDate desde,@RequestParam(required=false) LocalDate hasta){return agenda.obtener(desde,hasta);}
}
