import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map } from 'rxjs';
import { API_URL } from '../../../core/config/api-url.token';
import { WorkflowLocal } from '../models/configuracoes.model';
export interface Workflow extends WorkflowLocal { versao: number; }
export interface OpcaoWorkflow { id: string; nome: string; ativo: boolean; }
export interface WorkflowsDados { empresaId: string | null; papel: 'MASTER' | 'MEMBRO' | null; workflows: Workflow[]; tarefas: OpcaoWorkflow[]; responsaveis: OpcaoWorkflow[]; }
const receber = (w: Workflow): Workflow => ({ ...w, etapas: w.etapas.map(e => ({ ...e, responsavel: e.responsavel || '' })) });
@Injectable({ providedIn: 'root' })
export class WorkflowsApiService {
  private readonly http = inject(HttpClient);
  private readonly url = inject(API_URL).replace(/\/$/, '') + '/api/config/workflows';
  carregar() { return this.http.get<WorkflowsDados>(this.url).pipe(map(d => ({ ...d, workflows: d.workflows.map(receber) }))); }
  criar(nome: string) { return this.http.post<Workflow>(this.url, { nome }).pipe(map(receber)); }
  salvar(w: Workflow) { return this.http.put<Workflow>(this.url + '/' + w.id, { nome: w.nome, buffer: w.buffer, versao: w.versao, gatilhos: w.gatilhos, etapas: w.etapas.map(e => ({ ...e, responsavel: e.responsavel || null })) }).pipe(map(receber)); }
  excluir(w: Workflow) { return this.http.delete<void>(this.url + '/' + w.id, { params: { versao: w.versao } }); }
}
