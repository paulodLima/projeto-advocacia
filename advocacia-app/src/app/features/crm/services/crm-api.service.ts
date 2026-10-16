import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { API_URL } from '../../../core/config/api-url.token';
import {
  CadastroLead,
  Campanha,
  EstadoLead,
  Lead,
  Marketing,
  OpcoesCrm,
  PaginaLeads,
} from '../models/crm.model';

export interface FiltrosCrm {
  busca: string;
  status: string;
  temperatura: string;
  origem: string;
  atrasados: boolean;
  pagina: number;
}
export interface NotificacaoCrm {
  id: string;
  lead_id: string;
  texto: string;
  autor: string;
  criado_em: string;
}
@Injectable({ providedIn: 'root' })
export class CrmApiService {
  private readonly http = inject(HttpClient);
  private readonly url = inject(API_URL).replace(/\/$/, '') + '/api/crm';
  opcoes() {
    return this.http.get<OpcoesCrm>(this.url + '/opcoes');
  }
  listar(f: FiltrosCrm) {
    return this.http.get<PaginaLeads>(this.url + '/leads', { params: { ...f } });
  }
  buscar(id: string) {
    return this.http.get<Lead>(this.url + '/leads/' + id);
  }
  salvar(cadastro: CadastroLead, lead?: Lead | null) {
    const f = { cadastro, versao: lead?.versao };
    return lead
      ? this.http.put<Lead>(this.url + '/leads/' + lead.id, f)
      : this.http.post<Lead>(this.url + '/leads', f);
  }
  estado(l: Lead, estado: EstadoLead) {
    return this.http.put<Lead>(this.url + '/leads/' + l.id + '/estado', {
      versao: l.versao,
      estado,
    });
  }
  contato(l: Lead, f: { data: string; canal: string; resultado: string; observacao: string }) {
    return this.http.post<Lead>(this.url + '/leads/' + l.id + '/contatos', {
      ...f,
      versao: l.versao,
    });
  }
  comentar(l: Lead, texto: string, mencoes: string[]) {
    return this.http.post<Lead>(this.url + '/leads/' + l.id + '/comentarios', {
      versao: l.versao,
      texto,
      mencoes,
    });
  }
  converter(l: Lead, criarCaso: boolean) {
    return this.http.post<Lead>(this.url + '/leads/' + l.id + '/converter', {
      versao: l.versao,
      criarCaso,
    });
  }
  excluir(l: Lead) {
    return this.http.delete<void>(this.url + '/leads/' + l.id, { params: { versao: l.versao } });
  }
  marketing(ano: number) {
    return this.http.get<Marketing>(this.url + '/campanhas', { params: { ano } });
  }
  criarCampanha(c: Pick<Campanha, 'nome' | 'mes' | 'cor' | 'descricao' | 'etiquetas'>) {
    return this.http.post<void>(this.url + '/campanhas', c);
  }
  excluirCampanha(c: Campanha) {
    return this.http.delete<void>(this.url + '/campanhas/' + c.id, {
      params: { versao: c.versao },
    });
  }
  abordar(c: Campanha, destinatario: string, ano: number, abordado: boolean) {
    return this.http.put<void>(this.url + '/campanhas/' + c.id + '/abordagens', {
      destinatario,
      ano,
      abordado,
    });
  }
  manutencao(id: string) {
    return this.http.post<void>(this.url + '/manutencoes/' + id + '/contato', {});
  }
  notificacoes() {
    return this.http.get<NotificacaoCrm[]>(this.url + '/notificacoes');
  }
  lerNotificacao(id: string) {
    return this.http.put<void>(this.url + '/notificacoes/' + id + '/lida', {});
  }
}
