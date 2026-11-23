package com.accessguard.domain.auditoria;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface LogAcessoRepository extends JpaRepository<LogAcesso, Long>, JpaSpecificationExecutor<LogAcesso> {

    List<LogAcesso> findAllByOrderByCriadoEmDesc();
}