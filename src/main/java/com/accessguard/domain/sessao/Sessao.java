package com.accessguard.domain.sessao;

import com.accessguard.domain.usuario.Usuario;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "sessao", uniqueConstraints = @UniqueConstraint(columnNames = "session_id"))
@EntityListeners(AuditingEntityListener.class)
public class Sessao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "session_id", nullable = false, length = 128)
    private String sessionId;

    @Column(nullable = false, length = 45)
    private String ip;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @CreatedDate
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @LastModifiedDate
    @Column(name = "ultimo_acesso_em", nullable = false)
    private Instant ultimoAcessoEm;

    @Column(nullable = false)
    private boolean ativa = true;

    @Column(name = "encerrada_em")
    private Instant encerradaEm;

    protected Sessao() {}

    public Sessao(Usuario usuario, String sessionId, String ip, String userAgent) {
        this.usuario = usuario;
        this.sessionId = sessionId;
        this.ip = ip;
        this.userAgent = userAgent;
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getIp() {
        return ip;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getUltimoAcessoEm() {
        return ultimoAcessoEm;
    }

    public boolean isAtiva() {
        return ativa;
    }

    public Instant getEncerradaEm() {
        return encerradaEm;
    }

    public void encerrar() {
        this.ativa = false;
        this.encerradaEm = Instant.now();
    }

    public void atualizarUltimoAcesso() {
        this.ultimoAcessoEm = Instant.now();
    }
}