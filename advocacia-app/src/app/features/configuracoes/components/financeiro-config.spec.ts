import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { registerLocaleData } from '@angular/common';
import pt from '@angular/common/locales/pt';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { AuthService } from '../../../core/auth/auth.service';
import { API_URL } from '../../../core/config/api-url.token';
import { FinanceiroConfig } from './financeiro-config';
registerLocaleData(pt);

describe('Configuração financeira no servidor',()=>{
  let http:HttpTestingController;
  const conta={id:'conta-a',nome:'Principal',banco:'Banco',tipo:'conta',saldo:12.34,padrao:true,ativo:true};
  const capacidade={horas:160,modoHoras:'estrutura',advogados:1,horasSemanais:40,percentualProdutivo:60,semanasPorMes:4.2,margemLucro:30,fatorPosicionamento:3};
  const dados={empresaId:'empresa-a',papel:'MASTER',versao:2,contas:[conta],categorias:[],centros:[],custos:[{id:'custo-a',nome:'Aluguel',valor:1000}],urh:300,competencia:'2026-10',capacidade};
  beforeEach(()=>{
    TestBed.configureTestingModule({imports:[FinanceiroConfig],providers:[provideHttpClient(),provideHttpClientTesting(),{provide:API_URL,useValue:''},{provide:AuthService,useValue:{usuario:signal({id:'teste-financeiro'})}}]});
    http=TestBed.inject(HttpTestingController);
  });
  afterEach(()=>{http.verify();localStorage.removeItem('configuracoes-front:teste-financeiro');TestBed.resetTestingModule();});
  async function abrir(papel='MASTER') {
    const fixture=TestBed.createComponent(FinanceiroConfig);http.expectOne('/api/config/financeiro').flush({...dados,papel});await fixture.whenStable();return fixture;
  }
  function botao(element:HTMLElement,texto:string) { return Array.from(element.querySelectorAll<HTMLButtonElement>('button')).find(b=>b.textContent?.trim()===texto)!; }
  it('cria pelo servidor e conserva o formulário quando há falha',async()=>{
    const fixture=await abrir();const element=fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('Principal');
    botao(element,'Nova conta ou cartão').click();await fixture.whenStable();
    const nome=element.querySelector<HTMLInputElement>('input[name="nome"]')!;nome.value='Nova conta';nome.dispatchEvent(new Event('input'));await fixture.whenStable();
    element.querySelector('form')!.dispatchEvent(new Event('submit',{bubbles:true,cancelable:true}));
    const req=http.expectOne('/api/config/financeiro/contas');expect(req.request.method).toBe('POST');expect(req.request.body.versao).toBe(2);expect(req.request.body.dados.nome).toBe('Nova conta');
    req.flush({detail:'Falha temporária'},{status:503,statusText:'Unavailable'});await fixture.whenStable();
    expect(nome.value).toBe('Nova conta');expect(element.textContent).toContain('Falha temporária');expect(element.querySelectorAll('tbody tr').length).toBe(1);
    element.querySelector('form')!.dispatchEvent(new Event('submit',{bubbles:true,cancelable:true}));
    http.expectOne('/api/config/financeiro/contas').flush({...dados,versao:3,contas:[conta,{...conta,id:'id-do-servidor',nome:'Nova conta',padrao:false}]});await fixture.whenStable();
    expect(element.querySelector('form')).toBeNull();expect(element.querySelectorAll('tbody tr').length).toBe(2);
    expect(localStorage.getItem('configuracoes-front:teste-financeiro')).toBeNull();
  });
  it('não desativa a conta padrão antes da confirmação do banco',async()=>{
    const fixture=await abrir();const element=fixture.nativeElement as HTMLElement;botao(element,'desativar').click();
    const req=http.expectOne('/api/config/financeiro/contas/conta-a');expect(req.request.body.dados.ativo).toBe(false);expect(req.request.body.dados.padrao).toBe(false);
    req.flush({detail:'Versão desatualizada'},{status:409,statusText:'Conflict'});await fixture.whenStable();
    expect(element.querySelector<HTMLInputElement>('input[type="radio"]')!.checked).toBe(true);expect(botao(element,'desativar')).toBeTruthy();
  });
  it('preserva a seleção da conta padrão quando a troca falha',async()=>{
    const fixture=TestBed.createComponent(FinanceiroConfig);
    http.expectOne('/api/config/financeiro').flush({...dados,contas:[conta,{...conta,id:'conta-b',nome:'Segunda',padrao:false}]});await fixture.whenStable();
    const radios=fixture.nativeElement.querySelectorAll('input[type="radio"]') as NodeListOf<HTMLInputElement>;
    radios[1].click();
    http.expectOne('/api/config/financeiro/contas/conta-b').flush({detail:'Falha temporária'},{status:503,statusText:'Unavailable'});await fixture.whenStable();
    expect(radios[0].checked).toBe(true);expect(radios[1].checked).toBe(false);
  });
  it('salva URH sem substituir a capacidade ainda em edição e envia capacidade separadamente',async()=>{
    const fixture=await abrir();const element=fixture.nativeElement as HTMLElement;
    botao(element,'URH e Custos').click();await fixture.whenStable();
    Array.from(element.querySelectorAll<HTMLButtonElement>('button')).find(b=>b.textContent?.includes('Informar direto'))!.click();await fixture.whenStable();
    const horas=Array.from(element.querySelectorAll<HTMLLabelElement>('label')).find(l=>l.textContent?.includes('Horas produtivas por mês'))!.querySelector('input')!;
    horas.value='120';horas.dispatchEvent(new Event('input'));await fixture.whenStable();
    const urh=element.querySelector<HTMLInputElement>('input[name="urh"]')!;urh.value='321.45';urh.dispatchEvent(new Event('input'));await fixture.whenStable();
    element.querySelector('form')!.dispatchEvent(new Event('submit',{bubbles:true,cancelable:true}));
    const req=http.expectOne('/api/config/financeiro/urh');expect(req.request.body.dados).toEqual({urh:321.45,competencia:'2026-10'});
    req.flush({...dados,versao:3,urh:321.45});await fixture.whenStable();expect(horas.value).toBe('120');
    botao(element,'Salvar capacidade e margem').click();const capacidadeReq=http.expectOne('/api/config/financeiro/capacidade');
    expect(capacidadeReq.request.body.versao).toBe(3);expect(capacidadeReq.request.body.dados.horas).toBe(120);expect(capacidadeReq.request.body.dados.modoHoras).toBe('direto');expect(capacidadeReq.request.body.dados.urh).toBeUndefined();
    capacidadeReq.flush({...dados,versao:4,urh:321.45,capacidade:{...capacidade,horas:120,modoHoras:'direto'}});await fixture.whenStable();expect(urh.value).toBe('321.45');
  });
  it('mantém custos após falha na exclusão',async()=>{
    const fixture=await abrir();const element=fixture.nativeElement as HTMLElement;botao(element,'URH e Custos').click();await fixture.whenStable();
    element.querySelector<HTMLButtonElement>('button[aria-label="Excluir custo"]')!.click();
    const req=http.expectOne(r=>r.method==='DELETE');expect(req.request.url).toBe('/api/config/financeiro/custos/custo-a');expect(req.request.params.get('versao')).toBe('2');
    req.flush({detail:'Falha temporária'},{status:503,statusText:'Unavailable'});await fixture.whenStable();expect(element.textContent).toContain('Aluguel');
  });
  it('bloqueia a edição para membros e para falha de carregamento',async()=>{
    const fixture=await abrir('MEMBRO');expect(fixture.nativeElement.querySelector('fieldset').disabled).toBe(true);
    fixture.destroy();const falha=TestBed.createComponent(FinanceiroConfig);
    http.expectOne('/api/config/financeiro').flush({detail:'Indisponível'},{status:503,statusText:'Unavailable'});await falha.whenStable();
    expect(falha.nativeElement.querySelector('fieldset').disabled).toBe(true);expect(falha.nativeElement.textContent).toContain('Tentar novamente');
  });
});
