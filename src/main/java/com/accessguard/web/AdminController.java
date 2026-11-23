package com.accessguard.web;

import com.accessguard.domain.usuario.Role;
import com.accessguard.domain.usuario.StatusUsuario;
import com.accessguard.domain.usuario.Usuario;
import com.accessguard.domain.usuario.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminController(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/usuarios")
    public String listarUsuarios(@RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "10") int size,
                                 @RequestParam(required = false) String search,
                                 Model model) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("nome").ascending());
        Page<Usuario> usuarios;

        if (search != null && !search.isBlank()) {
            usuarios = usuarioRepository.findByEmailContainingIgnoreCaseOrNomeContainingIgnoreCase(search, search, pageable);
        } else {
            usuarios = usuarioRepository.findAll(pageable);
        }

        model.addAttribute("usuarios", usuarios);
        model.addAttribute("search", search);
        model.addAttribute("roles", Role.values());
        model.addAttribute("statuses", StatusUsuario.values());
        return "admin/usuarios";
    }

    @GetMapping("/usuarios/novo")
    public String novoUsuarioForm(Model model) {
        model.addAttribute("usuario", new UsuarioForm());
        model.addAttribute("roles", Role.values());
        model.addAttribute("statuses", StatusUsuario.values());
        model.addAttribute("isEdit", false);
        return "admin/usuario-form";
    }

    @PostMapping("/usuarios/novo")
    public String criarUsuario(@ModelAttribute UsuarioForm form, RedirectAttributes redirectAttributes) {
        if (form.getSenha() == null || form.getSenha().isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Senha é obrigatória");
            return "redirect:/admin/usuarios/novo";
        }
        if (usuarioRepository.findByEmail(form.getEmail()).isPresent()) {
            redirectAttributes.addFlashAttribute("error", "Email já cadastrado");
            return "redirect:/admin/usuarios/novo";
        }

        Usuario usuario = new Usuario(form.getEmail(), passwordEncoder.encode(form.getSenha()), form.getNome(), form.getRole());
        usuarioRepository.save(usuario);
        redirectAttributes.addFlashAttribute("message", "Usuário criado com sucesso");
        return "redirect:/admin/usuarios";
    }

    @GetMapping("/usuarios/{id}/editar")
    public String editarUsuarioForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findById(id);
        if (usuarioOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Usuário não encontrado");
            return "redirect:/admin/usuarios";
        }

        Usuario usuario = usuarioOpt.get();
        model.addAttribute("usuario", new UsuarioForm(usuario.getEmail(), null, usuario.getNome(), usuario.getRole()));
        model.addAttribute("usuarioId", id);
        model.addAttribute("roles", Role.values());
        model.addAttribute("statuses", StatusUsuario.values());
        model.addAttribute("isEdit", true);
        return "admin/usuario-form";
    }

    @PostMapping("/usuarios/{id}/editar")
    public String atualizarUsuario(@PathVariable Long id, @ModelAttribute UsuarioForm form, RedirectAttributes redirectAttributes) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findById(id);
        if (usuarioOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Usuário não encontrado");
            return "redirect:/admin/usuarios";
        }

        Usuario usuario = usuarioOpt.get();
        usuario.setNome(form.getNome());
        usuario.setRole(form.getRole());
        usuario.setStatus(form.getStatus());
        if (form.getSenha() != null && !form.getSenha().isBlank()) {
            usuario.setSenhaHash(passwordEncoder.encode(form.getSenha()));
        }
        usuarioRepository.save(usuario);
        redirectAttributes.addFlashAttribute("message", "Usuário atualizado com sucesso");
        return "redirect:/admin/usuarios";
    }

    @PostMapping("/usuarios/{id}/desbloquear")
    public String desbloquearUsuario(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findById(id);
        if (usuarioOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Usuário não encontrado");
            return "redirect:/admin/usuarios";
        }

        Usuario usuario = usuarioOpt.get();
        usuario.desbloquear();
        usuarioRepository.save(usuario);
        redirectAttributes.addFlashAttribute("message", "Usuário desbloqueado com sucesso");
        return "redirect:/admin/usuarios";
    }

    public static class UsuarioForm {
        private String email;
        private String senha;
        private String nome;
        private Role role;
        private StatusUsuario status;

        public UsuarioForm() {}

        public UsuarioForm(String email, String senha, String nome, Role role, StatusUsuario status) {
            this.email = email;
            this.senha = senha;
            this.nome = nome;
            this.role = role;
            this.status = status;
        }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getSenha() { return senha; }
        public void setSenha(String senha) { this.senha = senha; }
        public String getNome() { return nome; }
        public void setNome(String nome) { this.nome = nome; }
        public Role getRole() { return role; }
        public void setRole(Role role) { this.role = role; }
        public StatusUsuario getStatus() { return status; }
        public void setStatus(StatusUsuario status) { this.status = status; }
    }
}