package com.accessguard.api;

import com.accessguard.domain.auditoria.EventoAuditoria;
import com.accessguard.domain.auditoria.LogAcesso;
import com.accessguard.domain.auditoria.LogAcessoRepository;
import com.accessguard.domain.usuario.Role;
import com.accessguard.domain.usuario.Usuario;
import com.accessguard.domain.usuario.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditoriaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LogAcessoRepository logAcessoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Usuario admin;
    private Usuario auditor;

    @BeforeEach
    void setUp() {
        logAcessoRepository.deleteAll();
        usuarioRepository.deleteAll();

        admin = new Usuario("admin@test.com", passwordEncoder.encode("Senha@123"), "Admin", Role.ADMIN);
        auditor = new Usuario("auditor@test.com", passwordEncoder.encode("Senha@123"), "Auditor", Role.AUDITOR);
        usuarioRepository.saveAll(java.util.List.of(admin, auditor));

        // Cria logs de teste (criadoEm é auto-preenchido pelo @CreatedDate)
        logAcessoRepository.save(new LogAcesso("admin@test.com", "192.168.1.10", EventoAuditoria.LOGIN_SUCESSO, "Login realizado"));
        logAcessoRepository.save(new LogAcesso("auditor@test.com", "192.168.1.20", EventoAuditoria.LOGIN_FALHA, "Senha incorreta"));
        logAcessoRepository.save(new LogAcesso("admin@test.com", "192.168.1.10", EventoAuditoria.SENHA_ALTERADA, "Senha alterada"));
        logAcessoRepository.save(new LogAcesso("inexistente@test.com", "192.168.1.30", EventoAuditoria.USUARIO_INEXISTENTE, "Usuário não encontrado"));
    }

    @Test
    @WithMockUser(username = "auditor@test.com", roles = "AUDITOR")
    void listarLogsComPaginacao() throws Exception {
        mockMvc.perform(get("/api/auditoria")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    void filtrarPorEmail() throws Exception {
        mockMvc.perform(get("/api/auditoria")
                        .param("email", "admin@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].usuarioEmail").value("admin@test.com"))
                .andExpect(jsonPath("$.content[1].usuarioEmail").value("admin@test.com"));
    }

    @Test
    @WithMockUser(username = "auditor@test.com", roles = "AUDITOR")
    void filtrarPorEvento() throws Exception {
        mockMvc.perform(get("/api/auditoria")
                        .param("evento", "LOGIN_SUCESSO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].evento").value("LOGIN_SUCESSO"));
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    void filtrarPorIp() throws Exception {
        mockMvc.perform(get("/api/auditoria")
                        .param("ip", "192.168.1.20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].ip").value("192.168.1.20"));
    }

    @Test
    @WithMockUser(username = "auditor@test.com", roles = "AUDITOR")
    void filtrarPorDataInicio() throws Exception {
        String ontem = java.time.LocalDate.now().minusDays(1).toString();
        mockMvc.perform(get("/api/auditoria")
                        .param("dataInicio", ontem))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(4));
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    void filtrosCombinados() throws Exception {
        mockMvc.perform(get("/api/auditoria")
                        .param("email", "admin@test.com")
                        .param("evento", "LOGIN_SUCESSO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].usuarioEmail").value("admin@test.com"))
                .andExpect(jsonPath("$.content[0].evento").value("LOGIN_SUCESSO"));
    }
}