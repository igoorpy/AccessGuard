package com.accessguard.security;

import com.accessguard.auditoria.AuditService;
import com.accessguard.domain.auditoria.EventoAuditoria;
import com.accessguard.domain.sessao.Sessao;
import com.accessguard.domain.sessao.SessaoRepository;
import com.accessguard.domain.usuario.Role;
import com.accessguard.domain.usuario.StatusUsuario;
import com.accessguard.domain.usuario.Usuario;
import com.accessguard.domain.usuario.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class BruteForceAuthenticationProviderTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private SessaoRepository sessaoRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private HttpServletRequest httpServletRequest;

    private BruteForceAuthenticationProvider provider;
    private PasswordEncoder passwordEncoder;
    private Usuario usuario;
    private String senhaCorreta = "Senha@123";
    private String senhaHash;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        senhaHash = passwordEncoder.encode(senhaCorreta);

        usuario = new Usuario("teste@accessguard.com", senhaHash, "Usuário Teste", Role.COLABORADOR);
        // Força o ID para testes
        try {
            java.lang.reflect.Field idField = Usuario.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(usuario, 1L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        provider = new BruteForceAuthenticationProvider(usuarioRepository, sessaoRepository, auditService, httpServletRequest, passwordEncoder);

        when(httpServletRequest.getRemoteAddr()).thenReturn("192.168.1.100");
        when(httpServletRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0 Test");
        when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn("");
        when(httpServletRequest.getHeader("X-Real-IP")).thenReturn("");
        when(httpServletRequest.getSession()).thenReturn(new MockHttpSession());
    }

    @Test
    void deveAutenticarComSucessoEResetarContador() {
        // Given
        when(usuarioRepository.findByEmail("teste@accessguard.com")).thenReturn(Optional.of(usuario));
        when(sessaoRepository.save(any(Sessao.class))).thenAnswer(inv -> inv.getArgument(0));

        Authentication auth = new UsernamePasswordAuthenticationToken("teste@accessguard.com", senhaCorreta);

        // When
        Authentication result = provider.authenticate(auth);

        // Then
        assertNotNull(result);
        assertTrue(result.isAuthenticated());
        assertEquals(0, usuario.getTentativasFalha());
        assertEquals(StatusUsuario.ATIVO, usuario.getStatus());
        assertNotNull(usuario.getUltimoLoginEm());

        // Verifica auditoria de sucesso
        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> ipCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<EventoAuditoria> eventoCaptor = ArgumentCaptor.forClass(EventoAuditoria.class);
        verify(auditService).registrar(emailCaptor.capture(), ipCaptor.capture(), eventoCaptor.capture(), anyString());
        assertEquals("teste@accessguard.com", emailCaptor.getValue());
        assertEquals(EventoAuditoria.LOGIN_SUCESSO, eventoCaptor.getValue());
    }

    @Test
    void deveIncrementarTentativasFalhaEBloquearAposTresTentativas() {
        // Given
        when(usuarioRepository.findByEmail("teste@accessguard.com")).thenReturn(Optional.of(usuario));

        Authentication auth = new UsernamePasswordAuthenticationToken("teste@accessguard.com", "SenhaErrada");

        // 1ª falha
        assertThrows(BadCredentialsException.class, () -> provider.authenticate(auth));
        assertEquals(1, usuario.getTentativasFalha());
        assertEquals(StatusUsuario.ATIVO, usuario.getStatus());
        verify(auditService).registrar(eq("teste@accessguard.com"), anyString(), eq(EventoAuditoria.LOGIN_FALHA), anyString());

        // 2ª falha
        assertThrows(BadCredentialsException.class, () -> provider.authenticate(auth));
        assertEquals(2, usuario.getTentativasFalha());
        assertEquals(StatusUsuario.ATIVO, usuario.getStatus());

        // 3ª falha - deve bloquear
        assertThrows(BadCredentialsException.class, () -> provider.authenticate(auth));
        assertEquals(3, usuario.getTentativasFalha());
        assertEquals(StatusUsuario.BLOQUEADO, usuario.getStatus());

        // Verifica auditoria de bloqueio
        ArgumentCaptor<EventoAuditoria> eventoCaptor = ArgumentCaptor.forClass(EventoAuditoria.class);
        verify(auditService, atLeastOnce()).registrar(anyString(), anyString(), eventoCaptor.capture(), anyString());
        assertTrue(eventoCaptor.getAllValues().contains(EventoAuditoria.CONTA_BLOQUEADA));
    }

    @Test
    void deveLancarLockedExceptionQuandoContaJaBloqueada() {
        // Given - conta já bloqueada
        usuario.registrarFalha(); // 1
        usuario.registrarFalha(); // 2
        usuario.registrarFalha(); // 3 - bloqueia
        when(usuarioRepository.findByEmail("teste@accessguard.com")).thenReturn(Optional.of(usuario));

        Authentication auth = new UsernamePasswordAuthenticationToken("teste@accessguard.com", senhaCorreta);

        // When/Then
        LockedException exception = assertThrows(LockedException.class, () -> provider.authenticate(auth));
        assertEquals("Conta bloqueada. Contate o administrador.", exception.getMessage());
    }

    @Test
    void deveAuditarUsuarioInexistenteSemRevelarExistência() {
        // Given
        when(usuarioRepository.findByEmail("inexistente@accessguard.com")).thenReturn(Optional.empty());

        Authentication auth = new UsernamePasswordAuthenticationToken("inexistente@accessguard.com", "qualquer");

        // When
        BadCredentialsException exception = assertThrows(BadCredentialsException.class, () -> provider.authenticate(auth));

        // Then
        assertEquals("Credenciais inválidas", exception.getMessage());
        verify(auditService).registrar(eq("inexistente@accessguard.com"), anyString(), eq(EventoAuditoria.USUARIO_INEXISTENTE), anyString());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void deveSuportarUsernamePasswordAuthenticationToken() {
        assertTrue(provider.supports(UsernamePasswordAuthenticationToken.class));
        assertFalse(provider.supports(Object.class));
    }

    // Mock simples de HttpSession
    static class MockHttpSession implements jakarta.servlet.http.HttpSession {
        private final String id = "test-session-" + System.currentTimeMillis();
        @Override public long getCreationTime() { return System.currentTimeMillis(); }
        @Override public String getId() { return id; }
        @Override public long getLastAccessedTime() { return System.currentTimeMillis(); }
        @Override public jakarta.servlet.ServletContext getServletContext() { return null; }
        @Override public void setMaxInactiveInterval(int interval) {}
        @Override public int getMaxInactiveInterval() { return 1800; }
        @Override public Object getAttribute(String name) { return null; }
        @Override public java.util.Enumeration<String> getAttributeNames() { return java.util.Collections.emptyEnumeration(); }
        @Override public void setAttribute(String name, Object value) {}
        @Override public void removeAttribute(String name) {}
        @Override public void invalidate() {}
        @Override public boolean isNew() { return true; }
    }
}