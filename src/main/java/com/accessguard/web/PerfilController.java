package com.accessguard.web;

import com.accessguard.domain.usuario.Usuario;
import com.accessguard.domain.usuario.UsuarioRepository;
import com.accessguard.security.AccessGuardUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/perfil")
public class PerfilController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public PerfilController(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public String perfil(@AuthenticationPrincipal AccessGuardUserDetails userDetails, Model model) {
        Usuario usuario = usuarioRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        model.addAttribute("usuario", usuario);
        return "perfil";
    }

    @PostMapping("/atualizar-nome")
    public String atualizarNome(@AuthenticationPrincipal AccessGuardUserDetails userDetails,
                                @RequestParam String nome,
                                RedirectAttributes redirectAttributes) {
        Usuario usuario = usuarioRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        usuario.setNome(nome);
        usuarioRepository.save(usuario);
        redirectAttributes.addFlashAttribute("message", "Nome atualizado com sucesso");
        return "redirect:/perfil";
    }

    @PostMapping("/alterar-senha")
    public String alterarSenha(@AuthenticationPrincipal AccessGuardUserDetails userDetails,
                               @RequestParam String senhaAtual,
                               @RequestParam String novaSenha,
                               @RequestParam String confirmarSenha,
                               RedirectAttributes redirectAttributes) {
        Usuario usuario = usuarioRepository.findByEmail(userDetails.getUsername()).orElseThrow();

        if (!passwordEncoder.matches(senhaAtual, usuario.getSenhaHash())) {
            redirectAttributes.addFlashAttribute("error", "Senha atual incorreta");
            return "redirect:/perfil";
        }

        if (!novaSenha.equals(confirmarSenha)) {
            redirectAttributes.addFlashAttribute("error", "Nova senha e confirmação não conferem");
            return "redirect:/perfil";
        }

        if (novaSenha.length() < 8) {
            redirectAttributes.addFlashAttribute("error", "Nova senha deve ter pelo menos 8 caracteres");
            return "redirect:/perfil";
        }

        usuario.setSenhaHash(passwordEncoder.encode(novaSenha));
        usuarioRepository.save(usuario);
        redirectAttributes.addFlashAttribute("message", "Senha alterada com sucesso");
        return "redirect:/perfil";
    }
}