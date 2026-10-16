import { AfterViewInit, Component, DestroyRef, ElementRef, ViewChild, computed, effect, inject, signal, untracked } from '@angular/core';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subscription, finalize, forkJoin, of } from 'rxjs';
import { AtividadeForm } from '../components/atividade-form';
import { CalendarioMensal } from '../components/calendario-mensal';
import { AgendaApiService } from '../services/agenda-api.service';
import { AtividadeAgenda, DiaAgenda, FiltroAgenda, OpcoesAgenda, PaginaAgenda, SITUACOES_AGENDA, TIPOS_AGENDA, atividadeAtrasada, dataCivil, hojeEscritorio, nomeSituacao, nomeTipo } from '../models/agenda.model';

@Component({selector:'app-agenda',imports:[FormsModule,DatePipe,AtividadeForm,CalendarioMensal],templateUrl:'./agenda.html',styleUrl:'./agenda.scss'})
export class Agenda implements AfterViewInit {
  private readonly route=inject(ActivatedRoute);
  private readonly router=inject(Router);
  private readonly parametros=toSignal(this.route.queryParamMap,{initialValue:convertToParamMap({})});
  private readonly pronta=signal(false);
  private ultimoLink='';
  private readonly api=inject(AgendaApiService);
  private readonly destroyRef=inject(DestroyRef);
  @ViewChild('editor') private editor?:ElementRef<HTMLDialogElement>;
  @ViewChild(AtividadeForm) private formulario?:AtividadeForm;
  protected readonly opcoes=signal<OpcoesAgenda|null>(null);
  protected readonly resultado=signal<PaginaAgenda|null>(null);
  protected readonly calendario=signal<DiaAgenda[]>([]);
  protected readonly atividade=signal<AtividadeAgenda|null>(null);
  protected readonly aberto=signal(false);
  protected readonly carregando=signal(false);
  protected readonly abrindo=signal(false);
  protected readonly erro=signal('');
  protected readonly erroOpcoes=signal('');
  protected readonly aviso=signal('');
  protected readonly visual=signal<'calendario'|'lista'>('calendario');
  protected readonly mes=signal(hojeEscritorio().slice(0,7)+'-01');
  protected readonly diaSelecionado=signal('');
  protected readonly hoje=hojeEscritorio();
  protected readonly tipos=TIPOS_AGENDA;
  protected readonly situacoes=SITUACOES_AGENDA;
  protected readonly nomeTipo=nomeTipo;
  protected readonly nomeSituacao=nomeSituacao;
  protected readonly atrasada=atividadeAtrasada;
  protected inicio=this.mes();
  protected fim=dataCivil(new Date(Number(this.mes().slice(0,4)),Number(this.mes().slice(5,7)),0,12));
  protected tipo='';protected situacao='PENDENTE';protected responsavel='';protected busca='';protected incluirSemData=true;
  protected pagina=0;
  private pedido?:Subscription;
  protected readonly tituloMes=computed(()=>new Date(this.mes()+'T12:00:00').toLocaleDateString('pt-BR',{month:'long',year:'numeric'}));
  protected readonly dias=computed(()=>{
    const primeiro=new Date(this.mes()+'T12:00:00');primeiro.setDate(1-((primeiro.getDay()+6)%7));
    const resumos=new Map(this.calendario().map(d=>[d.data,d]));
    return Array.from({length:42},(_,i)=>{const d=new Date(primeiro);d.setDate(d.getDate()+i);const data=dataCivil(d);return {data,numero:d.getDate(),mesAtual:data.slice(0,7)===this.mes().slice(0,7),...(resumos.get(data) ?? {total:0,pendentes:0,prazosFatais:0})};});
  });
  constructor(){
    this.carregarOpcoes();this.carregar();
    effect(()=>{
      const id=this.parametros().get('atividade');
      if(!id){this.ultimoLink='';return;}
      if(!this.opcoes() || !this.pronta() || id===this.ultimoLink) return;
      this.ultimoLink=id;
      if(!/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(id)) {
        this.erro.set('O link da atividade é inválido.');return;
      }
      untracked(()=>{if(!this.aberto())this.abrir(id);});
    });
  }
  ngAfterViewInit(){this.pronta.set(true);}
  private limparLink(){
    if(this.route.snapshot.queryParamMap.has('atividade'))
      void this.router.navigate([],{relativeTo:this.route,queryParams:{atividade:null},queryParamsHandling:'merge',replaceUrl:true});
  }
  protected carregarOpcoes(){
    this.erroOpcoes.set('');
    this.api.opcoes().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({next:o=>this.opcoes.set(o),error:()=>this.erroOpcoes.set('Não foi possível carregar responsáveis e permissões. Tente novamente.')});
  }
  private filtro():FiltroAgenda {
    const dia=this.diaSelecionado();
    return {inicio:dia || this.inicio,fim:dia || this.fim,tipo:this.tipo,situacao:this.situacao,responsavel:this.responsavel,busca:this.busca.trim(),incluirSemData:!dia && this.incluirSemData};
  }
  protected carregar(pagina=0){
    this.pedido?.unsubscribe();this.carregando.set(true);this.erro.set('');this.resultado.set(null);this.calendario.set([]);this.pagina=pagina;
    const f=this.filtro(),dias=this.dias();
    const calendario=this.visual()==='calendario' ? this.api.calendario({...f,inicio:dias[0].data,fim:dias[41].data,incluirSemData:false}) : of([] as DiaAgenda[]);
    this.pedido=forkJoin({lista:this.api.listar(f,pagina),calendario}).pipe(finalize(()=>this.carregando.set(false)),takeUntilDestroyed(this.destroyRef)).subscribe({next:r=>{this.resultado.set(r.lista);this.calendario.set(r.calendario);},error:e=>this.erro.set(e.error?.detail || 'Não foi possível carregar a Agenda. Tente novamente.')});
  }
  protected filtrar(){this.diaSelecionado.set('');this.carregar();}
  protected mudarVisual(visual:'calendario'|'lista'){
    this.visual.set(visual);this.diaSelecionado.set('');
    if(visual==='calendario'){const d=new Date(this.mes()+'T12:00:00');this.inicio=this.mes();this.fim=dataCivil(new Date(d.getFullYear(),d.getMonth()+1,0,12));}
    this.carregar();
  }
  protected navegarMes(delta:number){const d=new Date(this.mes()+'T12:00:00');d.setMonth(d.getMonth()+delta);this.mes.set(dataCivil(d));this.inicio=this.mes();this.fim=dataCivil(new Date(d.getFullYear(),d.getMonth()+1,0,12));this.diaSelecionado.set('');this.carregar();}
  protected irHoje(){this.mes.set(hojeEscritorio().slice(0,7)+'-01');this.navegarMes(0);}
  protected selecionarDia(dia:string){this.diaSelecionado.set(this.diaSelecionado()===dia?'':dia);this.carregar();}
  protected responsaveis(a:AtividadeAgenda){return a.dados.responsaveis.map(id=>this.opcoes()?.responsaveis.find(p=>p.id===id)?.nome ?? 'Usuário removido').join(', ') || 'Sem responsável';}
  protected nova(){if(!this.opcoes()?.podeEditar)return;this.atividade.set(null);this.abrirEditor();}
  protected abrir(id:string){
    if(this.abrindo())return;this.abrindo.set(true);this.erro.set('');
    this.api.buscar(id).pipe(finalize(()=>this.abrindo.set(false)),takeUntilDestroyed(this.destroyRef)).subscribe({next:a=>{this.atividade.set(a);this.abrirEditor();},error:e=>{this.erro.set(e.error?.detail || 'Não foi possível abrir a atividade.');this.limparLink();}});
  }
  private abrirEditor(){this.aberto.set(true);this.editor?.nativeElement.showModal();}
  protected fechar(){if(this.formulario?.salvando())return;this.aberto.set(false);this.editor?.nativeElement.close();this.atividade.set(null);this.limparLink();}
  protected cancelarDialogo(event:Event){event.preventDefault();this.fechar();}
  protected salvo(a:AtividadeAgenda){this.aberto.set(false);this.editor?.nativeElement.close();this.atividade.set(null);this.aviso.set('Atividade salva. '+nomeSituacao(a.situacao)+'.');this.limparLink();this.carregar();}
  protected dataNova(){return this.diaSelecionado() || hojeEscritorio();}
}
