package com.advocacia_microservice.auth.api;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import com.advocacia_microservice.auth.application.ProvedoresLoginService;
import org.springframework.web.bind.annotation.*;

@RestController
public class GoogleProviderController {
    private final ProvedoresLoginService provedores;
    private final String publicUrl;

    public GoogleProviderController(ProvedoresLoginService provedores,
                                    @Value("${app.auth.public-url}") String publicUrl) {
        this.provedores = provedores;
        this.publicUrl = publicUrl.replaceAll("/+$", "");
    }

    @GetMapping("/api/auth/providers")
    public Map<String, Boolean> providers() {
        return Map.of("google", provedores.googleDisponivel());
    }

    // Com OAuth ativo, o filtro do Spring intercepta este caminho antes do controller.
    @GetMapping("/oauth2/authorization/google")
    public void indisponivel(HttpServletResponse response) throws IOException {
        response.sendRedirect(publicUrl + "/login?erro=google_indisponivel");
    }
}
