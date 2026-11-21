package com.accessguard.auditoria;

import com.accessguard.domain.auditoria.EventoAuditoria;
import com.accessguard.domain.auditoria.LogAcesso;

public interface AuditService {

    void registrar(String usuarioEmail, String ip, EventoAuditoria evento, String detalhes);

    void registrar(LogAcesso log);
}