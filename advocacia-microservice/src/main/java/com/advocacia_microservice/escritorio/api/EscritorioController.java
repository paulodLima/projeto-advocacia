package com.advocacia_microservice.escritorio.api;

import com.advocacia_microservice.escritorio.application.EscritorioService;
import com.advocacia_microservice.escritorio.api.response.AcessoEscritorioResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/escritorios")
public class EscritorioController {
    private static final String ESCRITORIO_ATIVO = "escritorio.ativo";
    private final EscritorioService service;

    public EscritorioController(EscritorioService service) { this.service = service; }

    @GetMapping
    public List<AcessoEscritorioResponse> listar(Principal principal) {
        return service.listar(UUID.fromString(principal.getName())).stream().map(AcessoEscritorioResponse::de).toList();
    }

    @PutMapping("/{id}/contexto")
    public AcessoEscritorioResponse selecionar(Principal principal, @PathVariable UUID id, HttpServletRequest request) {
        var acesso = service.validar(UUID.fromString(principal.getName()), id);
        request.getSession().setAttribute(ESCRITORIO_ATIVO, id);
        return AcessoEscritorioResponse.de(acesso);
    }

    @GetMapping("/contexto")
    public AcessoEscritorioResponse contexto(Principal principal, HttpServletRequest request) {
        var sessao = request.getSession(false);
        var id = sessao == null ? null : sessao.getAttribute(ESCRITORIO_ATIVO);
        if (!(id instanceof UUID escritorioId)) throw new AccessDeniedException("Selecione um escritório.");
        try {
            return AcessoEscritorioResponse.de(service.validar(UUID.fromString(principal.getName()), escritorioId));
        } catch (AccessDeniedException erro) {
            sessao.removeAttribute(ESCRITORIO_ATIVO);
            throw erro;
        }
    }
}
