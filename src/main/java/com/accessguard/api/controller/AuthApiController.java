package com.accessguard.api.controller;

import com.accessguard.api.dto.LoginRequest;
import com.accessguard.api.dto.LoginResponse;
import com.accessguard.domain.usuario.Usuario;
import com.accessguard.domain.usuario.UsuarioRepository;
import com.accessguard.security.AccessGuardUserDetails;
import com.accessguard.security.LoginSuccessHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;

    public AuthApiController(AuthenticationManager authenticationManager, UsuarioRepository usuarioRepository) {
        this.authenticationManager = authenticationManager;
        this.usuarioRepository = usuarioRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        AccessGuardUserDetails userDetails = (AccessGuardUserDetails) authentication.getPrincipal();
        Usuario usuario = usuarioRepository.findByEmail(userDetails.getUsername()).orElseThrow();

        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
            usuario.getId(),
            usuario.getEmail(),
            usuario.getNome(),
            usuario.getRole().name()
        );

        return ResponseEntity.ok(new LoginResponse(
            "session-cookie",
            "Bearer",
            1800L,
            userInfo
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            new SecurityContextLogoutHandler().logout(request, response, auth);
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/validate")
    public ResponseEntity<LoginResponse.UserInfo> validate(Authentication authentication) {
        AccessGuardUserDetails userDetails = (AccessGuardUserDetails) authentication.getPrincipal();
        Usuario usuario = usuarioRepository.findByEmail(userDetails.getUsername()).orElseThrow();

        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
            usuario.getId(),
            usuario.getEmail(),
            usuario.getNome(),
            usuario.getRole().name()
        );

        return ResponseEntity.ok(userInfo);
    }
}