package com.advocacia_microservice.plataforma.infrastructure;

import com.advocacia_microservice.plataforma.application.ProvisionarAdministradorService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class AdministradorInicial implements ApplicationRunner {
    private final ProvisionarAdministradorService service;
    private final String email;
    private final String nome;
    public AdministradorInicial(ProvisionarAdministradorService service,
            @Value("${app.plataforma.admin-inicial-email:}") String email,
            @Value("${app.plataforma.admin-inicial-nome:Administrador da plataforma}") String nome) {
        this.service = service;
        this.email = email;
        this.nome = nome;
    }
    @Override public void run(ApplicationArguments args) { service.executar(email, nome); }
}
