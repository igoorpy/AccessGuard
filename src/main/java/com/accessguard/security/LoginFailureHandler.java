package com.accessguard.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class LoginFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                         HttpServletResponse response,
                                         AuthenticationException exception) throws IOException, ServletException {
        String errorMessage;

        if (exception instanceof LockedException) {
            errorMessage = "Conta bloqueada. Contate o administrador.";
        } else {
            errorMessage = "E-mail ou senha inválidos.";
        }

        response.sendRedirect("/login?error=" + java.net.URLEncoder.encode(errorMessage, "UTF-8"));
    }
}