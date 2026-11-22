package com.accessguard.api.dto;

import com.accessguard.domain.usuario.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UsuarioCreateRequest(
    @NotBlank @Email @Size(max = 255) String email,
    @NotBlank @Size(min = 8, max = 255) String senha,
    @NotBlank @Size(max = 150) String nome,
    @NotNull Role role
) {}