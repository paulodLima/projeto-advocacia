import { HttpClient } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { tap } from 'rxjs';
import { API_URL } from '../config/api-url.token';

export interface Empresa {
  id: string; nome: string; identificacao: string; ativo: boolean;
  razaoSocial: string | null; cnpj: string | null; quantidadeFuncionarios: number | null;
  email: string | null; telefone: string | null; endereco: string | null;
  cidade: string | null; uf: string | null; cep: string | null;
}
export interface ListaEmpresas { itens: Empresa[]; total: number; pagina: number; paginas: number; }

@Injectable({ providedIn: 'root' })
export class PlataformaService {
  private readonly http = inject(HttpClient);
  private readonly url = inject(API_URL).replace(/\/$/, '') + '/api/plataforma';
  readonly podeAdministrar = signal(false);
  carregarAcesso() {
    return this.http.get<{ podeAdministrar: boolean }>(this.url + '/acesso')
      .pipe(tap((acesso) => this.podeAdministrar.set(acesso.podeAdministrar)));
  }
  carregarPainel() {
    return this.http.get<{ titulo: string; proximaEntrega: string }>(this.url + '/admin/painel');
  }
  listarEmpresas(pagina = 0) { return this.http.get<ListaEmpresas>(this.url + '/admin/empresas', { params: { pagina } }); }
  carregarEmpresa(id: string) { return this.http.get<Empresa>(this.url + '/admin/empresas/' + id); }
  salvarEmpresa(dados: Omit<Empresa, 'id'>, id?: string) {
    return id ? this.http.put<Empresa>(this.url + '/admin/empresas/' + id, dados)
      : this.http.post<Empresa>(this.url + '/admin/empresas', dados);
  }
}
