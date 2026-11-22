package com.accessguard.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PerfilUpdateRequest(
    @NotBlank @Size(max = 150) String nome
) {}