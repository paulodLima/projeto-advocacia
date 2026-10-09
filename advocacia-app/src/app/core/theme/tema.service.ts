import { computed, effect, inject, Injectable, signal, untracked } from '@angular/core';
import { catchError, concatMap, EMPTY, map, Subject, Subscription } from 'rxjs';
import { AuthService } from '../auth/auth.service';
import { TemaId, temaValido } from './tema.model';
import { PreferenciaTema, TemaApiService } from './tema-api.service';

@Injectable({providedIn:'root'})
export class TemaService {
  private readonly auth=inject(AuthService);
  private readonly sessaoId=computed(()=>this.auth.usuario()?.id ?? null);
  private readonly api=inject(TemaApiService);
  private readonly estado=signal<TemaId>('verde');
  readonly atual=this.estado.asReadonly();
  readonly carregando=signal(false); readonly salvando=signal(false);
  readonly pronto=signal(false); readonly aviso=signal(''); readonly erro=signal(false);
  private usuarioId:string | null=null;
  private sequencia=0;
  private fila:Subject<{tema:TemaId; sequencia:number}> | null=null;
  private carga:Subscription | null=null;

  constructor() {
    effect(onCleanup=>{
      const id=this.sessaoId();
      this.usuarioId=id; this.sequencia=0; this.estado.set('verde');
      this.pronto.set(false); this.carregando.set(false); this.salvando.set(false); this.aviso.set(''); this.erro.set(false);
      const fila=new Subject<{tema:TemaId; sequencia:number}>(); this.fila=fila;
      const envio=fila.pipe(concatMap(escolha=>this.api.salvar(escolha.tema).pipe(
        map(d=>({d,escolha})),
        catchError(e=>{ if(this.usuarioId===id && escolha.sequencia===this.sequencia) this.falha(e,false); return EMPTY; }),
      ))).subscribe(({d,escolha})=>{
        // A fila preserva a ordem das escritas, inclusive ao selecionar A, B e A novamente.
        if(this.usuarioId!==id || escolha.sequencia!==this.sequencia) return;
        if(!this.valido(d,id) || d.tema!==escolha.tema) { this.falha(null,false); return; }
        this.salvando.set(false); this.erro.set(false); this.aviso.set('Tema salvo na sua conta.');
      });
      if(id) untracked(()=>this.carregar());
      onCleanup(()=>{ this.carga?.unsubscribe(); envio.unsubscribe(); fila.complete(); });
    });
  }
  private valido(d:PreferenciaTema, id:string | null) { return d.usuarioId===id && temaValido(d.tema); }
  private falha(e:{error?:{detail?:string}} | null, carga:boolean) {
    this.erro.set(true); this.aviso.set(e?.error?.detail || (carga ? 'Não foi possível carregar seu tema. Tente novamente.' : 'O tema foi aplicado nesta tela, mas não foi salvo. Tente novamente.'));
    this.carregando.set(false); this.salvando.set(false);
  }
  carregar() {
    const id=this.usuarioId;
    if(!id || this.carregando() || this.salvando()) return;
    this.pronto.set(false); this.carregando.set(true); this.aviso.set('');
    this.carga?.unsubscribe();
    this.carga=this.api.carregar().subscribe({next:d=>{
      if(this.usuarioId!==id) return;
      if(!this.valido(d,id)) { this.falha(null,true); return; }
      this.estado.set(d.tema); this.pronto.set(true); this.carregando.set(false); this.erro.set(false);
      this.aviso.set('Sua preferência é salva na conta e acompanha seus acessos.');
    },error:e=>{if(this.usuarioId===id) this.falha(e,true);}});
  }
  selecionar(tema:unknown) {
    if(!temaValido(tema) || !this.pronto() || this.auth.usuario()?.id!==this.usuarioId) return;
    this.estado.set(tema); this.erro.set(false); this.aviso.set(''); this.salvando.set(true);
    this.fila?.next({tema,sequencia:++this.sequencia});
  }
  tentarNovamente() { if(this.pronto()) this.selecionar(this.atual()); else this.carregar(); }
}
