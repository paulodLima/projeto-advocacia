package com.advocacia_microservice.cadastro.application;

import com.advocacia_microservice.cadastro.domain.*;
import com.advocacia_microservice.cadastro.infrastructure.CadastrosRepository;
import com.advocacia_microservice.empresa.application.EmpresaService;
import com.advocacia_microservice.empresa.domain.PapelEmpresa;
import com.advocacia_microservice.shared.exception.*;
import java.util.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional(readOnly = true)
public class CadastrosService {
    public record Resposta(UUID empresaId, PapelEmpresa papel, Map<String, List<CadastroRegistro>> listas, List<CadastrosRepository.Rotina> rotinas, List<Sistema> sistemas) {}
    public record Inicio(UUID empresaId, List<CadastrosRepository.Rotina> rotinas, List<Sistema> sistemas) {}
    private final EmpresaService empresas;
    private final CadastrosRepository repository;
    public CadastrosService(EmpresaService empresas, CadastrosRepository repository) { this.empresas = empresas; this.repository = repository; }
    public Resposta buscar(UUID usuario) {
        var empresa = empresas.buscar(usuario); var listas = new HashMap<String, List<CadastroRegistro>>();
        CadastroRegistro.CAMPOS.keySet().forEach(tipo -> listas.put(tipo, new ArrayList<>()));
        if (empresa.id() == null) return new Resposta(null, null, listas, List.of(), List.of());
        repository.listar(empresa.id()).forEach(item -> listas.get(item.tipo()).add(item));
        return new Resposta(empresa.id(), empresa.papel(), listas, repository.rotinas(empresa.id()), repository.sistemas(empresa.id()));
    }
    private UUID editar(UUID usuario) { var empresa = empresas.exigirMaster(usuario).empresaId(); repository.bloquear(empresa); return empresa; }
    public Inicio inicio(UUID usuario) {
        var empresa = empresas.buscar(usuario);
        if (empresa.id() == null) return new Inicio(null, List.of(), List.of());
        return new Inicio(empresa.id(), repository.rotinas(empresa.id()), repository.sistemasAtivos(empresa.id()));
    }
    private CadastroRegistro exigir(UUID empresa, String tipo, UUID id) {
        return repository.listar(empresa).stream().filter(i -> i.id().equals(id) && i.tipo().equals(tipo)).findFirst().orElseThrow(() -> new RecursoNaoEncontradoException("Cadastro não encontrado na sua empresa."));
    }
    @Transactional
    public CadastroRegistro salvar(UUID usuario, String tipo, UUID id, Map<String, String> campos, boolean ativo) {
        UUID empresa = editar(usuario); boolean novo = id == null;
        if (!novo) exigir(empresa, tipo, id);
        var item = new CadastroRegistro(novo ? UUID.randomUUID() : id, tipo, ativo, campos, null);
        if (item.referenciaId() != null) {
            String destino = tipo.equals("acoes") ? "grupos" : "fases";
            var referencia = exigir(empresa, destino, item.referenciaId());
            if (!referencia.ativo()) throw new IllegalArgumentException("Selecione um grupo ou fase ativo.");
        }
        repository.salvar(empresa, item, novo); return item;
    }
    @Transactional
    public void excluir(UUID usuario, String tipo, UUID id) {
        UUID empresa = editar(usuario); exigir(empresa, tipo, id);
        if (repository.usado(empresa, id)) throw new ConflitoException("Este cadastro está vinculado a outros itens. Remova os vínculos ou mantenha-o cadastrado.");
        repository.excluir(empresa, id);
    }
    @Transactional
    public CadastrosRepository.Rotina criarRotina(UUID usuario, String nome, String periodo) {
        UUID empresa = editar(usuario);
        if (nome == null || nome.isBlank() || nome.strip().length() > 150 || periodo == null || !Set.of("diaria", "semanal", "mensal", "anual").contains(periodo)) throw new IllegalArgumentException("Informe uma rotina de até 150 caracteres e um período válido.");
        return repository.criarRotina(empresa, nome.strip(), periodo);
    }
    @Transactional
    public void excluirRotina(UUID usuario, UUID id) { if (repository.excluirRotina(editar(usuario), id) == 0) throw new RecursoNaoEncontradoException("Rotina não encontrada na sua empresa."); }
    @Transactional
    public List<Sistema> salvarSistemas(UUID usuario, List<Sistema> sistemas) {
        UUID empresa = editar(usuario);
        if (sistemas == null || sistemas.size() > 30) throw new IllegalArgumentException("Cadastre até 30 acessos rápidos.");
        var ids = new HashSet<UUID>();
        for (var sistema : sistemas) {
            if (sistema == null || !ids.add(sistema.id())) throw new IllegalArgumentException("Acessos rápidos duplicados ou inválidos.");
            UUID dono = repository.empresaDoSistema(sistema.id());
            if (dono != null && !dono.equals(empresa)) throw new AccessDeniedException("Sistema fora da sua empresa.");
        }
        var normalizados = sistemas.stream().map(Sistema::normalizarLogo).toList();
        repository.salvarSistemas(empresa, normalizados); return normalizados;
    }
}
