import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { AuthService } from '../../../core/auth/auth.service';
import { API_URL } from '../../../core/config/api-url.token';
import { DocumentosConfig } from './documentos-config';
describe('Documentos no servidor', () => {
  let http: HttpTestingController;
  const documento = { logo:'',rodape:'',alinhamento:'direita',repetir:false,documento:true,endereco:false,contato:true };
  beforeEach(() => {
    TestBed.configureTestingModule({imports:[DocumentosConfig],providers:[provideHttpClient(),provideHttpClientTesting(),{provide:API_URL,useValue:''},{provide:AuthService,useValue:{usuario:signal({id:'teste-documento'})}}]});
    http=TestBed.inject(HttpTestingController);
  });
  afterEach(()=>{http.verify();TestBed.resetTestingModule();});
  async function abrir(papel='MASTER') {
    const fixture=TestBed.createComponent(DocumentosConfig);
    http.expectOne('/api/config/documentos').flush({empresaId:'empresa-a',papel,documento,modelos:{'recibo:corpo':'<p>Texto do banco {{cliente}}</p>','recibo:rodape':''}});
    await fixture.whenStable();return fixture;
  }
  it('carrega modelos do banco e preserva o texto digitado após falha',async()=>{
    const fixture=await abrir();const element=fixture.nativeElement as HTMLElement;
    const campo=element.querySelector<HTMLElement>('[contenteditable]')!;
    expect(campo.textContent).toContain('Texto do banco');
    campo.innerHTML='<p>Texto atualizado {{cliente}}</p>';campo.dispatchEvent(new Event('input'));await fixture.whenStable();
    const salvar=Array.from(element.querySelectorAll<HTMLButtonElement>('button')).find(b=>b.textContent==='Salvar modelos')!;
    salvar.click();const req=http.expectOne('/api/config/documentos/modelos');expect(req.request.body.modelos['recibo:corpo']).toContain('Texto atualizado');
    expect(Object.keys(req.request.body.modelos)).toHaveLength(9);
    req.flush({detail:'Falha temporária'},{status:503,statusText:'Unavailable'});await fixture.whenStable();
    expect(campo.textContent).toContain('Texto atualizado');expect(element.textContent).toContain('Falha temporária');
    expect(localStorage.getItem('configuracoes-front:teste-documento')).toBeNull();
  });
  it('salva papel separadamente sem sobrescrever modelos',async()=>{
    const fixture=await abrir();const element=fixture.nativeElement as HTMLElement;
    Array.from(element.querySelectorAll<HTMLButtonElement>('button')).find(b=>b.textContent==='Salvar papel timbrado')!.click();
    const req=http.expectOne('/api/config/documentos/papel');expect(req.request.body).toEqual(documento);req.flush(documento);await fixture.whenStable();
    expect(element.textContent).toContain('Papel timbrado salvo');http.expectNone('/api/config/documentos/modelos');
  });
  it('bloqueia controles e contenteditable para membros',async()=>{
    const fixture=await abrir('MEMBRO');expect(fixture.nativeElement.querySelector('fieldset').disabled).toBe(true);
    expect(fixture.nativeElement.querySelector('[contenteditable]').getAttribute('contenteditable')).toBe('false');
  });
});
