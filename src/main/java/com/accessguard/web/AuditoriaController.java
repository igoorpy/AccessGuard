package com.accessguard.web;

import com.accessguard.domain.auditoria.EventoAuditoria;
import com.accessguard.domain.auditoria.LogAcesso;
import com.accessguard.domain.auditoria.LogAcessoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Controller
@RequestMapping("/auditoria")
@PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR')")
public class AuditoriaController {

    private final LogAcessoRepository logAcessoRepository;

    public AuditoriaController(LogAcessoRepository logAcessoRepository) {
        this.logAcessoRepository = logAcessoRepository;
    }

    @GetMapping
    public String listarLogs(@RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "20") int size,
                             @RequestParam(required = false) String email,
                             @RequestParam(required = false) String evento,
                             @RequestParam(required = false) String dataInicio,
                             @RequestParam(required = false) String dataFim,
                             @RequestParam(required = false) String ip,
                             Model model) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("criadoEm").descending());

        Instant inicio = parseDate(dataInicio, true) != null ? parseDate(dataInicio, true).atZone(ZoneId.systemDefault()).toInstant() : null;
        Instant fim = parseDate(dataFim, false) != null ? parseDate(dataFim, false).atZone(ZoneId.systemDefault()).toInstant() : null;

        EventoAuditoria eventoEnum = parseEvento(evento);

        Specification<LogAcesso> spec = buildSpecification(email, eventoEnum, inicio, fim, ip);
        Page<LogAcesso> logs = logAcessoRepository.findAll(spec, pageable);

        model.addAttribute("logs", logs);
        model.addAttribute("email", email);
        model.addAttribute("evento", evento);
        model.addAttribute("dataInicio", dataInicio);
        model.addAttribute("dataFim", dataFim);
        model.addAttribute("ip", ip);
        model.addAttribute("eventos", EventoAuditoria.values());
        return "auditoria/logs";
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

    private LocalDateTime parseDate(String dateStr, boolean isStart) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        try {
            LocalDate date = LocalDate.parse(dateStr);
            if (isStart) {
                return date.atStartOfDay();
            } else {
                return date.atTime(23, 59, 59);
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
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}