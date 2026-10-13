import { Component, DestroyRef, effect, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import { AgendaApiService } from '../services/agenda-api.service';
import { AtividadeAgenda, DadosAtividade, OpcoesAgenda, SituacaoAgenda, TIPOS_AGENDA, nomeSituacao } from '../models/agenda.model';

@Component({ selector:'app-atividade-form', imports:[FormsModule,DatePipe], templateUrl:'./atividade-form.html',styleUrl:'./atividade-form.scss' })
export class AtividadeForm {
  readonly base = input<AtividadeAgenda | null>(null);
  readonly opcoes = input.required<OpcoesAgenda>();
  readonly dataInicial = input('');
  readonly salvou = output<AtividadeAgenda>();
  readonly recarregou = output<AtividadeAgenda>();
  readonly fechar = output<void>();
  private readonly api = inject(AgendaApiService);
  private readonly destroyRef = inject(DestroyRef);
  readonly salvando = signal(false);
  protected readonly erro = signal('');
  protected readonly conflito = signal(false);
  protected readonly acao = signal<SituacaoAgenda | null>(null);
  protected observacao = '';
  protected readonly tipos = TIPOS_AGENDA;
  protected readonly nomeSituacao = nomeSituacao;
  protected dados: DadosAtividade = this.vazio();
  constructor() {
    effect(() => { const base=this.base(); this.dados=base ? { ...base.dados,responsaveis:[...base.dados.responsaveis] } : { ...this.vazio(),data:this.dataInicial() || null }; this.erro.set('');this.conflito.set(false);this.acao.set(null);this.observacao=''; });
  }
  private vazio(): DadosAtividade { return { tipo:'TAREFA',titulo:'',tipoTarefaId:null,data:null,dataFim:null,hora:null,horaFim:null,prazoFatal:null,diaInteiro:true,modalidade:null,local:'',descricao:'',responsaveis:[] }; }
  protected podeEditar() { return this.opcoes().podeEditar && (!this.base() || this.base()?.situacao === 'PENDENTE'); }
  protected compromisso() { return ['REUNIAO','AUDIENCIA'].includes(this.dados.tipo); }
  protected temAlteracoes() { return !!this.base() && JSON.stringify(this.dados) !== JSON.stringify(this.base()?.dados); }
  protected trocarTipo() {
    if(this.compromisso()) { this.dados.modalidade='PRESENCIAL';this.dados.prazoFatal=null; }
    else this.dados.modalidade=null;
    if(this.dados.tipo==='PRAZO' && !this.dados.prazoFatal) this.dados.prazoFatal=this.dados.data;
  }
  protected alternarResponsavel(id:string) { this.dados.responsaveis=this.dados.responsaveis.includes(id) ? this.dados.responsaveis.filter(r=>r!==id) : [...this.dados.responsaveis,id]; }
  protected salvar() {
    if(this.salvando() || !this.podeEditar() || !this.dados.titulo.trim() && !this.dados.tipoTarefaId) return;
    const d=this.dados;
    const dados={...d,titulo:d.titulo.trim(),tipoTarefaId:d.tipoTarefaId || null,data:d.data || null,dataFim:d.dataFim || null,
      hora:d.diaInteiro?null:d.hora || null,horaFim:d.diaInteiro?null:d.horaFim || null,prazoFatal:d.prazoFatal || null,modalidade:this.compromisso()?d.modalidade:null};
    this.iniciar();
    this.api.salvar(dados,this.base()).pipe(finalize(()=>this.salvando.set(false)),takeUntilDestroyed(this.destroyRef)).subscribe({next:a=>this.salvou.emit(a),error:e=>this.falha(e)});
  }
  protected confirmarAcao(situacao:SituacaoAgenda) {this.acao.set(situacao);this.observacao='';this.erro.set('');}
  protected executarAcao() {
    const base=this.base(),acao=this.acao();
    if(!base || !acao || this.salvando() || this.temAlteracoes() || !this.opcoes().podeEditar || acao==='CANCELADA' && !this.observacao.trim()) return;
    this.iniciar();
    this.api.situacao(base,acao,this.observacao.trim()).pipe(finalize(()=>this.salvando.set(false)),takeUntilDestroyed(this.destroyRef)).subscribe({next:a=>this.salvou.emit(a),error:e=>this.falha(e)});
  }
  protected recarregar() {
    const base=this.base();if(!base || this.salvando()) return;
    this.salvando.set(true);
    this.api.buscar(base.id).pipe(finalize(()=>this.salvando.set(false)),takeUntilDestroyed(this.destroyRef)).subscribe({next:a=>this.recarregou.emit(a),error:e=>this.falha(e)});
  }
  protected nomeAcao(acao:string) {return ({CRIADA:'Criada',EDITADA:'Editada',CONCLUIDA:'Concluída',CANCELADA:'Cancelada',REABERTA:'Reaberta'} as Record<string,string>)[acao] ?? acao;}
  private iniciar() {this.salvando.set(true);this.erro.set('');this.conflito.set(false);}
  private falha(e:{status?:number;error?:{detail?:string;erros?:string[]}}) {this.conflito.set(e.status===409);this.erro.set(e.error?.detail || e.error?.erros?.join(' · ') || 'Não foi possível salvar. Seu rascunho foi preservado.');}
}
