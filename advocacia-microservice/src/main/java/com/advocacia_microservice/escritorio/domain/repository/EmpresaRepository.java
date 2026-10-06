package com.advocacia_microservice.escritorio.domain.repository;
import com.advocacia_microservice.escritorio.domain.*;
import java.util.*;
public interface EmpresaRepository {
    Empresa salvar(Empresa empresa);
    Optional<Empresa> buscarPorId(UUID id);
    List<Empresa> buscarPorIds(List<UUID> ids);
    PaginaEmpresas listar(int pagina, int tamanho);
}
