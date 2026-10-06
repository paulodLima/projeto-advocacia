package com.advocacia_microservice.plataforma.api;

import com.advocacia_microservice.plataforma.application.AcessoPlataformaService;
import java.security.Principal;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/plataforma")
public class PlataformaController {
    private final AcessoPlataformaService acesso;
    public PlataformaController(AcessoPlataformaService acesso) { this.acesso = acesso; }
    @GetMapping("/acesso")
    public Map<String, Boolean> acesso(Principal principal) {
        return Map.of("podeAdministrar", acesso.podeAdministrar(principal.getName()));
    }
    @GetMapping("/admin/painel")
    public Map<String, String> painel() {
        return Map.of("titulo", "Administração da plataforma",
                "proximaEntrega", "Cadastro de empresas");
    }
}
