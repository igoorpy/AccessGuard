package com.accessguard.api.dto;

import com.accessguard.domain.auditoria.EventoAuditoria;

public record LogAcessoDTO(
    Long id,
    String usuarioEmail,
    String ip,
    EventoAuditoria evento,
    String detalhes,
    String criadoEm
) {}