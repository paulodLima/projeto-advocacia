import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { API_URL } from '../../../core/config/api-url.token';
import { VinculosAgenda } from './vinculos-agenda';
describe('Consultas e tarefas vinculadas à agenda',()=>{
 let http:HttpTestingController;
 const filtro={inicio:'2026-10-01',fim:'2026-10-31',tipo:'',situacao:'PENDENTE',responsavel:'',busca:'',incluirSemData:true};
 const url='/api/agenda/vinculos';
 const item={id:'tarefa',leadId:null,tarefaId:'tarefa',casoId:'caso',titulo:'Tarefa privada autorizada',tipo:'TAREFA',data:'2026-10-15',dataFim:null,hora:'10:00',horaFim:'11:00',prazoFatal:'2026-10-19',diaInteiro:false,local:'Sala',situacao:'PENDENTE',responsaveis:[{id:'pessoa',nome:'Ana',ativo:true}]};
 beforeEach(()=>{TestBed.configureTestingModule({imports:[VinculosAgenda],providers:[provideRouter([]),provideHttpClient(),provideHttpClientTesting(),{provide:API_URL,useValue:''}]});http=TestBed.inject(HttpTestingController);});
 afterEach(()=>{http.verify();TestBed.resetTestingModule();});
 async function criar(){const f=TestBed.createComponent(VinculosAgenda);f.componentRef.setInput('filtro',filtro);await f.whenStable();return f;}
 it('abre lead e tarefa exata na origem sem oferecer edição paralela',async()=>{
  const f=await criar();const r=http.expectOne(r=>r.url===url);expect(r.request.params.get('situacao')).toBe('PENDENTE');
  r.flush({itens:[item,{...item,id:'consulta',leadId:'lead',tarefaId:null,casoId:null,titulo:'Consulta CRM',responsaveis:[]}],total:2,pagina:0,tamanho:20});await f.whenStable();
  const links=f.nativeElement.querySelectorAll('a');
  expect(links[0].getAttribute('href')).toBe('/casos?id=caso&aba=tarefas&tarefa=tarefa');
  expect(links[1].getAttribute('href')).toBe('/crm?lead=lead');
  expect(f.nativeElement.textContent).toContain('Ana');expect(f.nativeElement.textContent).toContain('19/10/2026');
  expect(f.nativeElement.textContent).not.toContain('Salvar');
 });
 it('cancela resposta antiga e reinicia paginação quando filtros mudam',async()=>{
  const f=await criar();const antiga=http.expectOne(r=>r.url===url);
  f.componentRef.setInput('filtro',{...filtro,busca:'Nova',incluirSemData:false});await f.whenStable();
  expect(antiga.cancelled).toBe(true);
  const atual=http.expectOne(r=>r.url===url);expect(atual.request.params.get('busca')).toBe('Nova');expect(atual.request.params.get('pagina')).toBe('0');
  atual.flush({itens:[],total:0,pagina:0,tamanho:20});await f.whenStable();expect(f.nativeElement.textContent).toContain('Nenhuma consulta');
 });
 it('preserva filtros na falha e tenta novamente sem exibir sucesso',async()=>{
  const f=await criar();http.expectOne(r=>r.url===url).flush({detail:'Acesso alterado'},{status:403,statusText:'Forbidden'});await f.whenStable();
  expect(f.nativeElement.textContent).toContain('Acesso alterado');
  f.nativeElement.querySelector('button').click();
  const r=http.expectOne(r=>r.url===url);expect(r.request.params.get('inicio')).toBe(filtro.inicio);
  r.flush({itens:[],total:0,pagina:0,tamanho:20});await f.whenStable();expect(f.nativeElement.querySelector('[role=alert]')).toBeNull();
 });
});
