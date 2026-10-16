import { Component, DestroyRef, effect, inject, input, output, signal, untracked } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subscription, finalize } from 'rxjs';
import { ExecucaoWorkflow, OpcaoWorkflow, PlanejarWorkflow, PlanoWorkflow, WorkflowsAgendaApiService } from '../services/workflows-agenda-api.service';
@Component({selector:'app-workflow-execucao',imports:[FormsModule,DatePipe],templateUrl:'./workflow-execucao.html',styleUrl:'./workflow-execucao.scss'})
export class WorkflowExecucao {
  readonly tipoTarefaId=input.required<string>();
  readonly prazoFatal=input<string|null>(null);
  readonly usando=output<boolean>();
  readonly ocupado=output<boolean>();
  readonly gerou=output<ExecucaoWorkflow>();
  private readonly api=inject(WorkflowsAgendaApiService);
  private readonly destroy=inject(DestroyRef);
  protected readonly modelos=signal<OpcaoWorkflow[]>([]);
  protected readonly carregando=signal(false);
  protected readonly processando=signal(false);
  protected readonly ativo=signal(false);
  protected readonly plano=signal<PlanoWorkflow|null>(null);
  protected readonly erro=signal('');
  protected escolhido='';
  protected diasSemExpediente='';
  private modelosPedido?:Subscription;
  private pedido?:Subscription;
  private requisicao='';
  private comando?:PlanejarWorkflow;
  constructor(){
    effect(onCleanup=>{
      const tipo=this.tipoTarefaId();
      untracked(()=>{this.ativo.set(false);this.usando.emit(false);this.invalidar();this.modelos.set([]);this.escolhido='';this.carregarModelos(tipo);});
      onCleanup(()=>{this.modelosPedido?.unsubscribe();this.pedido?.unsubscribe();});
    });
    effect(()=>{this.prazoFatal();untracked(()=>this.invalidar());});
  }
  protected carregarModelos(tipo=this.tipoTarefaId()){
    this.modelosPedido?.unsubscribe();this.invalidar();this.carregando.set(true);
    this.modelosPedido=this.api.modelos(tipo).pipe(finalize(()=>this.carregando.set(false)),takeUntilDestroyed(this.destroy)).subscribe({
      next:lista=>{this.modelos.set(lista);if(!lista.some(m=>m.id===this.escolhido))this.escolhido=lista.length===1?lista[0].id:'';},
      error:e=>this.falha(e)
    });
  }
  protected alternar(){
    if(this.processando())return;
    this.ativo.set(!this.ativo());this.usando.emit(this.ativo());this.invalidar();
  }
  protected invalidar(){this.pedido?.unsubscribe();this.plano.set(null);this.requisicao='';this.comando=undefined;this.erro.set('');}
  private iniciar(){this.processando.set(true);this.ocupado.emit(true);this.erro.set('');}
  private terminar(){this.processando.set(false);this.ocupado.emit(false);}
  protected prever(){
    if(this.processando() || !this.ativo())return;
    const modelo=this.modelos().find(m=>m.id===this.escolhido),prazo=this.prazoFatal();
    if(!modelo || !prazo){this.erro.set('Escolha um workflow e informe o prazo fatal manual para conferir as etapas.');return;}
    const dias=this.diasSemExpediente.split(/[\s,;]+/).filter(Boolean);
    if(dias.length>366 || dias.some(d=>!/^\d{4}-\d{2}-\d{2}$/.test(d))){this.erro.set('Informe até 366 datas no formato AAAA-MM-DD.');return;}
    const c:PlanejarWorkflow={workflowId:modelo.id,versao:modelo.versao,tipoTarefaId:this.tipoTarefaId(),prazoFatal:prazo,diasNaoUteis:dias};
    this.iniciar();this.plano.set(null);
    this.pedido=this.api.prever(c).pipe(finalize(()=>this.terminar()),takeUntilDestroyed(this.destroy)).subscribe({
      next:plano=>{this.plano.set(plano);this.comando=c;this.requisicao=crypto.randomUUID();},error:e=>this.falha(e)
    });
  }
  protected gerar(){
    const plano=this.plano(),c=this.comando;
    if(this.processando() || !this.ativo() || !plano || !c || !this.requisicao)return;
    this.iniciar();
    this.pedido=this.api.gerar(c,plano.assinatura,this.requisicao).pipe(finalize(()=>this.terminar()),takeUntilDestroyed(this.destroy)).subscribe({next:e=>this.gerou.emit(e),error:e=>this.falha(e)});
  }
  private falha(e:{error?:{detail?:string;erros?:string[]}}){this.erro.set(e.error?.detail || e.error?.erros?.join(' · ') || 'Não foi possível concluir. Suas escolhas foram preservadas.');}
}
