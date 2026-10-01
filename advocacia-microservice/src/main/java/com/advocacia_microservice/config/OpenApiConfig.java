package com.advocacia_microservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI advocaciaOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Gestão de Advocacia")
                .description("API do projeto de advocacia")
                .version("v1"));
    }
}
