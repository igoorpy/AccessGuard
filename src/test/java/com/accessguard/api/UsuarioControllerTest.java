package com.accessguard.api;

import com.accessguard.api.dto.UsuarioCreateRequest;
import com.accessguard.api.dto.UsuarioUpdateRequest;
import com.accessguard.domain.usuario.Role;
import com.accessguard.domain.usuario.StatusUsuario;
import com.accessguard.domain.usuario.Usuario;
import com.accessguard.domain.usuario.UsuarioRepository;
import com.accessguard.security.AccessGuardUserDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private Usuario admin;
    private AccessGuardUserDetails adminDetails;

    @BeforeEach
    void setUp() {
        usuarioRepository.deleteAll();

        admin = new Usuario("admin@test.com", passwordEncoder.encode("Senha@123"), "Admin User", Role.ADMIN);
        usuarioRepository.save(admin);
        adminDetails = new AccessGuardUserDetails(admin);
    }

    @Test
    void criarUsuarioValidoRetorna201() throws Exception {
        UsuarioCreateRequest request = new UsuarioCreateRequest(
            "novo@test.com", "Senha@123", "Novo User", Role.COLABORADOR
        );

        mockMvc.perform(post("/api/admin/usuarios")
                        .with(user(adminDetails)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("novo@test.com"))
                .andExpect(jsonPath("$.nome").value("Novo User"))
                .andExpect(jsonPath("$.role").value("COLABORADOR"))
                .andExpect(jsonPath("$.status").value("ATIVO"));
    }

    @Test
    void criarUsuarioEmailDuplicadoRetorna400() throws Exception {
        UsuarioCreateRequest request = new UsuarioCreateRequest(
            "admin@test.com", "Senha@123", "Outro", Role.COLABORADOR
        );

        mockMvc.perform(post("/api/admin/usuarios")
                        .with(user(adminDetails)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listarUsuariosComPaginacao() throws Exception {
        // Cria alguns usuários
        for (int i = 0; i < 5; i++) {
            Usuario u = new Usuario("user" + i + "@test.com", passwordEncoder.encode("Senha@123"), "User " + i, Role.COLABORADOR);
            usuarioRepository.save(u);
        }

        mockMvc.perform(get("/api/admin/usuarios")
                        .with(user(adminDetails))
                        .param("page", "0")
                        .param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.totalElements").value(6)) // 5 + admin
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void buscarUsuarioPorId() throws Exception {
        Usuario u = new Usuario("buscar@test.com", passwordEncoder.encode("Senha@123"), "Buscar User", Role.AUDITOR);
        u = usuarioRepository.save(u);

        mockMvc.perform(get("/api/admin/usuarios/{id}", u.getId())
                        .with(user(adminDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("buscar@test.com"))
                .andExpect(jsonPath("$.role").value("AUDITOR"));
    }

    @Test
    void atualizarUsuario() throws Exception {
        Usuario u = new Usuario("atualizar@test.com", passwordEncoder.encode("Senha@123"), "Antigo", Role.COLABORADOR);
        u = usuarioRepository.save(u);

        UsuarioUpdateRequest request = new UsuarioUpdateRequest(
            "atualizar@test.com", "NovaSenha@123", "Novo Nome", Role.AUDITOR, StatusUsuario.ATIVO
        );

        mockMvc.perform(put("/api/admin/usuarios/{id}", u.getId())
                        .with(user(adminDetails)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Novo Nome"))
                .andExpect(jsonPath("$.role").value("AUDITOR"));

        // Verifica senha alterada no banco
        Usuario updated = usuarioRepository.findById(u.getId()).orElseThrow();
        assertTrue(passwordEncoder.matches("NovaSenha@123", updated.getSenhaHash()));
    }

    @Test
    void desbloquearUsuario() throws Exception {
        Usuario u = new Usuario("bloqueado@test.com", passwordEncoder.encode("Senha@123"), "Bloqueado", Role.COLABORADOR);
        u.registrarFalha(); u.registrarFalha(); u.registrarFalha();
        u = usuarioRepository.save(u);

        assertEquals(StatusUsuario.BLOQUEADO, u.getStatus());

        mockMvc.perform(post("/api/admin/usuarios/{id}/desbloquear", u.getId())
                        .with(user(adminDetails)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ATIVO"))
                .andExpect(jsonPath("$.tentativasFalha").value(0));
    }

    @Test
    void excluirUsuario() throws Exception {
        Usuario u = new Usuario("excluir@test.com", passwordEncoder.encode("Senha@123"), "Excluir", Role.COLABORADOR);
        u = usuarioRepository.save(u);

        mockMvc.perform(delete("/api/admin/usuarios/{id}", u.getId())
                        .with(user(adminDetails)).with(csrf()))
                .andExpect(status().isNoContent());

        assertTrue(usuarioRepository.findById(u.getId()).isEmpty());
    }

    @Test
    void excluirProprioUsuarioRetorna400() throws Exception {
        mockMvc.perform(delete("/api/admin/usuarios/{id}", admin.getId())
                        .with(user(adminDetails)).with(csrf()))
                .andExpect(status().isBadRequest());
    }
}