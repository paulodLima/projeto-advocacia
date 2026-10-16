import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_URL } from '../../../core/config/api-url.token';
import { AtividadeAgenda, OpcoesAgenda } from '../models/agenda.model';
import { AtividadeForm } from './atividade-form';

describe('Formulário de atividades da Agenda',()=>{
  let http:HttpTestingController;
  const url='/api/agenda';
  const opcoes:OpcoesAgenda={podeEditar:true,responsaveis:[{id:'pessoa',nome:'Responsável',ativo:true}],tiposTarefa:[],fusoHorario:'America/Sao_Paulo'};
  const atividade:AtividadeAgenda={id:'atividade',versao:2,dados:{tipo:'TAREFA',titulo:'Preparar documento',tipoTarefaId:null,data:'2026-10-15',dataFim:null,hora:null,horaFim:null,prazoFatal:null,diaInteiro:true,modalidade:null,local:'',descricao:'',responsaveis:['pessoa']},situacao:'PENDENTE',concluidaEm:null,criadoEm:'2026-10-12T12:00:00Z',atualizadoEm:'2026-10-12T12:00:00Z',historico:[]};
  beforeEach(()=>{TestBed.configureTestingModule({imports:[AtividadeForm],providers:[provideHttpClient(),provideHttpClientTesting(),{provide:API_URL,useValue:''}]});http=TestBed.inject(HttpTestingController);});
  afterEach(()=>{http.verify();TestBed.resetTestingModule();});
  async function criar(base:AtividadeAgenda|null=null,podeEditar=true){const fixture=TestBed.createComponent(AtividadeForm);fixture.componentRef.setInput('opcoes',{...opcoes,podeEditar});fixture.componentRef.setInput('base',base);fixture.componentRef.setInput('dataInicial','2026-10-15');await fixture.whenStable();return fixture;}
  it('salva atividade com dados reais do formulário e só informa sucesso após resposta',async()=>{
    const f=await criar();let salvo:AtividadeAgenda|undefined;f.componentInstance.salvou.subscribe(a=>salvo=a);
    const titulo=f.nativeElement.querySelector('[name=titulo]') as HTMLInputElement;titulo.value='Preparar documento';titulo.dispatchEvent(new Event('input'));
    f.nativeElement.querySelector('[name=pessoa-pessoa]').click();await f.whenStable();
    f.nativeElement.querySelector('form').dispatchEvent(new Event('submit',{cancelable:true}));const r=http.expectOne(url);
    expect(r.request.method).toBe('POST');expect(r.request.body.titulo).toBe('Preparar documento');expect(r.request.body.responsaveis).toEqual(['pessoa']);expect(r.request.body.versao).toBe(0);expect(salvo).toBeUndefined();
    r.flush(atividade);await f.whenStable();expect(salvo?.id).toBe('atividade');
  });
  it('conflito conserva rascunho e recarregamento consulta versão atual sem emitir gravação',async()=>{
    const f=await criar(atividade);const c=f.componentInstance as any;c.dados.titulo='Meu rascunho';c.salvar();
    http.expectOne(url+'/atividade').flush({detail:'Recarregue a ficha'},{status:409,statusText:'Conflict'});await f.whenStable();expect(c.dados.titulo).toBe('Meu rascunho');expect(c.base().versao).toBe(2);
    let atualizado:AtividadeAgenda|undefined;let gravado=false;f.componentInstance.recarregou.subscribe(a=>atualizado=a);f.componentInstance.salvou.subscribe(()=>gravado=true);
    f.nativeElement.querySelectorAll('button').forEach((b:HTMLButtonElement)=>{if(b.textContent?.trim()==='Recarregar ficha')b.click();});
    const r=http.expectOne(url+'/atividade');expect(r.request.method).toBe('GET');r.flush({...atividade,versao:3});await f.whenStable();expect(atualizado?.versao).toBe(3);expect(gravado).toBe(false);
  });
  it('cancelamento exige motivo e envia situação e versão somente após confirmação',async()=>{
    const f=await criar(atividade);const c=f.componentInstance as any;c.confirmarAcao('CANCELADA');c.executarAcao();http.expectNone(url+'/atividade/situacao');
    c.observacao='Cancelado pelo cliente';c.executarAcao();const r=http.expectOne(url+'/atividade/situacao');expect(r.request.method).toBe('PATCH');expect(r.request.body).toEqual({versao:2,situacao:'CANCELADA',observacao:'Cancelado pelo cliente'});r.flush({...atividade,situacao:'CANCELADA',versao:3});await f.whenStable();
  });
  it('perfil de consulta não oferece salvar ou ações e não envia mutações',async()=>{
    const f=await criar(atividade,false);expect(f.nativeElement.textContent).not.toContain('Salvar atividade');expect(f.nativeElement.textContent).not.toContain('Cancelar atividade');
    const c=f.componentInstance as any;c.salvar();c.confirmarAcao('CONCLUIDA');c.executarAcao();http.expectNone(url+'/atividade');http.expectNone(url+'/atividade/situacao');
  });
  it('exige salvar o rascunho antes de concluir para não perder alterações',async()=>{
    const f=await criar(atividade);const c=f.componentInstance as any;c.dados.descricao='Alteração ainda não salva';c.confirmarAcao('CONCLUIDA');c.executarAcao();
    http.expectNone(url+'/atividade/situacao');expect(c.dados.descricao).toBe('Alteração ainda não salva');
  });

  it('busca vincula e remove participantes sem perder responsáveis e preserva rascunho em conflito',async()=>{
    const f=await criar(atividade);f.componentRef.setInput('opcoes',{...opcoes,podeVincularContatos:true});await f.whenStable();
    const c=f.componentInstance as any;c.buscaContato='Maria';c.buscarContatos();
    const busca=http.expectOne(r=>r.url===url+'/contatos' && r.params.get('busca')==='Maria');
    busca.flush([{id:'cliente',nome:'Maria Silva',ativo:true}]);await f.whenStable();
    c.adicionarContato(c.contatos()[0]);c.adicionarContato(c.contatos()[0]);
    expect(c.dados.participantes).toEqual(['cliente']);expect(c.dados.responsaveis).toEqual(['pessoa']);
    c.salvar();const request=http.expectOne(url+'/atividade');expect(request.request.body.participantes).toEqual(['cliente']);
    request.flush({detail:'Conflito'},{status:409,statusText:'Conflict'});await f.whenStable();
    expect(c.dados.participantes).toEqual(['cliente']);expect(f.nativeElement.textContent).toContain('Maria Silva');
    c.removerContato('cliente');expect(c.dados.participantes).toEqual([]);
  });
  it('recarrega nomes selecionados e perfil de consulta não modifica vínculos',async()=>{
    const f=await criar();f.componentRef.setInput('opcoes',{...opcoes,podeEditar:false,podeVincularContatos:true});
    f.componentRef.setInput('base',{...atividade,dados:{...atividade.dados,participantes:['cliente']}});await f.whenStable();
    http.expectOne(r=>r.url===url+'/contatos' && r.params.get('ids')==='cliente').flush([{id:'cliente',nome:'Maria Atualizada',ativo:true}]);await f.whenStable();
    expect(f.nativeElement.textContent).toContain('Maria Atualizada');
    const c=f.componentInstance as any;c.removerContato('cliente');c.adicionarContato({id:'outro',nome:'Outro',ativo:true});
    expect(c.dados.participantes).toEqual(['cliente']);expect(c.temAlteracoes()).toBe(false);
    expect(f.nativeElement.textContent).not.toContain('Buscar contato por nome');
  });
  it('sem acesso a Contatos não busca nem mostra participantes e falhas de busca mantêm a seleção',async()=>{
    const f=await criar(atividade);const c=f.componentInstance as any;c.buscarContatos();http.expectNone(r=>r.url===url+'/contatos');
    expect(f.nativeElement.textContent).not.toContain('Clientes e participantes');
    f.componentRef.setInput('opcoes',{...opcoes,podeVincularContatos:true});await f.whenStable();
    c.adicionarContato({id:'cliente',nome:'Maria',ativo:true});c.buscarContatos();
    http.expectOne(r=>r.url===url+'/contatos').flush({},{status:500,statusText:'Failure'});await f.whenStable();
    expect(c.dados.participantes).toEqual(['cliente']);expect(f.nativeElement.textContent).toContain('Não foi possível buscar contatos');
  });

  it('configura recorrência e exige data inicial para tarefas que se repetem',async()=>{
    const f=await criar(atividade);const c=f.componentInstance as any;
    const campo=f.nativeElement.querySelector('[name=repeticao]') as HTMLSelectElement;
    campo.value='MENSAL';campo.dispatchEvent(new Event('change'));await f.whenStable();
    expect(f.nativeElement.textContent).toContain('Ao concluir, será criada uma próxima tarefa');
    c.salvar();const request=http.expectOne(url+'/atividade');
    expect(request.request.body.repeticao).toBe('MENSAL');request.flush({...atividade,dados:{...atividade.dados,repeticao:'MENSAL'}});await f.whenStable();
    c.dados.data=null;await f.whenStable();expect(f.nativeElement.querySelector('[name=data]').required).toBe(true);
    const tipo=f.nativeElement.querySelector('[name=tipo]') as HTMLSelectElement;tipo.value='REUNIAO';tipo.dispatchEvent(new Event('change'));await f.whenStable();
    expect(c.dados.repeticao).toBe('NUNCA');expect(f.nativeElement.querySelector('[name=repeticao]')).toBeNull();
  });
  it('carrega recorrência sem marcar rascunho alterado e conserva configuração na falha',async()=>{
    const f=await criar({...atividade,dados:{...atividade.dados,repeticao:'SEMANAL'}});
    const c=f.componentInstance as any;expect(c.temAlteracoes()).toBe(false);expect(c.dados.repeticao).toBe('SEMANAL');
    c.dados.titulo='Revisão semanal';c.salvar();
    http.expectOne(url+'/atividade').flush({detail:'Conflito'},{status:409,statusText:'Conflict'});await f.whenStable();
    expect(c.dados.repeticao).toBe('SEMANAL');expect(c.dados.titulo).toBe('Revisão semanal');
    c.confirmarAcao('CONCLUIDA');await f.whenStable();expect(f.nativeElement.textContent).toContain('A primeira conclusão cria uma próxima tarefa');
  });

  it('modo workflow preserva o rascunho manual e bloqueia envio da atividade individual',async()=>{
    const f=await criar();const c=f.componentInstance as any;
    c.dados.titulo='Rascunho';c.usandoWorkflow.set(true);await f.whenStable();
    expect(f.nativeElement.querySelector('[name=titulo]')).toBeNull();
    expect(f.nativeElement.querySelector('[name=workflowPrazoFatal]')).not.toBeNull();
    c.salvar();http.expectNone(url);
    c.usandoWorkflow.set(false);await f.whenStable();
    expect(f.nativeElement.querySelector('[name=titulo]').value).toBe('Rascunho');
  });
});
