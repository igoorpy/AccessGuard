package com.accessguard.api.controller;

import com.accessguard.api.dto.LogAcessoDTO;
import com.accessguard.api.dto.PageResponse;
import com.accessguard.domain.auditoria.EventoAuditoria;
import com.accessguard.domain.auditoria.LogAcesso;
import com.accessguard.domain.auditoria.LogAcessoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

@RestController
@RequestMapping("/api/auditoria")
@PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR')")
public class AuditoriaController {

    private final LogAcessoRepository logAcessoRepository;

    public AuditoriaController(LogAcessoRepository logAcessoRepository) {
        this.logAcessoRepository = logAcessoRepository;
    }

    @GetMapping
    public ResponseEntity<PageResponse<LogAcessoDTO>> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String evento,
            @RequestParam(required = false) String dataInicio,
            @RequestParam(required = false) String dataFim,
            @RequestParam(required = false) String ip) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("criadoEm").descending());

        Instant inicio = parseDate(dataInicio, true);
        Instant fim = parseDate(dataFim, false);

        EventoAuditoria eventoEnum = parseEvento(evento);

        Page<LogAcesso> logs = logAcessoRepository.findByFilters(email, eventoEnum, inicio, fim, ip, pageable);

        return ResponseEntity.ok(PageResponse.of(logs.map(this::toDTO)));
    }

    private LogAcessoDTO toDTO(LogAcesso log) {
        return new LogAcessoDTO(
            log.getId(),
            log.getUsuarioEmail(),
            log.getIp(),
            log.getEvento(),
            log.getDetalhes(),
            log.getCriadoEm() != null ? log.getCriadoEm().toString() : null
        );
    }

    private Instant parseDate(String dateStr, boolean isStart) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        try {
            LocalDate date = LocalDate.parse(dateStr);
            if (isStart) {
                return date.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant();
            } else {
                return date.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant();
            }
        } catch (Exception e) {
            return null;
        }
    }

    private EventoAuditoria parseEvento(String evento) {
        if (evento == null || evento.isBlank()) {
            return null;
        }
        try {
            return EventoAuditoria.valueOf(evento);
        } catch (Exception e) {
            return null;
        }
    }
}