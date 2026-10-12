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
export interface ConviteInicial { id: string; nome: string; email: string; status: string; expiraEm: string; empresaId: string | null; }
export interface GestaoEmpresa {
  id: string; nome: string; cnpj: string | null; responsavel: string | null; emailResponsavel: string | null;
  usuariosAtivos: number; situacao: string; financeiro: string; inicio: string | null; versao: number;
  modulos: { codigo: string; nome: string; situacao: string; liberado: boolean }[];
  historico: { id: string; autor: string; acao: string; anterior: string; atual: string; motivo: string; criadoEm: string }[];
}
export interface AlterarContrato { versao: number; situacao: string; financeiro: string; inicio: string | null; modulos: string[]; motivo: string; }

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
  listarConvites() { return this.http.get<ConviteInicial[]>(this.url + '/admin/convites'); }
  convidarResponsavel(nome: string, email: string) { return this.http.post<ConviteInicial>(this.url + '/admin/convites', { nome, email }); }
  reenviarConvite(id: string) { return this.http.post<ConviteInicial>(this.url + '/admin/convites/' + id + '/reenviar', {}); }
  cancelarConvite(id: string) { return this.http.delete<void>(this.url + '/admin/convites/' + id); }
  listarEmpresas(pagina = 0) { return this.http.get<ListaEmpresas>(this.url + '/admin/empresas', { params: { pagina } }); }
  listarGestaoEmpresas(busca = '', situacao = '', pagina = 0) { return this.http.get<GestaoEmpresa[]>(this.url + '/admin/gestao-empresas', { params: { busca, situacao, pagina } }); }
  carregarGestaoEmpresa(id: string) { return this.http.get<GestaoEmpresa>(this.url + '/admin/gestao-empresas/' + id); }
  alterarContrato(id: string, dados: AlterarContrato) { return this.http.put<GestaoEmpresa>(this.url + '/admin/gestao-empresas/' + id, dados); }
  carregarEmpresa(id: string) { return this.http.get<Empresa>(this.url + '/admin/empresas/' + id); }
  salvarEmpresa(dados: Omit<Empresa, 'id'>, id?: string) {
    return id ? this.http.put<Empresa>(this.url + '/admin/empresas/' + id, dados)
      : this.http.post<Empresa>(this.url + '/admin/empresas', dados);
  }
}
