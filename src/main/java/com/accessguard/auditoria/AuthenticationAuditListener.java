package com.accessguard.auditoria;

import com.accessguard.domain.auditoria.EventoAuditoria;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationAuditListener {

    private final AuditService auditService;

    public AuthenticationAuditListener(AuditService auditService) {
        this.auditService = auditService;
    }

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        String email = event.getAuthentication().getName();
        String ip = extractIp(event.getAuthentication().getDetails());
        String detalhes = "Login realizado com sucesso";

        auditService.registrar(email, ip, EventoAuditoria.LOGIN_SUCESSO, detalhes);
    }

    @EventListener
    public void onAuthenticationFailure(AuthenticationFailureBadCredentialsEvent event) {
        String email = event.getAuthentication().getName();
        String ip = extractIp(event.getAuthentication().getDetails());

        // O BruteForceAuthenticationProvider já audita LOGIN_FALHA e CONTA_BLOQUEADA
        // Aqui apenas registramos falhas genéricas se necessário
        if (!event.getException().getMessage().contains("bloqueada")) {
            String detalhes = "Tentativa de login falha: " + event.getException().getMessage();
            auditService.registrar(email, ip, EventoAuditoria.LOGIN_FALHA, detalhes);
        }
    }

    private String extractIp(Object details) {
        if (details instanceof WebAuthenticationDetails webDetails) {
            return webDetails.getRemoteAddress();
        }
        return "UNKNOWN";
    }
}