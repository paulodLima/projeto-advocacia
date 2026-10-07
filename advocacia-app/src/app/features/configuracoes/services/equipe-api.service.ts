import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { API_URL } from '../../../core/config/api-url.token';

export type PerfilEquipe = 'ADMINISTRADOR' | 'ADVOGADO' | 'ASSISTENTE' | 'FINANCEIRO';
export interface ConfiguracaoMembro {
  nomeExibicao: string; telefone: string; admissao: string | null; nascimento: string | null;
  perfil: PerfilEquipe; situacao: 'ATIVO' | 'INATIVO' | 'SUSPENSO'; restrito: boolean; abas: string[]; enviaDocumento: boolean;
}
export interface MembroEquipe { id: string; nome: string; email: string; papel: 'MASTER' | 'MEMBRO'; configuracao: ConfiguracaoMembro; }
export interface ConviteEquipe { id: string; nome: string; email: string; perfil: PerfilEquipe; status: 'PENDENTE' | 'EXPIRADO'; expiraEm: string; }
export interface Equipe { membros: MembroEquipe[]; convites: ConviteEquipe[]; }
export function configuracaoMembroInicial(): ConfiguracaoMembro { return { nomeExibicao: '', telefone: '', admissao: null, nascimento: null, perfil: 'ASSISTENTE', situacao: 'ATIVO', restrito: false, abas: [], enviaDocumento: false }; }
@Injectable({ providedIn: 'root' })
export class EquipeApiService {
  private readonly http = inject(HttpClient);
  private readonly url = inject(API_URL).replace(/\/$/, '') + '/api/equipe';
  carregar() { return this.http.get<Equipe>(this.url); }
  convidar(nome: string, email: string, configuracao: ConfiguracaoMembro) { return this.http.post<ConviteEquipe>(this.url + '/convites', { nome, email, configuracao }); }
  salvar(membro: MembroEquipe) { return this.http.put<MembroEquipe>(this.url + '/membros/' + membro.id, membro.configuracao); }
  cancelar(id: string) { return this.http.delete<void>(this.url + '/convites/' + id); }
  reenviar(id: string) { return this.http.post<ConviteEquipe>(this.url + '/convites/' + id + '/reenviar', {}); }
}
