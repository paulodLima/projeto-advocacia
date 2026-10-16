import { Component, DestroyRef, effect, inject, input, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subscription, finalize } from 'rxjs';
import { API_URL } from '../../../core/config/api-url.token';
import { FiltroAgenda, OpcaoAgenda, SituacaoAgenda, nomeSituacao } from '../models/agenda.model';
export interface VinculoAgenda {
 id:string;leadId:string|null;tarefaId:string|null;casoId:string|null;titulo:string;tipo:string;
 data:string|null;dataFim:string|null;hora:string|null;horaFim:string|null;prazoFatal:string|null;
 diaInteiro:boolean;local:string;situacao:SituacaoAgenda;responsaveis:OpcaoAgenda[];
}
export interface PaginaVinculos {itens:VinculoAgenda[];total:number;pagina:number;tamanho:number;}
@Component({selector:'app-vinculos-agenda',imports:[DatePipe,RouterLink],templateUrl:'./vinculos-agenda.html',styleUrl:'./vinculos-agenda.scss'})
export class VinculosAgenda{
 readonly filtro=input.required<FiltroAgenda>();
 private readonly http=inject(HttpClient);
 private readonly url=inject(API_URL).replace(/\/$/,'')+'/api/agenda/vinculos';
 private readonly destroy=inject(DestroyRef);
 private pedido?:Subscription;
 protected readonly resultado=signal<PaginaVinculos|null>(null);
 protected readonly erro=signal('');
 protected readonly carregando=signal(false);
 protected readonly nomeSituacao=nomeSituacao;
 constructor(){effect(()=>{const filtro=this.filtro();this.carregar(0,filtro);});}
 protected carregar(pagina=0,f=this.filtro()){
  this.pedido?.unsubscribe();this.carregando.set(true);this.erro.set('');this.resultado.set(null);
  let params=new HttpParams().set('inicio',f.inicio).set('fim',f.fim).set('incluirSemData',f.incluirSemData).set('pagina',pagina).set('tamanho',20);
  for(const campo of ['tipo','situacao','responsavel','busca'] as const)if(f[campo])params=params.set(campo,f[campo]);
  this.pedido=this.http.get<PaginaVinculos>(this.url,{params}).pipe(finalize(()=>this.carregando.set(false)),takeUntilDestroyed(this.destroy)).subscribe({
   next:r=>this.resultado.set(r),error:e=>this.erro.set(e.error?.detail || 'Não foi possível carregar consultas e tarefas vinculadas. Tente novamente.')
  });
 }
 protected responsaveis(v:VinculoAgenda){return v.responsaveis.map(r=>r.nome).join(', ') || 'Sem responsável';}
}
