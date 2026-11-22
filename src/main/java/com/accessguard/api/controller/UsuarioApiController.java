package com.accessguard.api.controller;

import com.accessguard.api.dto.PageResponse;
import com.accessguard.api.dto.UsuarioCreateRequest;
import com.accessguard.api.dto.UsuarioDTO;
import com.accessguard.api.dto.UsuarioUpdateRequest;
import com.accessguard.domain.usuario.Role;
import com.accessguard.domain.usuario.StatusUsuario;
import com.accessguard.domain.usuario.Usuario;
import com.accessguard.domain.usuario.UsuarioRepository;
import com.accessguard.security.AccessGuardUserDetails;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/usuarios")
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioApiController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioApiController(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public ResponseEntity<PageResponse<UsuarioDTO>> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("nome").ascending());
        Page<Usuario> usuarios;

        if (search != null && !search.isBlank()) {
            usuarios = usuarioRepository.findByEmailContainingIgnoreCaseOrNomeContainingIgnoreCase(search, search, pageable);
        } else {
            usuarios = usuarioRepository.findAll(pageable);
        }

        return ResponseEntity.ok(PageResponse.of(usuarios.map(this::toDTO)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioDTO> buscar(@PathVariable Long id) {
        return usuarioRepository.findById(id)
                .map(u -> ResponseEntity.ok(toDTO(u)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<UsuarioDTO> criar(@Valid @RequestBody UsuarioCreateRequest request) {
        if (usuarioRepository.findByEmail(request.email()).isPresent()) {
            return ResponseEntity.badRequest().build();
        }

        Usuario usuario = new Usuario(
            request.email(),
            passwordEncoder.encode(request.senha()),
            request.nome(),
            request.role()
        );
        usuario = usuarioRepository.save(usuario);
        return ResponseEntity.created(URI.create("/api/admin/usuarios/" + usuario.getId()))
                .body(toDTO(usuario));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioDTO> atualizar(@PathVariable Long id, @Valid @RequestBody UsuarioUpdateRequest request) {
        return usuarioRepository.findById(id)
                .map(usuario -> {
                    usuario.setNome(request.nome());
                    usuario.setRole(request.role());
                    usuario.setStatus(request.status());
                    if (request.senha() != null && !request.senha().isBlank()) {
                        usuario.setSenhaHash(passwordEncoder.encode(request.senha()));
                    }
                    usuario = usuarioRepository.save(usuario);
                    return ResponseEntity.ok(toDTO(usuario));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/desbloquear")
    public ResponseEntity<UsuarioDTO> desbloquear(@PathVariable Long id) {
        return usuarioRepository.findById(id)
                .map(usuario -> {
                    usuario.desbloquear();
                    usuario = usuarioRepository.save(usuario);
                    return ResponseEntity.ok(toDTO(usuario));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id, @AuthenticationPrincipal AccessGuardUserDetails userDetails) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findById(id);
        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Usuario usuario = usuarioOpt.get();
        if (usuario.getEmail().equals(userDetails.getUsername())) {
            return ResponseEntity.badRequest().build();
        }

        usuarioRepository.delete(usuario);
        return ResponseEntity.noContent().build();
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