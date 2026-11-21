package com.accessguard.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String login(@RequestParam(value = "error", required = false) String error,
                        @RequestParam(value = "logout", required = false) String logout,
                        @RequestParam(value = "blocked", required = false) String blocked,
                        Model model) {
        if (error != null) {
            model.addAttribute("error", "Credenciais inválidas ou conta bloqueada");
        }
        if (logout != null) {
            model.addAttribute("message", "Logout realizado com sucesso");
        }
        if (blocked != null) {
            model.addAttribute("error", "Conta bloqueada por excesso de tentativas. Contate o administrador.");
        }
        return "login";
    }
}