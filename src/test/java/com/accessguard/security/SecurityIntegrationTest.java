package com.accessguard.security;

import com.accessguard.domain.usuario.Role;
import com.accessguard.domain.usuario.Usuario;
import com.accessguard.domain.usuario.UsuarioRepository;
import com.accessguard.security.AccessGuardUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Usuario admin;
    private Usuario auditor;
    private Usuario colaborador;
    private AccessGuardUserDetails adminDetails;
    private AccessGuardUserDetails auditorDetails;
    private AccessGuardUserDetails colaboradorDetails;

    @BeforeEach
    void setUp() {
        usuarioRepository.deleteAll();

        admin = new Usuario("admin@test.com", passwordEncoder.encode("Senha@123"), "Admin User", Role.ADMIN);
        auditor = new Usuario("auditor@test.com", passwordEncoder.encode("Senha@123"), "Auditor User", Role.AUDITOR);
        colaborador = new Usuario("colab@test.com", passwordEncoder.encode("Senha@123"), "Colaborador User", Role.COLABORADOR);

        usuarioRepository.saveAll(java.util.List.of(admin, auditor, colaborador));

        adminDetails = new AccessGuardUserDetails(admin);
        auditorDetails = new AccessGuardUserDetails(auditor);
        colaboradorDetails = new AccessGuardUserDetails(colaborador);
    }

    // ==================== RBAC: Web (Thymeleaf) ====================

    @Test
    void dashboardRequerAutenticacao() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login*"));
    }

    @Test
    void colaboradorAcessaDashboard() throws Exception {
        mockMvc.perform(get("/dashboard").with(user(colaboradorDetails)))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"));
    }

    @Test
    void adminAcessaAdminUsuarios() throws Exception {
        mockMvc.perform(get("/admin/usuarios").with(user(adminDetails)))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/usuarios"));
    }

    @Test
    void auditorAcessaAdminUsuariosRetorna403() throws Exception {
        mockMvc.perform(get("/admin/usuarios").with(user(auditorDetails)))
                .andExpect(status().isForbidden());
    }

    @Test
    void colaboradorAcessaAdminUsuariosRetorna403() throws Exception {
        mockMvc.perform(get("/admin/usuarios").with(user(colaboradorDetails)))
                .andExpect(status().isForbidden());
    }

    @Test
    void auditorAcessaAuditoria() throws Exception {
        mockMvc.perform(get("/auditoria").with(user(auditorDetails)))
                .andExpect(status().isOk())
                .andExpect(view().name("auditoria/logs"));
    }

    @Test
    void adminAcessaAuditoria() throws Exception {
        mockMvc.perform(get("/auditoria").with(user(adminDetails)))
                .andExpect(status().isOk());
    }

    @Test
    void colaboradorAcessaAuditoriaRetorna403() throws Exception {
        mockMvc.perform(get("/auditoria").with(user(colaboradorDetails)))
                .andExpect(status().isForbidden());
    }

    @Test
    void colaboradorAcessaPerfil() throws Exception {
        mockMvc.perform(get("/perfil").with(user(colaboradorDetails)))
                .andExpect(status().isOk())
                .andExpect(view().name("perfil"));
    }

    // ==================== RBAC: API REST ====================

    @Test
    void apiAdminUsuariosRequerAutenticacao() throws Exception {
        mockMvc.perform(get("/api/admin/usuarios"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void apiAdminUsuariosColaboradorRetorna403() throws Exception {
        mockMvc.perform(get("/api/admin/usuarios").with(user(colaboradorDetails)))
                .andExpect(status().isForbidden());
    }

    @Test
    void apiAdminUsuariosAuditorRetorna403() throws Exception {
        mockMvc.perform(get("/api/admin/usuarios").with(user(auditorDetails)))
                .andExpect(status().isForbidden());
    }

    @Test
    void apiAdminUsuariosAdminRetorna200() throws Exception {
        mockMvc.perform(get("/api/admin/usuarios").with(user(adminDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void apiAuditoriaRequerAutenticacao() throws Exception {
        mockMvc.perform(get("/api/auditoria"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void apiAuditoriaAuditorRetorna200() throws Exception {
        mockMvc.perform(get("/api/auditoria").with(user(auditorDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void apiAuditoriaAdminRetorna200() throws Exception {
        mockMvc.perform(get("/api/auditoria").with(user(adminDetails)))
                .andExpect(status().isOk());
    }

    @Test
    void apiAuditoriaColaboradorRetorna403() throws Exception {
        mockMvc.perform(get("/api/auditoria").with(user(colaboradorDetails)))
                .andExpect(status().isForbidden());
    }

    @Test
    void apiPerfilRequerAutenticacao() throws Exception {
        mockMvc.perform(get("/api/perfil"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void apiPerfilAutenticadoRetorna200() throws Exception {
        mockMvc.perform(get("/api/perfil").with(user(colaboradorDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("colab@test.com"));
    }
}