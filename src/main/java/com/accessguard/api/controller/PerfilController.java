package com.accessguard.api.controller;

import com.accessguard.api.dto.LoginResponse;
import com.accessguard.api.dto.PerfilUpdateRequest;
import com.accessguard.api.dto.SenhaUpdateRequest;
import com.accessguard.api.dto.UsuarioDTO;
import com.accessguard.domain.usuario.Usuario;
import com.accessguard.domain.usuario.UsuarioRepository;
import com.accessguard.security.AccessGuardUserDetails;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/perfil")
public class PerfilController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public PerfilController(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public ResponseEntity<UsuarioDTO> perfil(@AuthenticationPrincipal AccessGuardUserDetails userDetails) {
        Usuario usuario = usuarioRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        return ResponseEntity.ok(toDTO(usuario));
    }

    @PutMapping
    public ResponseEntity<UsuarioDTO> atualizarNome(@AuthenticationPrincipal AccessGuardUserDetails userDetails,
                                                     @Valid @RequestBody PerfilUpdateRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        usuario.setNome(request.nome());
        usuario = usuarioRepository.save(usuario);
        return ResponseEntity.ok(toDTO(usuario));
    }

    @PutMapping("/senha")
    public ResponseEntity<Void> alterarSenha(@AuthenticationPrincipal AccessGuardUserDetails userDetails,
                                              @Valid @RequestBody SenhaUpdateRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(userDetails.getUsername()).orElseThrow();

        if (!passwordEncoder.matches(request.senhaAtual(), usuario.getSenhaHash())) {
            return ResponseEntity.badRequest().build();
        }

        if (!request.novaSenha().equals(request.confirmarSenha())) {
            return ResponseEntity.badRequest().build();
        }

        usuario.setSenhaHash(passwordEncoder.encode(request.novaSenha()));
        usuarioRepository.save(usuario);
        return ResponseEntity.ok().build();
    }

    private UsuarioDTO toDTO(Usuario u) {
        return new UsuarioDTO(
            u.getId(),
            u.getEmail(),
            u.getNome(),
            u.getRole(),
            u.getStatus(),
            u.getTentativasFalha(),
            u.getUltimoLoginEm() != null ? u.getUltimoLoginEm().toString() : null
        );
    }
}