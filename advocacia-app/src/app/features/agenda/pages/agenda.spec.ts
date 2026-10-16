import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, ParamMap, convertToParamMap, provideRouter } from '@angular/router';
import { TestBed } from '@angular/core/testing';
import { API_URL } from '../../../core/config/api-url.token';
import { BehaviorSubject } from 'rxjs';
import { Agenda } from './agenda';

describe('Agenda calendário e lista',()=>{
  let http:HttpTestingController;
  let parametros:BehaviorSubject<ParamMap>;
  const url='/api/agenda';
  beforeEach(()=>{parametros=new BehaviorSubject(convertToParamMap({}));TestBed.configureTestingModule({imports:[Agenda],providers:[provideRouter([]),{provide:ActivatedRoute,useValue:{queryParamMap:parametros.asObservable(),snapshot:{get queryParamMap(){return parametros.value;}}}},provideHttpClient(),provideHttpClientTesting(),{provide:API_URL,useValue:''}]});http=TestBed.inject(HttpTestingController);});
  afterEach(()=>{http.verify();TestBed.resetTestingModule();});
  function inicial(){const f=TestBed.createComponent(Agenda);http.expectOne(url+'/opcoes').flush({podeEditar:true,responsaveis:[],tiposTarefa:[],fusoHorario:'America/Sao_Paulo'});http.expectOne(r=>r.url===url).flush({itens:[],total:0,pagina:0,tamanho:50});http.expectOne(r=>r.url===url+'/calendario').flush([]);return f;}
  it('consulta dia selecionado sem tarefas sem data e mantém calendário completo',async()=>{
    const f=inicial();await f.whenStable();const dia=f.nativeElement.querySelector('.dia') as HTMLButtonElement;const data=dia.getAttribute('aria-label')!.slice(0,10);dia.click();
    const lista=http.expectOne(r=>r.url===url);expect(lista.request.params.get('inicio')).toBe(data);expect(lista.request.params.get('fim')).toBe(data);expect(lista.request.params.get('incluirSemData')).toBe('false');lista.flush({itens:[],total:0,pagina:0,tamanho:50});
    const calendario=http.expectOne(r=>r.url===url+'/calendario');expect(calendario.request.params.get('inicio')).not.toBe(calendario.request.params.get('fim'));calendario.flush([]);await f.whenStable();expect(f.nativeElement.textContent).toContain('Atividades do dia');
  });
  it('cancela consulta antiga e permite listar período com filtros sem resposta obsoleta',async()=>{
    const f=inicial();await f.whenStable();const c=f.componentInstance as any;c.busca='Primeira';c.mudarVisual('lista');const antiga=http.expectOne(r=>r.url===url);c.busca='Segunda';c.filtrar();expect(antiga.cancelled).toBe(true);
    const atual=http.expectOne(r=>r.url===url);expect(atual.request.params.get('busca')).toBe('Segunda');atual.flush({itens:[],total:0,pagina:0,tamanho:50});http.expectNone(r=>r.url===url+'/calendario');await f.whenStable();expect(f.nativeElement.textContent).toContain('Nenhuma atividade');
  });

  it('abre link de notificação fora do filtro atual após carregar permissões',async()=>{
    const id='00000000-0000-0000-0000-000000000099';
    parametros.next(convertToParamMap({atividade:id}));
    const f=inicial();f.detectChanges();
    const dialog=f.nativeElement.querySelector('dialog') as HTMLDialogElement;
    Object.defineProperty(dialog,'showModal',{value:()=>dialog.setAttribute('open','')});
    await f.whenStable();
    const r=http.expectOne(url+'/'+id);
    r.flush({id,versao:0,dados:{tipo:'TAREFA',titulo:'Atividade antiga',tipoTarefaId:null,data:'2020-01-10',dataFim:null,hora:null,horaFim:null,prazoFatal:null,diaInteiro:true,modalidade:null,local:'',descricao:'',responsaveis:[],participantes:[],repeticao:'NUNCA'},situacao:'PENDENTE',concluidaEm:null,criadoEm:'2020-01-01T12:00:00Z',atualizadoEm:'2020-01-01T12:00:00Z',historico:[]});
    await f.whenStable();expect(dialog.hasAttribute('open')).toBe(true);expect(dialog.textContent).toContain('Atividade antiga');
  });
  it('link inválido não consulta outra rota de API e mostra erro',async()=>{
    parametros.next(convertToParamMap({atividade:'../../outra-rota'}));
    const f=inicial();await f.whenStable();http.expectNone(r=>r.url.includes('outra-rota'));
    expect(f.nativeElement.textContent).toContain('O link da atividade é inválido');
  });
});
