import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_URL } from '../../../core/config/api-url.token';
import { WorkflowExecucao } from './workflow-execucao';

describe('Execução de workflows na agenda',()=>{
  let http:HttpTestingController;
  const url='/api/agenda/workflows';
  const modelo={id:'modelo',nome:'Preparar',versao:1,buffer:1,quantidadeEtapas:1};
  const plano={assinatura:'a'.repeat(64),workflowId:'modelo',nome:'Preparar',versao:1,buffer:1,tipoTarefaId:'tipo',prazoFatal:'2026-10-19',diasNaoUteis:[],etapas:[{ordem:1,nome:'Protocolar',responsavel:null,responsavelNome:'',dias:1,inicio:'2026-10-16',fim:'2026-10-16'}]};
  beforeEach(()=>{TestBed.configureTestingModule({imports:[WorkflowExecucao],providers:[provideHttpClient(),provideHttpClientTesting(),{provide:API_URL,useValue:''}]});http=TestBed.inject(HttpTestingController);});
  afterEach(()=>{http.verify();TestBed.resetTestingModule();});
  async function criar(){
    const f=TestBed.createComponent(WorkflowExecucao);f.componentRef.setInput('tipoTarefaId','tipo');f.componentRef.setInput('prazoFatal','2026-10-19');
    await f.whenStable();http.expectOne(url+'?tipoTarefaId=tipo').flush([modelo]);await f.whenStable();return f;
  }
  it('exige adesão e prévia antes de gerar e reutiliza identificador após falha de rede',async()=>{
    const f=await criar(),c=f.componentInstance as any;
    c.gerar();http.expectNone(url+'/execucoes');
    c.alternar();c.gerar();http.expectNone(url+'/execucoes');
    c.prever();const previa=http.expectOne(url+'/previsao');expect(previa.request.body.prazoFatal).toBe('2026-10-19');
    previa.flush(plano);await f.whenStable();
    c.gerar();const primeiro=http.expectOne(url+'/execucoes'),id=primeiro.request.body.requisicaoId;
    expect(primeiro.request.body.assinaturaPlano).toBe(plano.assinatura);
    primeiro.flush({detail:'Falha temporária'},{status:503,statusText:'Unavailable'});await f.whenStable();
    expect(c.plano()).toEqual(plano);expect(c.ativo()).toBe(true);
    let gerado:any;f.componentInstance.gerou.subscribe(e=>gerado=e);
    c.gerar();const segundo=http.expectOne(url+'/execucoes');expect(segundo.request.body.requisicaoId).toBe(id);
    expect(gerado).toBeUndefined();segundo.flush({id:'execucao',plano,atividades:['atividade']});await f.whenStable();expect(gerado.id).toBe('execucao');
  });
  it('trocar prazo invalida a prévia e impede gerar datas antigas',async()=>{
    const f=await criar(),c=f.componentInstance as any;c.alternar();c.prever();http.expectOne(url+'/previsao').flush(plano);await f.whenStable();
    f.componentRef.setInput('prazoFatal','2026-10-20');await f.whenStable();
    expect(c.plano()).toBeNull();c.gerar();http.expectNone(url+'/execucoes');
    c.prever();expect(http.expectOne(url+'/previsao').request.body.prazoFatal).toBe('2026-10-20');
  });
  it('exige prazo manual e preserva datas sem expediente em conflito',async()=>{
    const f=await criar(),c=f.componentInstance as any;c.alternar();
    f.componentRef.setInput('prazoFatal',null);await f.whenStable();c.prever();http.expectNone(url+'/previsao');
    f.componentRef.setInput('prazoFatal','2026-10-19');await f.whenStable();c.diasSemExpediente='2026-10-16';c.prever();
    const r=http.expectOne(url+'/previsao');expect(r.request.body.diasNaoUteis).toEqual(['2026-10-16']);
    r.flush({detail:'Modelo alterado'},{status:409,statusText:'Conflict'});await f.whenStable();
    expect(c.diasSemExpediente).toBe('2026-10-16');expect(c.erro()).toBe('Modelo alterado');
  });
});
