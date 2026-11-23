package com.accessguard.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.util.Map;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain apiFilterChain(HttpSecurity http,
                                               AuthenticationProvider authenticationProvider,
                                               UserDetailsService userDetailsService,
                                               ObjectMapper objectMapper,
                                               JwtAuthenticationFilter jwtFilter) throws Exception {
        http
                .securityMatcher("/api/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login", "/api/auth/validate").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/auditoria/**").hasAnyRole("ADMIN", "AUDITOR")
                        .requestMatchers("/api/perfil/**").authenticated()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .csrf(csrf -> csrf.disable())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            objectMapper.writeValue(response.getWriter(), Map.of(
                                    "status", 401,
                                    "error", "Unauthorized",
                                    "message", "Credenciais inválidas ou ausentes"
                            ));
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");
                            objectMapper.writeValue(response.getWriter(), Map.of(
                                    "status", 403,
                                    "error", "Forbidden",
                                    "message", "Acesso negado"
                            ));
                        })
                )
                .authenticationProvider(authenticationProvider)
                .userDetailsService(userDetailsService)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public SecurityFilterChain webFilterChain(HttpSecurity http,
                                               AuthenticationProvider authenticationProvider,
                                               UserDetailsService userDetailsService,
                                               AuthenticationSuccessHandler successHandler,
                                               AuthenticationFailureHandler failureHandler) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/css/**", "/js/**", "/webjars/**", "/favicon.ico").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/auditoria/**").hasAnyRole("ADMIN", "AUDITOR")
                        .requestMatchers("/perfil/**", "/dashboard").authenticated()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .successHandler(successHandler)
                        .failureHandler(failureHandler)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .sessionManagement(session -> session
                        .sessionFixation().migrateSession()
                        .maximumSessions(10)
                        .maxSessionsPreventsLogin(false)
                )
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/actuator/**")
                )
                .exceptionHandling(ex -> ex
                        .accessDeniedPage("/403")
                )
                .authenticationProvider(authenticationProvider)
                .userDetailsService(userDetailsService);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider(BruteForceAuthenticationProvider provider) {
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(BruteForceAuthenticationProvider provider) {
        return new org.springframework.security.authentication.ProviderManager(provider);
    }
}