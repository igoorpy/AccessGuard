package com.accessguard.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtUtil(@Value("${accessguard.jwt.secret:accessguard-secret-key-change-in-production-min-32-chars}") String secret,
                   @Value("${accessguard.jwt.expiration:1800000}") long expirationMs) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(AccessGuardUserDetails userDetails) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(expirationMs);

        List<String> roles = userDetails.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .collect(Collectors.toList());

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("roles", roles)
                .claim("userId", userDetails.getUsuario().getId())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(secretKey)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String getUsernameFromToken(String token) {
        return parseToken(token).getSubject();
    }

    public List<String> getRolesFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("roles", List.class);
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("userId", Long.class);
    }

    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
}