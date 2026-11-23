package com.accessguard.config;

import com.accessguard.domain.usuario.Role;
import com.accessguard.domain.usuario.Usuario;
import com.accessguard.domain.usuario.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (usuarioRepository.count() == 0) {
            Usuario admin = new Usuario(
                    "admin@accessguard.com",
                    passwordEncoder.encode("Admin@123"),
                    "Administrador",
                    Role.ADMIN
            );
            Usuario auditor = new Usuario(
                    "auditor@accessguard.com",
                    passwordEncoder.encode("Auditor@123"),
                    "Auditor",
                    Role.AUDITOR
            );
            Usuario colaborador = new Usuario(
                    "colab@accessguard.com",
                    passwordEncoder.encode("Colab@123"),
                    "Colaborador",
                    Role.COLABORADOR
            );
            usuarioRepository.saveAll(java.util.List.of(admin, auditor, colaborador));
        }
    }
}