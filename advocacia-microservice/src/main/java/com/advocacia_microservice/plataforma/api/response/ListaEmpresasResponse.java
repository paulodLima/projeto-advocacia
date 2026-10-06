package com.advocacia_microservice.plataforma.api.response;
import com.advocacia_microservice.escritorio.domain.PaginaEmpresas;
import java.util.List;
public record ListaEmpresasResponse(List<EmpresaResponse> itens, long total, int pagina, int paginas) {
    public static ListaEmpresasResponse de(PaginaEmpresas p) {
        return new ListaEmpresasResponse(p.itens().stream().map(EmpresaResponse::de).toList(), p.total(), p.pagina(), p.paginas());
    }
}
