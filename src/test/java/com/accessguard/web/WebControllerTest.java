package com.accessguard.web;

import com.accessguard.domain.auditoria.EventoAuditoria;
import com.accessguard.domain.auditoria.LogAcesso;
import com.accessguard.domain.auditoria.LogAcessoRepository;
import com.accessguard.domain.usuario.Role;
import com.accessguard.domain.usuario.StatusUsuario;
import com.accessguard.domain.usuario.Usuario;
import com.accessguard.domain.usuario.UsuarioRepository;
import com.accessguard.security.AccessGuardUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private LogAcessoRepository logAcessoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Usuario admin;
    private Usuario auditor;
    private Usuario colaborador;
    private AccessGuardUserDetails adminUserDetails;
    private AccessGuardUserDetails auditorUserDetails;
    private AccessGuardUserDetails colaboradorUserDetails;

    @BeforeEach
    void setUp() {
        logAcessoRepository.deleteAll();
        usuarioRepository.deleteAll();

        admin = new Usuario("admin@test.com", passwordEncoder.encode("Senha@123"), "Admin", Role.ADMIN);
        auditor = new Usuario("auditor@test.com", passwordEncoder.encode("Senha@123"), "Auditor", Role.AUDITOR);
        colaborador = new Usuario("colab@test.com", passwordEncoder.encode("Senha@123"), "Colaborador", Role.COLABORADOR);
        usuarioRepository.saveAll(java.util.List.of(admin, auditor, colaborador));

        adminUserDetails = new AccessGuardUserDetails(admin);
        auditorUserDetails = new AccessGuardUserDetails(auditor);
        colaboradorUserDetails = new AccessGuardUserDetails(colaborador);

        logAcessoRepository.save(new LogAcesso("admin@test.com", "192.168.1.10", EventoAuditoria.LOGIN_SUCESSO, "Login realizado"));
        logAcessoRepository.save(new LogAcesso("auditor@test.com", "192.168.1.20", EventoAuditoria.LOGIN_FALHA, "Senha incorreta"));
    }

    @Test
    void dashboardRetorna200ERenderizaHtml() throws Exception {
        mockMvc.perform(get("/dashboard").with(SecurityMockMvcRequestPostProcessors.user(adminUserDetails)))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("usuario"));
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    void adminUsuariosRetorna200ERenderizaHtml() throws Exception {
        mockMvc.perform(get("/admin/usuarios"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/usuarios"))
                .andExpect(model().attributeExists("usuarios"))
                .andExpect(model().attributeExists("roles"))
                .andExpect(model().attributeExists("statuses"));
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    void adminUsuarioNovoRetorna200ERenderizaHtml() throws Exception {
        mockMvc.perform(get("/admin/usuarios/novo"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/usuario-form"))
                .andExpect(model().attributeExists("usuario"))
                .andExpect(model().attributeExists("roles"))
                .andExpect(model().attributeExists("statuses"))
                .andExpect(model().attributeExists("isEdit"));
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    void auditoriaRetorna200ERenderizaHtml() throws Exception {
        mockMvc.perform(get("/auditoria"))
                .andExpect(status().isOk())
                .andExpect(view().name("auditoria/logs"))
                .andExpect(model().attributeExists("logs"))
                .andExpect(model().attributeExists("eventos"));
    }

    @Test
    void perfilRetorna200ERenderizaHtml() throws Exception {
        mockMvc.perform(get("/perfil").with(SecurityMockMvcRequestPostProcessors.user(adminUserDetails)))
                .andExpect(status().isOk())
                .andExpect(view().name("perfil"))
                .andExpect(model().attributeExists("usuario"));
    }

    @Test
    @WithMockUser(username = "auditor@test.com", roles = "AUDITOR")
    void auditoriaComAuditorRetorna200() throws Exception {
        mockMvc.perform(get("/auditoria"))
                .andExpect(status().isOk())
                .andExpect(view().name("auditoria/logs"));
    }

    @Test
    void dashboardComColaboradorRetorna200() throws Exception {
        mockMvc.perform(get("/dashboard").with(SecurityMockMvcRequestPostProcessors.user(colaboradorUserDetails)))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"));
    }

    @Test
    void perfilComColaboradorRetorna200() throws Exception {
        mockMvc.perform(get("/perfil").with(SecurityMockMvcRequestPostProcessors.user(colaboradorUserDetails)))
                .andExpect(status().isOk())
                .andExpect(view().name("perfil"));
    }

    @Test
    void adminUsuarioCriarComCsrfValidoRedirecionaParaLista() throws Exception {
        mockMvc.perform(post("/admin/usuarios/novo")
                        .param("email", "novo@test.com")
                        .param("nome", "Novo Usuario")
                        .param("role", "COLABORADOR")
                        .param("senha", "Senha@123")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .with(SecurityMockMvcRequestPostProcessors.user(adminUserDetails)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/usuarios"));

        assertTrue(usuarioRepository.findByEmail("novo@test.com").isPresent());
    }

    @Test
    void logoutComCsrfValidoRedirecionaParaLogin() throws Exception {
        mockMvc.perform(post("/logout")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .with(SecurityMockMvcRequestPostProcessors.user(adminUserDetails)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"));
    }
}