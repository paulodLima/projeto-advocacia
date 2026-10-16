import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { API_URL } from '../../../core/config/api-url.token';
import { Subject } from 'rxjs';
import { TipoAtividade } from '../models/agenda.model';
export type MarcoLembrete = 'ATIVIDADE' | 'PRAZO_FATAL';
export interface PreferenciasLembrete { ativo: boolean; antecedenciaMinutos: number; }
export interface LembreteAgenda { atividadeId: string; versao: number; titulo: string; tipo: TipoAtividade; marco: MarcoLembrete; data: string; hora: string; }
export interface PainelLembretes { preferencias: PreferenciasLembrete; itens: LembreteAgenda[]; total: number; pagina: number; tamanho: number; consultadoEm: string; fusoHorario: string; }
@Injectable({providedIn:'root'})
export class LembretesAgendaApiService {
  private readonly alteracoesSubject=new Subject<void>();
  readonly alteracoes=this.alteracoesSubject.asObservable();
  notificarAlteracao(){this.alteracoesSubject.next();}
  private readonly http=inject(HttpClient);
  private readonly url=inject(API_URL).replace(/\/$/,'')+'/api/agenda/lembretes';
  listar(pagina=0,tamanho=20) {return this.http.get<PainelLembretes>(this.url,{params:new HttpParams().set('pagina',pagina).set('tamanho',tamanho)});}
  preferencias(p:PreferenciasLembrete) {return this.http.put<PreferenciasLembrete>(this.url+'/preferencias',p);}
  ler(l:LembreteAgenda) {return this.http.patch<void>(this.url+'/'+l.atividadeId+'/lido',{versao:l.versao,marco:l.marco});}
}
