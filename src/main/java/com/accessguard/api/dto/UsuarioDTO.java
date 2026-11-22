package com.accessguard.api.dto;

import com.accessguard.domain.usuario.Role;
import com.accessguard.domain.usuario.StatusUsuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UsuarioDTO(
    Long id,
    @NotBlank @Email @Size(max = 255) String email,
    @NotBlank @Size(max = 150) String nome,
    @NotNull Role role,
    @NotNull StatusUsuario status,
    Integer tentativasFalha,
    String ultimoLoginEm
) {}