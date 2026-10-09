package com.advocacia_microservice.documento.api;
import com.advocacia_microservice.documento.application.DocumentosConfigService;
import com.advocacia_microservice.documento.domain.PapelTimbrado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/config/documentos")
public class DocumentosConfigController {
    public record ModelosRequest(@NotNull @Size(max=9) Map<@NotNull @Size(max=50) String, @NotNull @Size(max=20000) String> modelos) {}
    private final DocumentosConfigService service;
    public DocumentosConfigController(DocumentosConfigService service) { this.service = service; }
    @GetMapping public DocumentosConfigService.Config buscar(Principal p) { return service.buscar(UUID.fromString(p.getName())); }
    @PutMapping("/papel") public PapelTimbrado papel(Principal p, @RequestBody PapelTimbrado body) { return service.salvarPapel(UUID.fromString(p.getName()), body); }
    @PutMapping("/modelos") public Map<String, String> modelos(Principal p, @Valid @RequestBody ModelosRequest body) { return service.salvarModelos(UUID.fromString(p.getName()), body.modelos()); }
}
