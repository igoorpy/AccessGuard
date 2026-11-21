package com.accessguard.domain.usuario;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "usuario", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
@EntityListeners(AuditingEntityListener.class)
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(name = "senha_hash", nullable = false, length = 255)
    private String senhaHash;

    @Column(nullable = false, length = 150)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusUsuario status = StatusUsuario.ATIVO;

    @Column(name = "tentativas_falha", nullable = false)
    private int tentativasFalha = 0;

    @CreatedDate
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @LastModifiedDate
    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    @Column(name = "ultimo_login_em")
    private Instant ultimoLoginEm;

    @Version
    private Long version;

    protected Usuario() {}

    public Usuario(String email, String senhaHash, String nome, Role role) {
        this.email = email;
        this.senhaHash = senhaHash;
        this.nome = nome;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public void setSenhaHash(String senhaHash) {
        this.senhaHash = senhaHash;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public StatusUsuario getStatus() {
        return status;
    }

    public int getTentativasFalha() {
        return tentativasFalha;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }

    public Instant getUltimoLoginEm() {
        return ultimoLoginEm;
    }

    public void registrarFalha() {
        this.tentativasFalha++;
        if (this.tentativasFalha >= 3) {
            this.status = StatusUsuario.BLOQUEADO;
        }
    }

    public void registrarSucesso() {
        this.tentativasFalha = 0;
        this.status = StatusUsuario.ATIVO;
        this.ultimoLoginEm = Instant.now();
    }

    public boolean estaBloqueado() {
        return this.status == StatusUsuario.BLOQUEADO;
    }

    public void desbloquear() {
        this.status = StatusUsuario.ATIVO;
        this.tentativasFalha = 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Usuario)) return false;
        Usuario usuario = (Usuario) o;
        return id != null && id.equals(usuario.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}