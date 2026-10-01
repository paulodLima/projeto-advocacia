package com.advocacia_microservice.config;

import com.advocacia_microservice.usuario.domain.StatusUsuario;
import com.advocacia_microservice.usuario.infrastructure.persistence.JpaUsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.web.cors.*;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
public class SecurityConfig {
    @Bean
    public org.springframework.security.core.userdetails.UserDetailsService userDetailsService() {
        return username -> { throw new org.springframework.security.core.userdetails.UsernameNotFoundException("Use o código enviado por email."); };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @org.springframework.beans.factory.annotation.Value("${app.auth.public-url}") String publicUrl) {
        var config = new CorsConfiguration();
        var frontend = java.net.URI.create(publicUrl);
        config.setAllowedOrigins(List.of(frontend.getScheme() + "://" + frontend.getRawAuthority()));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Content-Type", "Authorization", "Accept", "X-CSRF-TOKEN"));
        config.setExposedHeaders(List.of("Location"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JpaUsuarioRepository usuarios,
            org.springframework.beans.factory.ObjectProvider<org.springframework.security.oauth2.client.registration.ClientRegistrationRepository> registrations,
            com.advocacia_microservice.auth.infrastructure.GoogleLoginSuccessHandler googleSuccess,
            @org.springframework.beans.factory.annotation.Value("${app.auth.public-url}") String publicUrl) throws Exception {
        http.cors(cors -> {});
        // CSRF permanece habilitado. O frontend obtém o token em /api/auth/csrf.
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/csrf", "/api/auth/codigo", "/api/auth/validar", "/api/auth/providers", "/api/auth/cadastro",
                        "/oauth2/authorization/**", "/login/oauth2/code/**",
                        "/swagger-ui/**", "/v3/api-docs/**", "/error").permitAll()
                .anyRequest().authenticated());
        http.exceptionHandling(errors -> errors
                .authenticationEntryPoint((request, response, error) -> response.sendError(HttpStatus.UNAUTHORIZED.value())));
        http.logout(logout -> logout.logoutUrl("/api/auth/logout")
                .deleteCookies("JSESSIONID")
                .logoutSuccessHandler((request, response, auth) -> response.setStatus(204)));
        if (registrations.getIfAvailable() != null) {
            http.oauth2Login(oauth -> oauth
                    .successHandler(googleSuccess)
                    .failureHandler((request, response, exception) -> {
                        SecurityContextHolder.clearContext();
                        var session = request.getSession(false);
                        if (session != null) session.invalidate();
                        response.sendRedirect(publicUrl.replaceAll("/+$", "") + "/login?erro=google_falhou");
                    }));
        }
        // Revoga sessões quando o usuário é excluído ou desativado.
        http.addFilterBefore(new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                            FilterChain chain) throws ServletException, IOException {
                var auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                    boolean ativo;
                    try {
                        ativo = usuarios.findById(UUID.fromString(auth.getName()))
                                .map(item -> item.paraDominio().status() == StatusUsuario.ATIVO).orElse(false);
                    } catch (IllegalArgumentException error) {
                        ativo = false;
                    }
                    if (!ativo) {
                        SecurityContextHolder.clearContext();
                        var session = request.getSession(false);
                        if (session != null) session.invalidate();
                    }
                }
                chain.doFilter(request, response);
            }
        }, AuthorizationFilter.class);
        return http.build();
    }
}
