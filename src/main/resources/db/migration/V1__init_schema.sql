-- AccessGuard Database Schema
-- Version 1: Initial schema for Usuario, LogAcesso, Sessao tables

-- Enum types as VARCHAR with CHECK constraints
-- Role: ADMIN, AUDITOR, COLABORADOR
-- StatusUsuario: ATIVO, BLOQUEADO
-- EventoAuditoria: LOGIN_SUCESSO, LOGIN_FALHA, USUARIO_INEXISTENTE, CONTA_BLOQUEADA, CONTA_DESBLOQUEADA, SENHA_ALTERADA, PERFIL_ALTERADO, SESSAO_ENCERRADA

CREATE TABLE usuario (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    senha_hash VARCHAR(255) NOT NULL,
    nome VARCHAR(150) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN', 'AUDITOR', 'COLABORADOR')),
    status VARCHAR(20) NOT NULL DEFAULT 'ATIVO' CHECK (status IN ('ATIVO', 'BLOQUEADO')),
    tentativas_falha INTEGER NOT NULL DEFAULT 0,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ultimo_login_em TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_usuario_email ON usuario(email);
CREATE INDEX idx_usuario_status ON usuario(status);

CREATE TABLE log_acesso (
    id BIGSERIAL PRIMARY KEY,
    usuario_email VARCHAR(255),
    ip VARCHAR(45) NOT NULL,
    evento VARCHAR(30) NOT NULL CHECK (evento IN (
        'LOGIN_SUCESSO', 'LOGIN_FALHA', 'USUARIO_INEXISTENTE',
        'CONTA_BLOQUEADA', 'CONTA_DESBLOQUEADA',
        'SENHA_ALTERADA', 'PERFIL_ALTERADO', 'SESSAO_ENCERRADA'
    )),
    detalhes TEXT,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_log_acesso_usuario_email_criado_em ON log_acesso(usuario_email, criado_em DESC);
CREATE INDEX idx_log_acesso_evento_criado_em ON log_acesso(evento, criado_em DESC);
CREATE INDEX idx_log_acesso_criado_em ON log_acesso(criado_em DESC);

CREATE TABLE sessao (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    session_id VARCHAR(128) NOT NULL UNIQUE,
    ip VARCHAR(45) NOT NULL,
    user_agent VARCHAR(500),
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ultimo_acesso_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ativa BOOLEAN NOT NULL DEFAULT TRUE,
    encerrada_em TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_sessao_usuario_ativa ON sessao(usuario_id, ativa);
CREATE INDEX idx_sessao_session_id ON sessao(session_id);
CREATE INDEX idx_sessao_ultimo_acesso ON sessao(ultimo_acesso_em DESC);