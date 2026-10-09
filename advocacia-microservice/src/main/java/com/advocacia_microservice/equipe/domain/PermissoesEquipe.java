package com.advocacia_microservice.equipe.domain;

import java.util.*;

public final class PermissoesEquipe {
    public static final Set<String> TODOS = Set.of("inicio", "config", "agenda", "crm", "casos", "processual", "contatos", "parceiros", "equipe", "documentos", "calculadoras", "teses", "pops", "financeiro", "relatorios", "produtos");
    private PermissoesEquipe() {}
    public static Set<String> resolver(PerfilEquipe perfil, boolean restrito, Set<String> abas) {
        if (abas == null || !TODOS.containsAll(abas)) throw new IllegalArgumentException("Módulo de acesso inválido.");
        var permitidos = new HashSet<String>();
        if (restrito) permitidos.addAll(abas);
        else switch (perfil) {
            case ADMINISTRADOR -> permitidos.addAll(TODOS);
            case ADVOGADO, ASSISTENTE -> { permitidos.addAll(TODOS); permitidos.removeAll(Set.of("financeiro", "relatorios", "produtos")); }
            case FINANCEIRO -> permitidos.addAll(Set.of("agenda", "contatos", "financeiro", "relatorios", "produtos"));
        }
        permitidos.addAll(Set.of("inicio", "config"));
        return Set.copyOf(permitidos);
    }
}
