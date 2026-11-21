package com.accessguard.security;

import com.accessguard.auditoria.AuditService;
import com.accessguard.domain.auditoria.EventoAuditoria;
import com.accessguard.domain.sessao.Sessao;
import com.accessguard.domain.sessao.SessaoRepository;
import com.accessguard.domain.usuario.Usuario;
import com.accessguard.domain.usuario.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class BruteForceAuthenticationProvider extends DaoAuthenticationProvider {

    private final UsuarioRepository usuarioRepository;
    private final SessaoRepository sessaoRepository;
    private final AuditService auditService;
    private final HttpServletRequest httpServletRequest;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public BruteForceAuthenticationProvider(UsuarioRepository usuarioRepository,
                                             SessaoRepository sessaoRepository,
                                             AuditService auditService,
                                             HttpServletRequest httpServletRequest,
                                             PasswordEncoder passwordEncoder,
                                             UserDetailsService userDetailsService) {
        this.usuarioRepository = usuarioRepository;
        this.sessaoRepository = sessaoRepository;
        this.auditService = auditService;
        this.httpServletRequest = httpServletRequest;
        this.passwordEncoder = passwordEncoder;
        setUserDetailsService(userDetailsService);
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String email = authentication.getName();
        String rawPassword = authentication.getCredentials().toString();
        String ip = getClientIp();

        // Busca o usuário
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);

        if (usuarioOpt.isEmpty()) {
            // Usuário inexistente: audita mas não revela existência
            auditService.registrar(email, ip, EventoAuditoria.USUARIO_INEXISTENTE,
                    "Tentativa de login para usuário inexistente");
            throw new BadCredentialsException("Credenciais inválidas");
        }

        Usuario usuario = usuarioOpt.get();

        // Verifica se conta está bloqueada ANTES de verificar senha
        if (usuario.estaBloqueado()) {
            throw new LockedException("Conta bloqueada. Contate o administrador.");
        }

        // Verifica senha
        if (!passwordEncoder.matches(rawPassword, usuario.getSenhaHash())) {
            // Falha de credenciais: incrementa contador, verifica bloqueio
            usuario.registrarFalha();
            if (usuario.estaBloqueado()) {
                usuarioRepository.save(usuario);
                auditService.registrar(usuario.getEmail(), ip, EventoAuditoria.CONTA_BLOQUEADA,
                        "Conta bloqueada após 3 tentativas de login falhas");
            } else {
                usuarioRepository.save(usuario);
                auditService.registrar(usuario.getEmail(), ip, EventoAuditoria.LOGIN_FALHA,
                        "Tentativa de login falha (" + usuario.getTentativasFalha() + "/3)");
            }
            throw new BadCredentialsException("Credenciais inválidas");
        }

        // Sucesso: reseta contador, atualiza último login, cria sessão, audita
        usuario.registrarSucesso();
        usuarioRepository.save(usuario);

        // Cria registro de sessão ativa
        String sessionId = httpServletRequest.getSession().getId();
        Sessao sessao = new Sessao(usuario, sessionId, ip, getUserAgent());
        sessaoRepository.save(sessao);

        
        // Retorna Authentication com Authorities
        return new UsernamePasswordAuthenticationToken(
                new AccessGuardUserDetails(usuario),
                null,
                new AccessGuardUserDetails(usuario).getAuthorities()
        );
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private String getClientIp() {
        String xForwardedFor = httpServletRequest.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = httpServletRequest.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return httpServletRequest.getRemoteAddr();
    }

    private String getUserAgent() {
        return httpServletRequest.getHeader("User-Agent");
    }
}