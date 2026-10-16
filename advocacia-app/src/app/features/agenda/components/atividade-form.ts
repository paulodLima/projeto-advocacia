import { Component, DestroyRef, effect, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize, Subscription } from 'rxjs';
import { AgendaApiService } from '../services/agenda-api.service';
import { AtividadeAgenda, DadosAtividade, OpcoesAgenda, OpcaoAgenda, SituacaoAgenda, TIPOS_AGENDA, REPETICOES_AGENDA, nomeSituacao } from '../models/agenda.model';

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
  protected buscaContato = '';
  protected readonly contatos = signal<OpcaoAgenda[]>([]);
  protected readonly selecionados = signal<OpcaoAgenda[]>([]);
  protected readonly buscandoContatos = signal(false);
  protected readonly erroContatos = signal('');
  private consultaContatos?: Subscription;
  protected observacao = '';
  protected readonly repeticoes = REPETICOES_AGENDA;
  protected readonly tipos = TIPOS_AGENDA;
  protected readonly nomeSituacao = nomeSituacao;
  protected dados: DadosAtividade = this.vazio();
  constructor() {
    effect(() => { const base=this.base(); this.dados=base ? { ...base.dados,responsaveis:[...base.dados.responsaveis],participantes:[...(base.dados.participantes ?? [])],repeticao:base.dados.repeticao ?? 'NUNCA' } : { ...this.vazio(),data:this.dataInicial() || null }; this.erro.set('');this.conflito.set(false);this.acao.set(null);this.observacao='';
      this.selecionados.set([]);this.contatos.set([]);this.erroContatos.set('');
      this.consultaContatos?.unsubscribe();
      const ids=this.dados.participantes ?? [];
      if(this.opcoes().podeVincularContatos && ids.length)
        this.consultaContatos=this.api.contatos('',ids).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
          next:pessoas=>this.selecionados.set(pessoas),error:()=>this.erroContatos.set('Não foi possível carregar os participantes. Tente buscar novamente.')
        });
    });
  }
  private vazio(): DadosAtividade { return { tipo:'TAREFA',titulo:'',tipoTarefaId:null,data:null,dataFim:null,hora:null,horaFim:null,prazoFatal:null,diaInteiro:true,modalidade:null,local:'',descricao:'',responsaveis:[],participantes:[],repeticao:'NUNCA' }; }
  protected podeEditar() { return this.opcoes().podeEditar && (!this.base() || this.base()?.situacao === 'PENDENTE'); }
  protected compromisso() { return ['REUNIAO','AUDIENCIA'].includes(this.dados.tipo); }
  protected temAlteracoes() { return !!this.base() && JSON.stringify(this.dados) !== JSON.stringify({...this.base()?.dados,participantes:this.base()?.dados.participantes ?? [],repeticao:this.base()?.dados.repeticao ?? 'NUNCA'}); }
  protected trocarTipo() {
    if(this.dados.tipo!=='TAREFA') this.dados.repeticao='NUNCA';
    if(this.compromisso()) { this.dados.modalidade='PRESENCIAL';this.dados.prazoFatal=null; }
    else this.dados.modalidade=null;
    if(this.dados.tipo==='PRAZO' && !this.dados.prazoFatal) this.dados.prazoFatal=this.dados.data;
  }
  protected nomeContato(id:string) { return this.selecionados().find(p=>p.id===id)?.nome ?? this.contatos().find(p=>p.id===id)?.nome ?? "Contato vinculado"; }
  protected buscarContatos() {
    if(!this.opcoes().podeVincularContatos) return;
    this.consultaContatos?.unsubscribe();this.buscandoContatos.set(true);this.erroContatos.set('');
    this.consultaContatos=this.api.contatos(this.buscaContato.trim()).pipe(finalize(()=>this.buscandoContatos.set(false)),takeUntilDestroyed(this.destroyRef)).subscribe({
      next:pessoas=>this.contatos.set(pessoas),error:()=>this.erroContatos.set('Não foi possível buscar contatos. Tente novamente.')
    });
  }
  protected adicionarContato(pessoa:OpcaoAgenda) {
    if(!this.podeEditar() || !this.opcoes().podeVincularContatos) return;
    const ids=this.dados.participantes ?? [];
    if(ids.includes(pessoa.id) || ids.length>=50) return;
    this.dados.participantes=[...ids,pessoa.id];this.selecionados.update(lista=>[...lista,pessoa]);
  }
  protected removerContato(id:string) {
    if(!this.podeEditar() || !this.opcoes().podeVincularContatos) return;
    this.dados.participantes=(this.dados.participantes ?? []).filter(p=>p!==id);
    this.selecionados.update(lista=>lista.filter(p=>p.id!==id));
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
  protected nomeAcao(acao:string) {return ({CRIADA:'Criada',EDITADA:'Editada',CONCLUIDA:'Concluída',CANCELADA:'Cancelada',REABERTA:'Reaberta',RECORRENCIA_GERADA:'Próxima tarefa gerada'} as Record<string,string>)[acao] ?? acao;}
  private iniciar() {this.salvando.set(true);this.erro.set('');this.conflito.set(false);}
  private falha(e:{status?:number;error?:{detail?:string;erros?:string[]}}) {this.conflito.set(e.status===409);this.erro.set(e.error?.detail || e.error?.erros?.join(' · ') || 'Não foi possível salvar. Seu rascunho foi preservado.');}
}
