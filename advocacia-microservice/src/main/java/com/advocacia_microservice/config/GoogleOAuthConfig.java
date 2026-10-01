package com.advocacia_microservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.*;

@Configuration
public class GoogleOAuthConfig {
    @Bean
    @Conditional(CredenciaisGooglePresentes.class)
    public ClientRegistrationRepository googleClientRegistration(
            @Value("${app.auth.google.client-id}") String clientId,
            @Value("${app.auth.google.client-secret}") String clientSecret,
            @Value("${app.auth.google.redirect-uri}") String redirectUri) {
        var google = CommonOAuth2Provider.GOOGLE.getBuilder("google")
                .clientId(clientId)
                .clientSecret(clientSecret)
                .redirectUri(redirectUri)
                .scope("openid", "email", "profile")
                .build();
        return new InMemoryClientRegistrationRepository(google);
    }

    static class CredenciaisGooglePresentes implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            String id = context.getEnvironment().getProperty("app.auth.google.client-id", "");
            String secret = context.getEnvironment().getProperty("app.auth.google.client-secret", "");
            return !id.isBlank() && !secret.isBlank();
        }
    }
}
