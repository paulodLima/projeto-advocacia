import { Component, DestroyRef, inject, output, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { Subscription, finalize, timer } from 'rxjs';
import { LembretesAgendaApiService, LembreteAgenda, PainelLembretes, PreferenciasLembrete } from '../services/lembretes-agenda-api.service';

@Component({selector:'app-agenda-notificacoes',imports:[FormsModule,DatePipe],templateUrl:'./agenda-notificacoes.html',styleUrl:'./agenda-notificacoes.scss'})
export class AgendaNotificacoes {
  readonly abriu=output<void>();
  private readonly api=inject(LembretesAgendaApiService);
  private readonly destroy=inject(DestroyRef);
  private readonly router=inject(Router);
  protected readonly painel=signal<PainelLembretes|null>(null);
  protected readonly carregando=signal(false);
  protected readonly gravando=signal(false);
  protected readonly erro=signal('');
  protected readonly aviso=signal('');
  protected readonly configurando=signal(false);
  protected preferencias:PreferenciasLembrete={ativo:true,antecedenciaMinutos:1440};
  private pedido?:Subscription;
  constructor() {
    // Refresh only while this panel is mounted; do not overwrite preference drafts.
    this.carregar();
    timer(45000,45000).pipe(takeUntilDestroyed(this.destroy)).subscribe(()=>{
      if(!this.gravando() && !this.configurando()) this.carregar(this.painel()?.pagina ?? 0);
    });
  }
  protected carregar(pagina=0) {
    if(this.gravando()) return;
    this.pedido?.unsubscribe();this.carregando.set(true);this.erro.set('');
    this.pedido=this.api.listar(pagina).pipe(finalize(()=>this.carregando.set(false)),takeUntilDestroyed(this.destroy)).subscribe({
      next:p=>{this.painel.set(p);if(!this.configurando()) this.preferencias={...p.preferencias};},
      error:()=>{this.painel.set(null);this.erro.set('Não foi possível carregar os lembretes da Agenda.');}
    });
  }
  protected configurar() {
    if(this.gravando()) return;
    this.configurando.set(!this.configurando());
    this.preferencias={...(this.painel()?.preferencias ?? {ativo:true,antecedenciaMinutos:1440})};
  }
  protected salvarPreferencias() {
    if(this.gravando() || !this.painel()) return;
    this.pedido?.unsubscribe();this.gravando.set(true);this.erro.set('');this.aviso.set('');
    this.api.preferencias({...this.preferencias}).pipe(finalize(()=>this.gravando.set(false)),takeUntilDestroyed(this.destroy)).subscribe({
      next:p=>{this.preferencias={...p};this.configurando.set(false);this.aviso.set('Preferências salvas.');this.api.notificarAlteracao();},
      error:()=>this.erro.set('Não foi possível salvar. Suas escolhas foram preservadas.'),
      complete:()=>{this.gravando.set(false);this.carregar();}
    });
  }
  protected ler(l:LembreteAgenda,abrir=false) {
    if(this.gravando()) return;
    this.pedido?.unsubscribe();this.gravando.set(true);this.erro.set('');this.aviso.set('');
    this.api.ler(l).pipe(finalize(()=>this.gravando.set(false)),takeUntilDestroyed(this.destroy)).subscribe({
      next:()=>{
        this.painel.update(p=>p?{...p,itens:p.itens.filter(i=>!(i.atividadeId===l.atividadeId && i.marco===l.marco)),total:Math.max(0,p.total-1)}:p);
        this.aviso.set('Aviso marcado como visto.');this.api.notificarAlteracao();
        if(abrir) {
          this.router.navigate(['/agenda'],{queryParams:{atividade:l.atividadeId}}).then(ok=>{if(ok)this.abriu.emit();else this.erro.set('Não foi possível abrir a atividade. Acesse a Agenda para consultar.');}).catch(()=>this.erro.set('Não foi possível abrir a atividade. Acesse a Agenda para consultar.'));
        }
      },
      error:e=>this.erro.set(e.error?.detail || 'Não foi possível marcar como visto. Atualize o painel e tente novamente.'),
      complete:()=>{this.gravando.set(false);if(!abrir)this.carregar();}
    });
  }
}
