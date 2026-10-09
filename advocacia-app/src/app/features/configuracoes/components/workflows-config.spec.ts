import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { AuthService } from '../../../core/auth/auth.service';
import { API_URL } from '../../../core/config/api-url.token';
import { WorkflowsConfig } from './workflows-config';

describe('Workflows no servidor', () => {
  let http: HttpTestingController;
  const w = { id:'workflow-a',nome:'Petição inicial',buffer:2,versao:3,gatilhos:['tarefa-a'],etapas:[{id:'etapa-a',nome:'Revisar',responsavel:null,dias:2}] };
  beforeEach(() => {
    TestBed.configureTestingModule({imports:[WorkflowsConfig],providers:[provideHttpClient(),provideHttpClientTesting(),{provide:API_URL,useValue:''},{provide:AuthService,useValue:{usuario:signal({id:'teste-workflow'})}}]});
    http=TestBed.inject(HttpTestingController);
  });
  afterEach(()=>{ http.verify(); localStorage.removeItem('configuracoes-front:teste-workflow'); TestBed.resetTestingModule(); });
  async function abrir(papel='MASTER') {
    const fixture=TestBed.createComponent(WorkflowsConfig);
    http.expectOne('/api/config/workflows').flush({empresaId:'empresa-a',papel,workflows:[w,{...w,id:'workflow-b',nome:'Segundo'}],tarefas:[{id:'tarefa-a',nome:'Petição',ativo:true}],responsaveis:[{id:'usuario-a',nome:'Pessoa da equipe',ativo:true}]});
    await fixture.whenStable();
    const element=fixture.nativeElement as HTMLElement;
    element.querySelector<HTMLButtonElement>('[aria-expanded]')!.click(); await fixture.whenStable();
    return fixture;
  }
  it('exibe nomes para os IDs e salva somente o workflow escolhido sem gravar localmente',async()=>{
    const fixture=await abrir(); const element=fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('Petição'); expect(element.textContent).toContain('Pessoa da equipe');
    const outros=element.querySelectorAll<HTMLInputElement>('input[aria-label="Nome do workflow"]');
    outros[1].value='Segundo rascunho'; outros[1].dispatchEvent(new Event('input')); await fixture.whenStable();
    Array.from(element.querySelectorAll<HTMLButtonElement>('button')).find(b=>b.textContent==='Salvar workflow')!.click();
    const req=http.expectOne('/api/config/workflows/workflow-a'); expect(req.request.method).toBe('PUT');
    expect(req.request.body.versao).toBe(3); expect(req.request.body.gatilhos).toEqual(['tarefa-a']); expect(req.request.body.etapas[0].responsavel).toBeNull();
    req.flush({...w,versao:4}); await fixture.whenStable();
    expect(element.querySelectorAll<HTMLInputElement>('input[aria-label="Nome do workflow"]')[1].value).toBe('Segundo rascunho');
    expect(element.textContent).toContain('salvo no banco'); expect(localStorage.getItem('configuracoes-front:teste-workflow')).toBeNull();
  });
  it('preserva etapas editadas e o workflow quando salvar ou excluir falha',async()=>{
    const fixture=await abrir(); const element=fixture.nativeElement as HTMLElement;
    const etapa=element.querySelector<HTMLInputElement>('input[aria-label="Nome da etapa"]')!;
    etapa.value='Revisar rascunho'; etapa.dispatchEvent(new Event('input')); await fixture.whenStable();
    Array.from(element.querySelectorAll<HTMLButtonElement>('button')).find(b=>b.textContent==='Salvar workflow')!.click();
    http.expectOne('/api/config/workflows/workflow-a').flush({detail:'Conflito de versão'},{status:409,statusText:'Conflict'}); await fixture.whenStable();
    expect(etapa.value).toBe('Revisar rascunho'); expect(element.textContent).toContain('Conflito de versão');
    Array.from(element.querySelectorAll<HTMLButtonElement>('button')).find(b=>b.textContent==='Excluir ×')!.click();
    const req=http.expectOne(r=>r.method==='DELETE'); expect(req.request.params.get('versao')).toBe('3');
    req.flush({detail:'Falha temporária'},{status:503,statusText:'Unavailable'}); await fixture.whenStable();
    expect(element.querySelectorAll('input[aria-label="Nome do workflow"]').length).toBe(2);
  });
  it('somente libera criação e edição após carregar uma empresa MASTER',async()=>{
    const fixture=await abrir('MEMBRO'); expect(fixture.nativeElement.querySelector('fieldset').disabled).toBe(true);
  });
  it('bloqueia edição quando a carga falha e oferece nova tentativa',async()=>{
    const fixture=TestBed.createComponent(WorkflowsConfig);
    http.expectOne('/api/config/workflows').flush({detail:'Indisponível'},{status:503,statusText:'Unavailable'}); await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('fieldset').disabled).toBe(true);
    expect(fixture.nativeElement.textContent).toContain('Tentar novamente');
  });
});
