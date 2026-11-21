package com.accessguard.auditoria;

import com.accessguard.domain.auditoria.EventoAuditoria;
import com.accessguard.domain.auditoria.LogAcesso;
import com.accessguard.domain.auditoria.LogAcessoRepository;
import com.accessguard.domain.usuario.Role;
import com.accessguard.domain.usuario.Usuario;
import com.accessguard.domain.usuario.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class AuthenticationAuditListenerIT {

    @Autowired
    private LogAcessoRepository logAcessoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        logAcessoRepository.deleteAll();
        usuarioRepository.deleteAll();

        usuario = new Usuario("audit@test.com", "hash", "Audit User", Role.COLABORADOR);
        usuario = usuarioRepository.save(usuario);
    }

    @AfterEach
    void tearDown() {
        logAcessoRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    @Test
    void deveAuditarLoginSucesso() {
        // Given
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(usuario.getEmail(), "senha");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.50");
        auth.setDetails(new WebAuthenticationDetails(request));

        // When
        eventPublisher.publishEvent(new AuthenticationSuccessEvent(auth));

        // Then - aguarda processamento assíncrono
        await().atMost(java.time.Duration.ofSeconds(5))
                .pollInterval(java.time.Duration.ofMillis(100))
                .untilAsserted(() -> {
                    List<LogAcesso> logs = logAcessoRepository.findAll();
                    assertEquals(1, logs.size());
                    LogAcesso log = logs.get(0);
                    assertEquals(usuario.getEmail(), log.getUsuarioEmail());
                    assertEquals("192.168.1.50", log.getIp());
                    assertEquals(EventoAuditoria.LOGIN_SUCESSO, log.getEvento());
                    assertTrue(log.getDetalhes().contains("Login realizado com sucesso"));
                });
    }

    @Test
    void deveAuditarLoginFalha() {
        // Given
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("inexistente@test.com", "senha");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.50");
        auth.setDetails(new WebAuthenticationDetails(request));

        // When
        eventPublisher.publishEvent(new AuthenticationFailureBadCredentialsEvent(
                auth, new org.springframework.security.authentication.BadCredentialsException("Credenciais inválidas")));

        // Then
        await().atMost(java.time.Duration.ofSeconds(5))
                .pollInterval(java.time.Duration.ofMillis(100))
                .untilAsserted(() -> {
                    List<LogAcesso> logs = logAcessoRepository.findAll();
                    assertEquals(1, logs.size());
                    LogAcesso log = logs.get(0);
                    assertEquals("inexistente@test.com", log.getUsuarioEmail());
                    assertEquals("192.168.1.50", log.getIp());
                    assertEquals(EventoAuditoria.LOGIN_FALHA, log.getEvento());
                    assertTrue(log.getDetalhes().contains("Tentativa de login falha"));
                });
    }
}