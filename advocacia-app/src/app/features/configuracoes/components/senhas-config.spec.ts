import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { SenhasConfig } from './senhas-config';
import { API_URL } from '../../../core/config/api-url.token';
describe('Cofre de credenciais',()=>{
  let http:HttpTestingController;
  const item={id:'a',nome:'Sistema de teste',url:'https://example.test',usuario:'login',descricao:'',observacao:'',soAdmin:false,versao:2};
  beforeEach(()=>{TestBed.configureTestingModule({imports:[SenhasConfig],providers:[provideHttpClient(),provideHttpClientTesting(),{provide:API_URL,useValue:''}]});http=TestBed.inject(HttpTestingController);});
  afterEach(()=>{http.verify();vi.useRealTimers();TestBed.resetTestingModule();});
  async function abrir(podeEditar=true) {
    const f=TestBed.createComponent(SenhasConfig);http.expectOne(r=>r.url.endsWith('/api/config/cofre')).flush({empresaId:'empresa',podeEditar,configurado:true,credenciais:[item]});await f.whenStable();return f;
  }
  function button(e:HTMLElement,text:string) {return Array.from(e.querySelectorAll<HTMLButtonElement>('button')).find(b=>b.textContent?.trim()===text)!;}
  it('não busca senha na carga e revela apenas por ação explícita, limpando ao destruir',async()=>{
    const f=await abrir(false);const e=f.nativeElement as HTMLElement;expect(e.textContent).not.toContain('segredo-teste');http.expectNone(r=>r.url.endsWith('/revelar'));
    const hidden=vi.spyOn(document,'hidden','get').mockReturnValue(false);
    button(e,'Mostrar senha').click();const req=http.expectOne(r=>r.url.endsWith('/a/revelar'));expect(req.request.method).toBe('POST');req.flush({senha:'segredo-teste'});await f.whenStable();expect(e.textContent).toContain('segredo-teste');
    hidden.mockReturnValue(true);document.dispatchEvent(new Event('visibilitychange'));await f.whenStable();expect(e.textContent).not.toContain('segredo-teste');hidden.mockRestore();f.destroy();
  });
  it('edita metadados sem revelar a senha e envia null para preservá-la',async()=>{
    const f=await abrir();const e=f.nativeElement as HTMLElement;button(e,'Editar').click();await f.whenStable();
    expect(e.querySelector<HTMLInputElement>('input[name="senha"]')!.value).toBe('');http.expectNone(r=>r.url.endsWith('/revelar'));
    e.querySelector('form')!.dispatchEvent(new Event('submit',{bubbles:true,cancelable:true}));const req=http.expectOne(r=>r.method==='PUT');expect(req.request.body.senha).toBeNull();expect(req.request.body.versao).toBe(2);
    req.flush({...item,versao:3});await f.whenStable();expect(e.querySelector('form')).toBeNull();expect(e.textContent).toContain('salva no cofre');
  });
  it('preserva formulário em falha e não permite editar por conta sem permissão',async()=>{
    const f=await abrir();const e=f.nativeElement as HTMLElement;button(e,'Editar').click();await f.whenStable();
    e.querySelector('form')!.dispatchEvent(new Event('submit',{bubbles:true,cancelable:true}));http.expectOne(r=>r.method==='PUT').flush({detail:'Conflito de versão'},{status:409,statusText:'Conflict'});await f.whenStable();expect(e.querySelector('form')).not.toBeNull();expect(e.textContent).toContain('Conflito de versão');f.destroy();
    const membro=await abrir(false);expect(button(membro.nativeElement,'Editar')).toBeUndefined();expect(button(membro.nativeElement,'+ Nova credencial')).toBeUndefined();
  });
});
