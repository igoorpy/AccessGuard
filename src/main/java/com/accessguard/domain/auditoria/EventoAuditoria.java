package com.accessguard.domain.auditoria;

public enum EventoAuditoria {
    LOGIN_SUCESSO,
    LOGIN_FALHA,
    USUARIO_INEXISTENTE,
    CONTA_BLOQUEADA,
    CONTA_DESBLOQUEADA,
    SENHA_ALTERADA,
    PERFIL_ALTERADO,
    SESSAO_ENCERRADA
}