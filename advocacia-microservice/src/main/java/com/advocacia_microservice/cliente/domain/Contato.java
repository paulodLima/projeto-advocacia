package com.advocacia_microservice.cliente.domain;

import java.time.LocalDate;
import java.util.*;

public record Contato(UUID id, UUID empresaId, int versao, Map<String,String> dados,
                      List<Map<String,String>> representantes, List<UUID> indicadores, boolean cadastroIncompleto) {
    public Contato(UUID id, UUID empresaId, int versao, Map<String,String> dados, List<Map<String,String>> representantes, List<UUID> indicadores) {
        this(id,empresaId,versao,dados,representantes,indicadores,false);
    }
    public static final Set<String> TIPOS = Set.of("Cliente","Parte adversa","Parte interessada","Advogado(a)","Fornecedor","Parceiro");
    public static final Set<String> CAMPOS = Set.of("nome","tipo","tipo_pessoa","documento","nome_fantasia","email","telefone","telefone2","whatsapp","cep","logradouro","numero","complemento","bairro","cidade","uf","origem_id","observacoes","data_nascimento","rg","rg_orgao_emissor","nacionalidade","estado_civil","profissao","carteira","carteira_parceiro_id");
    public static final Set<String> REPRESENTANTE = Set.of("nome","cpf","cargo","telefone","data_nascimento","rg","rg_orgao_emissor","nacionalidade","estado_civil");
    public Contato {
        dados = normalizar(dados,CAMPOS);
        if(dados.get("nome").isEmpty()) throw new IllegalArgumentException("O nome completo ou razão social é obrigatório.");
        if(!TIPOS.contains(dados.get("tipo")) || !Set.of("PF","PJ").contains(dados.get("tipo_pessoa"))) throw new IllegalArgumentException("Tipo de contato ou pessoa inválido.");
        if(!Set.of("telefone","telefone2","nenhum").contains(dados.get("whatsapp")) || !Set.of("casa","parceiro").contains(dados.get("carteira"))) throw new IllegalArgumentException("Selecione WhatsApp e carteira válidos.");
        var copia = new HashMap<>(dados);
        copia.put("email",dados.get("email").toLowerCase(Locale.ROOT));
        copia.put("uf",dados.get("uf").toUpperCase(Locale.ROOT));
        validarDocumento(dados.get("documento"),dados.get("tipo_pessoa").equals("PF")?11:14);
        validarTelefone(dados.get("telefone")); validarTelefone(dados.get("telefone2"));
        for(var campo:List.of("documento","telefone","telefone2")) copia.put(campo,dados.get(campo).replaceAll("\\D",""));
        if(!dados.get("email").isEmpty() && !dados.get("email").matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) throw new IllegalArgumentException("Informe um e-mail válido.");
        if(!cadastroIncompleto && dados.get("tipo").equals("Cliente") && (dados.get("documento").isEmpty() || dados.get("telefone").isEmpty() || dados.get("email").isEmpty())) throw new IllegalArgumentException("Para cadastrar um cliente, informe CPF/CNPJ, telefone principal e e-mail.");
        if(!dados.get("cep").isEmpty() && !dados.get("cep").matches("\\d{5}-?\\d{3}")) throw new IllegalArgumentException("O CEP deve ter oito dígitos.");
        if(!copia.get("uf").isEmpty() && !Set.of("AC","AL","AP","AM","BA","CE","DF","ES","GO","MA","MT","MS","MG","PA","PB","PR","PE","PI","RJ","RN","RS","RO","RR","SC","SP","SE","TO").contains(copia.get("uf"))) throw new IllegalArgumentException("UF inválida.");
        data(dados.get("data_nascimento")); estadoCivil(dados.get("estado_civil"));
        uuid(dados.get("origem_id")); uuid(dados.get("carteira_parceiro_id"));
        if(dados.get("carteira").equals("casa")) copia.put("carteira_parceiro_id","");
        else if(dados.get("carteira_parceiro_id").isEmpty()) throw new IllegalArgumentException("Selecione o parceiro responsável pela carteira.");
        dados=Map.copyOf(copia);
        if(representantes==null || representantes.size()>20) throw new IllegalArgumentException("Cadastre até 20 representantes.");
        representantes=representantes.stream().map(r->{var m=normalizar(r,REPRESENTANTE); if(m.get("nome").isEmpty()) throw new IllegalArgumentException("Informe o nome do representante."); validarDocumento(m.get("cpf"),11); validarTelefone(m.get("telefone"));data(m.get("data_nascimento"));estadoCivil(m.get("estado_civil"));var limpo=new HashMap<>(m);for(var campo:List.of("cpf","telefone")) limpo.put(campo,m.get(campo).replaceAll("\\D",""));return Map.copyOf(limpo);}).toList();
        if(!dados.get("tipo_pessoa").equals("PJ")) representantes=List.of();
        if(indicadores==null || indicadores.size()>50 || indicadores.stream().anyMatch(Objects::isNull) || new HashSet<>(indicadores).size()!=indicadores.size()) throw new IllegalArgumentException("Indicadores inválidos.");
        indicadores=List.copyOf(indicadores);
    }
    private static Map<String,String> normalizar(Map<String,String> entrada,Set<String> campos) {
        if(entrada==null || !campos.containsAll(entrada.keySet())) throw new IllegalArgumentException("Campos de contato inválidos.");
        var dados=new HashMap<String,String>();
        for(var campo:campos) {
            var valor=entrada.getOrDefault(campo,""); int limite=campo.equals("observacoes")?4000:campo.equals("email")?254:150;
            if(valor==null || valor.length()>limite) throw new IllegalArgumentException("Verifique o campo "+campo+".");
            dados.put(campo,valor.strip());
        }
        return Map.copyOf(dados);
    }
    private static void validarDocumento(String valor,int tamanho) { if(!valor.isEmpty() && (!valor.matches("[0-9./ -]+") || valor.replaceAll("\\D","").length()!=tamanho)) throw new IllegalArgumentException("CPF/CNPJ com quantidade de dígitos inválida."); }
    private static void validarTelefone(String valor) { if(!valor.isEmpty() && (!valor.matches("[0-9()+ -]+") || !Set.of(10,11).contains(valor.replaceAll("\\D","").length()))) throw new IllegalArgumentException("Telefone deve ter DDD e 10 ou 11 dígitos."); }
    private static void data(String valor) { if(!valor.isEmpty()) { try {var data=LocalDate.parse(valor);if(data.isAfter(LocalDate.now())||data.isBefore(LocalDate.of(1900,1,1))) throw new IllegalArgumentException();}catch(Exception e){throw new IllegalArgumentException("Data de nascimento inválida.");} } }
    private static void estadoCivil(String valor) {if(!valor.isEmpty() && !Set.of("Solteiro(a)","Casado(a)","Divorciado(a)","Viúvo(a)","União estável").contains(valor)) throw new IllegalArgumentException("Estado civil inválido.");}
    public static UUID uuid(String valor) { return valor==null||valor.isEmpty()?null:UUID.fromString(valor); }
}
