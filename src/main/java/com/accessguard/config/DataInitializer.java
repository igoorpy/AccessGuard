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
        if (usuarioRepository.findByEmail("admin@accessguard.com").isEmpty()) {
            usuarioRepository.save(new Usuario(
                    "admin@accessguard.com",
                    passwordEncoder.encode("Admin@123"),
                    "Administrador",
                    Role.ADMIN
            ));
        }
        if (usuarioRepository.findByEmail("auditor@accessguard.com").isEmpty()) {
            usuarioRepository.save(new Usuario(
                    "auditor@accessguard.com",
                    passwordEncoder.encode("Auditor@123"),
                    "Auditor",
                    Role.AUDITOR
            ));
        }
        if (usuarioRepository.findByEmail("colab@accessguard.com").isEmpty()) {
            usuarioRepository.save(new Usuario(
                    "colab@accessguard.com",
                    passwordEncoder.encode("Colab@123"),
                    "Colaborador",
                    Role.COLABORADOR
            ));
        }
    }
}