import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { API_URL } from '../../../core/config/api-url.token';
import { RegistroLocal, RotinaLocal, SistemaLocal } from '../models/configuracoes.model';
import { map } from 'rxjs';

interface RegistroResposta { id: string; tipo: string; ativo: boolean; campos: Record<string, string>; }
export interface Cadastros { empresaId: string | null; papel: 'MASTER' | 'MEMBRO' | null; listas: Record<string, RegistroLocal[]>; rotinas: RotinaLocal[]; sistemas: SistemaLocal[]; atalhos: string[]; }
export type CadastrosInicio = Pick<Cadastros, 'empresaId' | 'rotinas' | 'sistemas' | 'atalhos'>;
export const registroDoServidor = (item: RegistroResposta): RegistroLocal => ({ ...item.campos, id: item.id, ativo: item.ativo });
@Injectable({ providedIn: 'root' })
export class CadastrosApiService {
  private readonly http = inject(HttpClient);
  private readonly url = inject(API_URL).replace(/\/$/, '') + '/api/config/cadastros';
  inicio() { return this.http.get<CadastrosInicio>(this.url + '/inicio'); }
  salvarAtalhos(atalhos: string[]) { return this.http.put<string[]>(this.url + '/atalhos', { atalhos }); }
  carregar() { return this.http.get<Omit<Cadastros, 'listas'> & { listas: Record<string, RegistroResposta[]> }>(this.url).pipe(map(dados => ({ ...dados, listas: Object.fromEntries(Object.entries(dados.listas).map(([tipo, itens]) => [tipo, itens.map(registroDoServidor)])) }))); }
  criar(tipo: string, campos: Record<string, string>) { return this.http.post<RegistroResposta>(this.url + '/listas/' + tipo, { campos, ativo: true }).pipe(map(registroDoServidor)); }
  salvar(tipo: string, item: RegistroLocal, campos: Record<string, string>) { return this.http.put<RegistroResposta>(this.url + '/listas/' + tipo + '/' + item.id, { campos, ativo: item.ativo }).pipe(map(registroDoServidor)); }
  excluir(tipo: string, id: string) { return this.http.delete<void>(this.url + '/listas/' + tipo + '/' + id); }
  criarRotina(nome: string, periodo: string) { return this.http.post<RotinaLocal>(this.url + '/rotinas', { nome, periodo }); }
  excluirRotina(id: string) { return this.http.delete<void>(this.url + '/rotinas/' + id); }
  salvarSistemas(sistemas: SistemaLocal[]) { return this.http.put<SistemaLocal[]>(this.url + '/sistemas', { sistemas }); }
}
