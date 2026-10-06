package com.advocacia_microservice.plataforma.application;
import com.advocacia_microservice.escritorio.domain.*;
import com.advocacia_microservice.escritorio.domain.repository.EmpresaRepository;
import com.advocacia_microservice.plataforma.domain.repository.AuditoriaRepository;
import com.advocacia_microservice.plataforma.domain.Cnpj;
import com.advocacia_microservice.plataforma.application.command.DadosEmpresa;
import com.advocacia_microservice.shared.exception.RecursoNaoEncontradoException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class EmpresasService {
    private final EmpresaRepository empresas;
    private final AuditoriaRepository auditoria;
    public EmpresasService(EmpresaRepository empresas, AuditoriaRepository auditoria) {
        this.empresas = empresas; this.auditoria = auditoria;
    }
    @Transactional(readOnly = true)
    public PaginaEmpresas listar(int pagina) {
        if (pagina < 0) throw new IllegalArgumentException("Página inválida.");
        return empresas.listar(pagina, 20);
    }
    @Transactional(readOnly = true)
    public Empresa buscar(UUID id) {
        return empresas.buscarPorId(id).orElseThrow(() -> new RecursoNaoEncontradoException("Empresa não encontrada."));
    }
    @Transactional
    public Empresa criar(DadosEmpresa dados, UUID autor) { return salvar(UUID.randomUUID(), dados, autor, "EMPRESA_CADASTRADA"); }
    @Transactional
    public Empresa editar(UUID id, DadosEmpresa dados, UUID autor) {
        buscar(id); return salvar(id, dados, autor, "EMPRESA_ATUALIZADA");
    }
    private Empresa salvar(UUID id, DadosEmpresa d, UUID autor, String acao) {
        var e = empresas.salvar(new Empresa(id, d.nome().strip(), d.identificacao(), d.ativo(), d.razaoSocial().strip(),
                Cnpj.normalizar(d.cnpj()), d.quantidadeFuncionarios().intValueExact(), limpar(d.email()),
                limpar(d.telefone()), limpar(d.endereco()), limpar(d.cidade()), limpar(d.uf()),
                d.cep() == null || d.cep().isBlank() ? null : d.cep().replace("-", "")));
        auditoria.registrar(autor, acao, e.id()); return e;
    }
    private String limpar(String valor) { return valor == null || valor.isBlank() ? null : valor.strip(); }
}
