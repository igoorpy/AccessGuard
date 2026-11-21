# AccessGuard

Sistema corporativo de gestão de identidades e controle de acesso (IAM) com RBAC, proteção contra força bruta e auditoria imutável.

## Stack Tecnológica

- **Java 21** (LTS)
- **Spring Boot 3.3.x**
- **Spring Security 6** (session-based, form login)
- **Spring Data JPA** + **Hibernate**
- **Thymeleaf** (server-side rendering)
- **PostgreSQL 15** (via Docker Compose)
- **springdoc-openapi** (Swagger UI)
- **Flyway** (migrations)
- **BCrypt** (password hashing)

## Perfis de Acesso (RBAC)

| Role | Descrição |
|------|-----------|
| `ADMIN` | Acesso total: gerencia usuários, roles, desbloqueio, visualiza todos os logs |
| `AUDITOR` | Leitura: painel de auditoria e logs de segurança (sem escrita) |
| `COLABORADOR` | Básico: próprio perfil, histórico de acessos, troca de senha, encerrar sessões |

## Proteção contra Força Bruta

- 3 tentativas consecutivas de senha incorreta → conta **BLOQUEADA**
- Desbloqueio **apenas manual por ADMIN**
- Escopo: por conta (usuário), não por IP
- Usuários inexistentes: log `USUARIO_INEXISTENTE` (sem bloqueio)

## Auditoria

Eventos gravados (append-only, retenção indefinida):
- `LOGIN_SUCESSO`, `LOGIN_FALHA`, `USUARIO_INEXISTENTE`
- `CONTA_BLOQUEADA`, `CONTA_DESBLOQUEADA`
- `SENHA_ALTERADA`, `PERFIL_ALTERADO`, `SESSAO_ENCERRADA`

Painel com filtros (email, evento, datas), paginação (20/pág), exportação CSV.

## Início Rápido

### Pré-requisitos
- Docker + Docker Compose
- Java 21 + Maven 3.9+ (ou use `./mvnw`)

### Subir o banco
```bash
docker compose up -d
```

### Rodar a aplicação (perfil local)
```bash
./mvnw spring-boot:run
```
Acesse: http://localhost:8080/login

### Rodar com perfil Docker (app + banco no mesmo compose)
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=docker
```

### Testes
```bash
# Unitários + integração
./mvnw test

# Build completo (inclui Testcontainers)
./mvnw verify
```

### Swagger UI
http://localhost:8080/swagger-ui.html

## Estrutura do Projeto

```
src/main/java/com/accessguard/
├── AccessGuardApplication.java
├── config/              # OpenAPI, Async, Actuator
├── domain/
│   ├── usuario/         # Usuario, Role, UsuarioRepository
│   ├── auditoria/       # LogAcesso, EventoAuditoria, LogAcessoRepository
│   └── sessao/          # Sessao, SessaoRepository
├── security/            # UserDetails, AuthenticationProvider, SecurityConfig
├── auditoria/           # AuditService, AuthenticationAuditListener
└── web/
    ├── LoginController.java
    ├── DashboardController.java
    ├── admin/UsuarioAdminController.java
    ├── auditoria/AuditoriaController.java
    └── perfil/PerfilController.java
```

## Variáveis de Ambiente (application-docker.yml)

| Variável | Padrão | Descrição |
|----------|--------|-----------|
| `POSTGRES_HOST` | `localhost` | Host do PostgreSQL |
| `POSTGRES_PORT` | `5432` | Porta do PostgreSQL |
| `POSTGRES_DB` | `accessguard` | Nome do banco |
| `POSTGRES_USER` | `accessguard` | Usuário |
| `POSTGRES_PASSWORD` | `accessguard` | Senha |

## Licença

Uso interno corporativo.