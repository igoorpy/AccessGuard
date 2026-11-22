package com.accessguard.api;

import com.accessguard.api.dto.PerfilUpdateRequest;
import com.accessguard.api.dto.SenhaUpdateRequest;
import com.accessguard.domain.usuario.Role;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PerfilControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private Usuario usuario;
    private AccessGuardUserDetails userDetails;

    @BeforeEach
    void setUp() {
        usuarioRepository.deleteAll();

        usuario = new Usuario("perfil@test.com", passwordEncoder.encode("Senha@123"), "Nome Original", Role.COLABORADOR);
        usuarioRepository.save(usuario);
        userDetails = new AccessGuardUserDetails(usuario);
    }

    @Test
    void obterPerfilRetornaDados() throws Exception {
        mockMvc.perform(get("/api/perfil").with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("perfil@test.com"))
                .andExpect(jsonPath("$.nome").value("Nome Original"))
                .andExpect(jsonPath("$.role").value("COLABORADOR"));
    }

    @Test
    void atualizarNome() throws Exception {
        PerfilUpdateRequest request = new PerfilUpdateRequest("Novo Nome");

        mockMvc.perform(put("/api/perfil")
                        .with(user(userDetails)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Novo Nome"));
    }

    @Test
    void alterarSenhaValida() throws Exception {
        SenhaUpdateRequest request = new SenhaUpdateRequest("Senha@123", "NovaSenha@123", "NovaSenha@123");

        mockMvc.perform(put("/api/perfil/senha")
                        .with(user(userDetails)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Verifica se senha foi alterada no banco
        Usuario updated = usuarioRepository.findByEmail("perfil@test.com").orElseThrow();
        assertTrue(passwordEncoder.matches("NovaSenha@123", updated.getSenhaHash()));
    }

    @Test
    void alterarSenhaSenhaAtualIncorretaRetorna400() throws Exception {
        SenhaUpdateRequest request = new SenhaUpdateRequest("SenhaErrada", "NovaSenha@123", "NovaSenha@123");

        mockMvc.perform(put("/api/perfil/senha")
                        .with(user(userDetails)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void alterarSenhaConfirmacaoDiferenteRetorna400() throws Exception {
        SenhaUpdateRequest request = new SenhaUpdateRequest("Senha@123", "NovaSenha@123", "OutraSenha@123");

        mockMvc.perform(put("/api/perfil/senha")
                        .with(user(userDetails)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void alterarSenhaMuitoCurtaRetorna400() throws Exception {
        SenhaUpdateRequest request = new SenhaUpdateRequest("Senha@123", "Curta", "Curta");

        mockMvc.perform(put("/api/perfil/senha")
                        .with(user(userDetails)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}