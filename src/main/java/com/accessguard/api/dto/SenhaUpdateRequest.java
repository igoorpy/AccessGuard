package com.accessguard.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SenhaUpdateRequest(
    @NotBlank @Size(min = 8, max = 255) String senhaAtual,
    @NotBlank @Size(min = 8, max = 255) String novaSenha,
    @NotBlank @Size(min = 8, max = 255) String confirmarSenha
) {}