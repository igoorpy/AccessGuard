package com.accessguard.domain.auditoria;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "log_acesso", indexes = {
        @Index(name = "idx_log_acesso_usuario_email_criado_em", columnList = "usuario_email, criado_em"),
        @Index(name = "idx_log_acesso_evento_criado_em", columnList = "evento, criado_em")
})
@EntityListeners(AuditingEntityListener.class)
public class LogAcesso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_email", length = 255)
    private String usuarioEmail;

    @Column(nullable = false, length = 45)
    private String ip;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EventoAuditoria evento;

    @Column(columnDefinition = "TEXT")
    private String detalhes;

    @CreatedDate
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    protected LogAcesso() {}

    public LogAcesso(String usuarioEmail, String ip, EventoAuditoria evento, String detalhes) {
        this.usuarioEmail = usuarioEmail;
        this.ip = ip;
        this.evento = evento;
        this.detalhes = detalhes;
    }

    public Long getId() {
        return id;
    }

    public String getUsuarioEmail() {
        return usuarioEmail;
    }

    public String getIp() {
        return ip;
    }

    public EventoAuditoria getEvento() {
        return evento;
    }

    public String getDetalhes() {
        return detalhes;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}