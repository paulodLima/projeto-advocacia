package com.advocacia_microservice.crm.domain;

import java.time.*;
import java.util.*;

public record Lead(UUID id, UUID empresaId, int versao, Dados dados, LocalDate proximo,
                   List<Tratativa> contatos, List<Comentario> comentarios) {
    public static final List<String> ORIGENS = List.of("Instagram Ads","Google Ads","Facebook Ads","Meta (orgânico)","Indicação","Site / Landing","WhatsApp","Outro");
    public static final List<String> CANAIS = List.of("WhatsApp","Ligação","E-mail","Instagram DM","SMS");
    public static final List<String> RESULTADOS = List.of("Sem resposta","Conversou — segue interessado","Pediu para retornar depois","Quer agendar consultoria","Sem interesse");
    public static final Set<String> FACA = Set.of("fit","assunto","capacidade","autoridade");
    public record Cadastro(String nome, String telefone, String email, String origem, String necessidade,
                           String temperatura, String observacoes, List<UUID> etiquetas) {
        public Cadastro {
            nome = texto(nome,150,true); telefone = texto(telefone,30,true); email = texto(email,254,false).toLowerCase(Locale.ROOT);
            origem = texto(origem,150,false); necessidade = texto(necessidade,150,true); observacoes = texto(observacoes,4000,false);
            if (!telefone.matches("[0-9()+ -]+") || !Set.of(10,11).contains(telefone.replaceAll("\\D","").length())) throw new IllegalArgumentException("Informe telefone com DDD.");
            telefone = telefone.replaceAll("\\D", "");
            if (!email.isEmpty() && !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) throw new IllegalArgumentException("Informe um e-mail válido.");
            if (!Set.of("quente","morno","frio").contains(Objects.requireNonNullElse(temperatura,""))) throw new IllegalArgumentException("Temperatura inválida.");
            etiquetas = ids(etiquetas);
        }
    }
    public record Estado(String status, boolean cadenciaPausada, LocalDate retornoEm, boolean consultaAgendada,
                         LocalDateTime consultaEm, String consultaStatus, Map<String,Boolean> qualificacao) {
        public Estado {
            if (!Set.of("ativo","agendou","ganho","perdido").contains(Objects.requireNonNullElse(status,""))) throw new IllegalArgumentException("Status inválido.");
            if (qualificacao == null || qualificacao.keySet().stream().anyMatch(Objects::isNull) || !FACA.containsAll(qualificacao.keySet())) throw new IllegalArgumentException("Qualificação inválida.");
            qualificacao = Collections.unmodifiableMap(new HashMap<>(qualificacao));
            if (!cadenciaPausada) retornoEm = null;
            if (!consultaAgendada) { consultaEm = null; consultaStatus = null; }
            else {
                if (!Set.of("marcada","confirmada","realizada","nao_compareceu").contains(Objects.requireNonNullElse(consultaStatus,""))) throw new IllegalArgumentException("Situação da consulta inválida.");
                if (consultaEm != null && (consultaEm.getYear()<2000 || consultaEm.getYear()>2200)) throw new IllegalArgumentException("Data da consulta inválida.");
            }
            if (retornoEm != null && (retornoEm.getYear()<2000 || retornoEm.getYear()>2200)) throw new IllegalArgumentException("Data de retorno inválida.");
        }
        public static Estado inicial() { return new Estado("ativo",false,null,false,null,null,Map.of()); }
        public Estado comStatus(String novo) { return new Estado(novo,cadenciaPausada,retornoEm,consultaAgendada,consultaEm,consultaStatus,qualificacao); }
    }
    public record Dados(Cadastro cadastro, Estado estado, LocalDate entrada, LocalDate ultimoContato, int passo, UUID casoId) {}
    public record Passo(int passo, int dias, String rotulo) {
        public Passo { if(passo<0 || passo>19 || dias<0 || dias>365) throw new IllegalArgumentException("Passo da cadência inválido."); rotulo=texto(rotulo,80,true); }
    }
    public record Tratativa(UUID id, LocalDate data, String canal, String resultado, String observacao, UUID autorId) {}
    public record Comentario(UUID id, UUID autorId, String autor, String texto, String criadoEm, List<UUID> mencoes) {}
    public static LocalDate proximo(Dados d,List<Passo> passos) {
        if(!d.estado.status.equals("ativo")) return null;
        if(d.estado.cadenciaPausada) return d.estado.retornoEm;
        return passos.stream().filter(p->p.passo==d.passo).findFirst().map(p->(d.ultimoContato==null?d.entrada:d.ultimoContato).plusDays(p.dias)).orElse(null);
    }
    public static String texto(String valor,int limite,boolean obrigatorio) {
        if(valor==null) valor=""; valor=valor.strip();
        if(valor.length()>limite || (obrigatorio && valor.isEmpty())) throw new IllegalArgumentException("Verifique os campos obrigatórios e seus limites.");
        return valor;
    }
    public static List<UUID> ids(List<UUID> ids) {
        if(ids==null || ids.size()>50 || ids.stream().anyMatch(Objects::isNull) || new HashSet<>(ids).size()!=ids.size()) throw new IllegalArgumentException("Etiquetas ou menções inválidas.");
        return List.copyOf(ids);
    }
}
