package com.accessguard.api.dto;

public record LoginResponse(
    String token,
    String type,
    Long expiresIn,
    UserInfo user
) {
    public record UserInfo(
        Long id,
        String email,
        String nome,
        String role
    ) {}
}