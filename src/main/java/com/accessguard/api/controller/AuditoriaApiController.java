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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@RestController
@RequestMapping("/api/auditoria")
@PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR')")
public class AuditoriaApiController {

    private final LogAcessoRepository logAcessoRepository;

    public AuditoriaApiController(LogAcessoRepository logAcessoRepository) {
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

        Specification<LogAcesso> spec = buildSpecification(email, eventoEnum, inicio, fim, ip);
        Page<LogAcesso> logs = logAcessoRepository.findAll(spec, pageable);

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

    private Specification<LogAcesso> buildSpecification(String email, EventoAuditoria evento, Instant inicio, Instant fim, String ip) {
        return (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();

            if (email != null && !email.isBlank()) {
                predicates.add(cb.equal(root.get("usuarioEmail"), email));
            }
            if (evento != null) {
                predicates.add(cb.equal(root.get("evento"), evento));
            }
            if (inicio != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("criadoEm"), inicio));
            }
            if (fim != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("criadoEm"), fim));
            }
            if (ip != null && !ip.isBlank()) {
                predicates.add(cb.equal(root.get("ip"), ip));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
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