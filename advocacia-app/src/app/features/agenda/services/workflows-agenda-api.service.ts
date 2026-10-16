import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { API_URL } from '../../../core/config/api-url.token';
export interface OpcaoWorkflow { id:string; nome:string; versao:number; buffer:number; quantidadeEtapas:number; }
export interface EtapaPlanejada { ordem:number; nome:string; responsavel:string|null; responsavelNome:string; dias:number; inicio:string; fim:string; }
export interface PlanejarWorkflow { workflowId:string; versao:number; tipoTarefaId:string; prazoFatal:string; diasNaoUteis:string[]; }
export interface PlanoWorkflow extends PlanejarWorkflow { assinatura:string; nome:string; buffer:number; etapas:EtapaPlanejada[]; }
export interface ExecucaoWorkflow { id:string; plano:PlanoWorkflow; atividades:string[]; }
@Injectable({providedIn:'root'})
export class WorkflowsAgendaApiService {
  private readonly http=inject(HttpClient);
  private readonly url=inject(API_URL).replace(/\/$/,'')+'/api/agenda/workflows';
  modelos(tipo:string){return this.http.get<OpcaoWorkflow[]>(this.url,{params:new HttpParams().set('tipoTarefaId',tipo)});}
  prever(c:PlanejarWorkflow){return this.http.post<PlanoWorkflow>(this.url+'/previsao',c);}
  gerar(c:PlanejarWorkflow,assinaturaPlano:string,requisicaoId:string){return this.http.post<ExecucaoWorkflow>(this.url+'/execucoes',{...c,assinaturaPlano,requisicaoId});}
}
