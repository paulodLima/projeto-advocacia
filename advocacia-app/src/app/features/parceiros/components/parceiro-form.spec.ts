import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_URL } from '../../../core/config/api-url.token';
import { ParceiroForm } from './parceiro-form';
import { parceiroVazio } from '../models/parceiro.model';

describe('Formulário de parceiros', () => {
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ParceiroForm],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: API_URL, useValue: '' },
      ],
    });
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => {
    http.verify();
    TestBed.resetTestingModule();
  });
  function preencher(e: HTMLElement, nome: string, valor: string) {
    const input = e.querySelector<HTMLInputElement>(`input[name="${nome}"]`)!;
    input.value = valor;
    input.dispatchEvent(new Event('input'));
  }
  function enviar(e: HTMLElement) {
    e.querySelector('form')!.dispatchEvent(
      new Event('submit', { bubbles: true, cancelable: true }),
    );
  }
  it('preserva dados profissionais e versão na edição e mantém rascunho após conflito', async () => {
    const f = TestBed.createComponent(ParceiroForm);
    const base = parceiroVazio();
    base.dados = {
      ...base.dados,
      nome: 'Escritório',
      tipo_pessoa: 'PJ',
      oab: '123/DF',
      foto_propria: 'data:image/jpeg;base64,AAAA',
    };
    base.socios = [{ nome: 'Sócia', oab: '222/DF' }];
    base.areasAtuacao = ['Família'];
    f.componentRef.setInput('parceiro', {
      ...base,
      id: 'parceiro',
      empresaId: 'empresa',
      versao: 3,
    });
    await f.whenStable();
    const e = f.nativeElement as HTMLElement;
    preencher(e, 'nome', 'Escritório novo');
    await f.whenStable();
    enviar(e);
    const r = http.expectOne('/api/parceiros/parceiro');
    expect(r.request.method).toBe('PUT');
    expect(r.request.body.versao).toBe(3);
    expect(r.request.body.socios).toEqual(base.socios);
    expect(r.request.body.areasAtuacao).toEqual(['Família']);
    expect(r.request.body.dados.foto_propria).toBe(base.dados['foto_propria']);
    r.flush(
      { detail: 'Parceiro atualizado em outra sessão.' },
      { status: 409, statusText: 'Conflict' },
    );
    await f.whenStable();
    expect(e.textContent).toContain('Parceiro atualizado');
    expect(e.querySelector<HTMLInputElement>('input[name="nome"]')!.value).toBe('Escritório novo');
  });
  it('não envia sócios ocultos ao salvar advogado PF e só confirma após resposta', async () => {
    const f = TestBed.createComponent(ParceiroForm);
    const base = parceiroVazio();
    base.dados['nome'] = 'Ana';
    base.dados['tipo_pessoa'] = 'PJ';
    base.socios = [{ nome: 'Sócio', oab: '123/DF' }];
    f.componentRef.setInput('parceiro', { ...base, id: 'p', empresaId: 'e', versao: 1 });
    await f.whenStable();
    const e = f.nativeElement as HTMLElement;
    Array.from(e.querySelectorAll('button'))
      .find((b) => b.textContent?.trim() === 'Advogado (PF)')!
      .click();
    await f.whenStable();
    const emit = vi.fn();
    f.componentInstance.salvo.subscribe(emit);
    enviar(e);
    const r = http.expectOne('/api/parceiros/p');
    expect(r.request.body.socios).toEqual([]);
    expect(emit).not.toHaveBeenCalled();
    r.flush({ ...r.request.body, id: 'p', empresaId: 'e', versao: 2 });
    expect(emit).toHaveBeenCalledOnce();
  });
  it('preserva endereço digitado durante consulta ao CEP e ignora resposta de CEP antigo', async () => {
    const f = TestBed.createComponent(ParceiroForm);
    await f.whenStable();
    const e = f.nativeElement as HTMLElement;
    preencher(e, 'cep', '70000000');
    e.querySelector<HTMLInputElement>('input[name="cep"]')!.dispatchEvent(new Event('blur'));
    const antigo = http.expectOne('https://viacep.com.br/ws/70000000/json/');
    preencher(e, 'cep', '71000000');
    antigo.flush({ logradouro: 'Rua antiga', bairro: 'Antigo', localidade: 'Outra', uf: 'DF' });
    await f.whenStable();
    expect(e.querySelector<HTMLInputElement>('input[name="logradouro"]')!.value).toBe('');
    e.querySelector<HTMLInputElement>('input[name="cep"]')!.dispatchEvent(new Event('blur'));
    const atual = http.expectOne('https://viacep.com.br/ws/71000000/json/');
    preencher(e, 'logradouro', 'Rua digitada');
    atual.flush({ logradouro: 'Rua API', bairro: 'Bairro API', localidade: 'Brasília', uf: 'DF' });
    await f.whenStable();
    expect(e.querySelector<HTMLInputElement>('input[name="logradouro"]')!.value).toBe(
      'Rua digitada',
    );
    expect(e.querySelector<HTMLInputElement>('input[name="bairro"]')!.value).toBe('Bairro API');
  });
});
