package com.accessguard.domain.auditoria;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface LogAcessoRepository extends JpaRepository<LogAcesso, Long> {

    @Query("""
        SELECT l FROM LogAcesso l
        WHERE (:email IS NULL OR l.usuarioEmail = :email)
          AND (:evento IS NULL OR l.evento = :evento)
          AND (:inicio IS NULL OR l.criadoEm >= :inicio)
          AND (:fim IS NULL OR l.criadoEm <= :fim)
          AND (:ip IS NULL OR l.ip = :ip)
        ORDER BY l.criadoEm DESC
    """)
    Page<LogAcesso> findByFilters(
            @Param("email") String email,
            @Param("evento") EventoAuditoria evento,
            @Param("inicio") Instant inicio,
            @Param("fim") Instant fim,
            @Param("ip") String ip,
            Pageable pageable
    );

    @Query("""
        SELECT l FROM LogAcesso l
        WHERE (:email IS NULL OR l.usuarioEmail = :email)
          AND (:evento IS NULL OR l.evento = :evento)
          AND (:inicio IS NULL OR l.criadoEm >= :inicio)
          AND (:fim IS NULL OR l.criadoEm <= :fim)
        ORDER BY l.criadoEm DESC
    """)
    List<LogAcesso> findByFiltersForExport(
            @Param("email") String email,
            @Param("evento") EventoAuditoria evento,
            @Param("inicio") Instant inicio,
            @Param("fim") Instant fim
    );

    @Query("""
        SELECT l FROM LogAcesso l
        WHERE (:email IS NULL OR l.usuarioEmail = :email)
          AND (:evento IS NULL OR l.evento = :evento)
          AND (:inicio IS NULL OR l.criadoEm >= :inicio)
          AND (:fim IS NULL OR l.criadoEm <= :fim)
          AND (:ip IS NULL OR l.ip = :ip)
        ORDER BY l.criadoEm DESC
    """)
    Page<LogAcesso> findWithFilters(
            @Param("email") String email,
            @Param("evento") EventoAuditoria evento,
            @Param("inicio") Instant inicio,
            @Param("fim") Instant fim,
            @Param("ip") String ip,
            Pageable pageable
    );
}