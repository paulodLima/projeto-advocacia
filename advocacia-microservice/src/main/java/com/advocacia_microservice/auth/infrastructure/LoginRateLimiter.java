package com.advocacia_microservice.auth.infrastructure;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Limite por IP para a instância atual; não utiliza headers de proxy não confiáveis. */
@Component
public class LoginRateLimiter {
    private record Janela(Instant inicio, int pedidos) {}
    private final Map<String, Janela> janelas = new HashMap<>();

    public synchronized boolean permitir(String ip) {
        Instant agora = Instant.now();
        janelas.entrySet().removeIf(item -> item.getValue().inicio().plusSeconds(3600).isBefore(agora));
        Janela atual = janelas.get(ip);
        if (atual == null) {
            if (janelas.size() >= 10000) return false;
            janelas.put(ip, new Janela(agora, 1));
            return true;
        }
        if (atual.pedidos() >= 30) return false;
        janelas.put(ip, new Janela(atual.inicio(), atual.pedidos() + 1));
        return true;
    }
}
