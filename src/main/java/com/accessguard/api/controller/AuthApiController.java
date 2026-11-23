package com.accessguard.api.controller;

import com.accessguard.api.dto.LoginRequest;
import com.accessguard.api.dto.LoginResponse;
import com.accessguard.domain.usuario.Usuario;
import com.accessguard.domain.usuario.UsuarioRepository;
import com.accessguard.security.AccessGuardUserDetails;
import com.accessguard.security.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final JwtUtil jwtUtil;

    public AuthApiController(AuthenticationManager authenticationManager,
                             UsuarioRepository usuarioRepository,
                             JwtUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.usuarioRepository = usuarioRepository;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        AccessGuardUserDetails userDetails = (AccessGuardUserDetails) authentication.getPrincipal();
        Usuario usuario = usuarioRepository.findByEmail(userDetails.getUsername()).orElseThrow();

        String token = jwtUtil.generateToken(userDetails);

        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
            usuario.getId(),
            usuario.getEmail(),
            usuario.getNome(),
            usuario.getRole().name()
        );

        return ResponseEntity.ok(new LoginResponse(
            token,
            "Bearer",
            1800L,
            userInfo
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        SecurityContextHolder.clearContext();
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