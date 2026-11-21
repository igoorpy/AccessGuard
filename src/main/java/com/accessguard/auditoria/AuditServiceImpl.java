package com.accessguard.auditoria;

import com.accessguard.domain.auditoria.EventoAuditoria;
import com.accessguard.domain.auditoria.LogAcesso;
import com.accessguard.domain.auditoria.LogAcessoRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditServiceImpl implements AuditService {

    private final LogAcessoRepository logAcessoRepository;

    public AuditServiceImpl(LogAcessoRepository logAcessoRepository) {
        this.logAcessoRepository = logAcessoRepository;
    }

    @Override
    @Async("auditExecutor")
    @Transactional
    public void registrar(String usuarioEmail, String ip, EventoAuditoria evento, String detalhes) {
        LogAcesso log = new LogAcesso(usuarioEmail, ip, evento, detalhes);
        logAcessoRepository.save(log);
    }

    @Override
    @Async("auditExecutor")
    @Transactional
    public void registrar(LogAcesso log) {
        logAcessoRepository.save(log);
    }
}