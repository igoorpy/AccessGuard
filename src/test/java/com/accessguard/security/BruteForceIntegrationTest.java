package com.accessguard.security;

import com.accessguard.domain.usuario.Role;
import com.accessguard.domain.usuario.StatusUsuario;
import com.accessguard.domain.usuario.Usuario;
import com.accessguard.domain.usuario.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BruteForceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuarioRepository.deleteAll();

        usuario = new Usuario("brute@test.com", passwordEncoder.encode("Senha@123"), "Brute User", Role.COLABORADOR);
        usuarioRepository.save(usuario);
    }

    // ==================== API Login: Força Bruta ====================

    @Test
    void apiLoginTresFalhasBloqueiaConta() throws Exception {
        String loginJson = """
            {"email": "brute@test.com", "password": "%s"}
            """;

        // 1ª falha
        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(loginJson.formatted("SenhaErrada1")))
                .andExpect(status().is4xxClientError());

        // 2ª falha
        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(loginJson.formatted("SenhaErrada2")))
                .andExpect(status().is4xxClientError());

        // 3ª falha - deve bloquear
        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(loginJson.formatted("SenhaErrada3")))
                .andExpect(status().is4xxClientError());

        // Verifica se conta está bloqueada no banco
        Usuario updated = usuarioRepository.findByEmail("brute@test.com").orElseThrow();
        assertEquals(StatusUsuario.BLOQUEADO, updated.getStatus());
        assertEquals(3, updated.getTentativasFalha());
    }

    @Test
    void apiLoginContaBloqueadaRetorna401() throws Exception {
        // Bloqueia a conta
        usuario.registrarFalha();
        usuario.registrarFalha();
        usuario.registrarFalha();
        usuarioRepository.save(usuario);

        String loginJson = """
            {"email": "brute@test.com", "password": "Senha@123"}
            """;

        // Tenta logar com senha correta - deve falhar por conta bloqueada
        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(loginJson))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void apiLoginSucessoRetornaUserInfo() throws Exception {
        String loginJson = """
            {"email": "brute@test.com", "password": "Senha@123"}
            """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("brute@test.com"))
                .andExpect(jsonPath("$.user.role").value("COLABORADOR"));
    }
}