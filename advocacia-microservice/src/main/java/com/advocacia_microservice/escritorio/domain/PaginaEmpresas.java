package com.advocacia_microservice.escritorio.domain;
import java.util.List;
public record PaginaEmpresas(List<Empresa> itens, long total, int pagina, int paginas) {}
