package com.accessguard.domain.sessao;

import com.accessguard.domain.usuario.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SessaoRepository extends JpaRepository<Sessao, Long> {

    List<Sessao> findByUsuarioAndAtivaTrue(Usuario usuario);

    Optional<Sessao> findBySessionId(String sessionId);
}