import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { AuthService } from '../../../core/auth/auth.service';
import { API_URL } from '../../../core/config/api-url.token';
import { CadastrosConfig } from './cadastros-config';

describe('Cadastros e sistemas no servidor', () => {
  let http: HttpTestingController;
  const url = '/api/config/cadastros';
  const resposta = { empresaId: 'empresa-a', papel: 'MASTER', listas: {
    grupos: [{ id: 'grupo-a', tipo: 'grupos', ativo: true, campos: { nome: 'Grupo do banco', sigla: 'GB' } }],
    acoes: [{ id: 'acao-a', tipo: 'acoes', ativo: true, campos: { nome: 'Ação do banco', grupo: 'grupo-a' } }],
  }, rotinas: [], sistemas: [{ id: 'sistema-a', nome: 'Sistema A', url: 'https://example.test', ativo: true, icone: 'link', cor: '#eeeeee', logo: '' }] };
  beforeEach(() => {
    localStorage.setItem('configuracoes-front:cadastro-teste', JSON.stringify({ versao: 1, listas: { grupos: [{ id: 'antigo', nome: 'Rascunho antigo', ativo: true }] } }));
    TestBed.configureTestingModule({ imports: [CadastrosConfig], providers: [provideHttpClient(), provideHttpClientTesting(), { provide: API_URL, useValue: '' }, { provide: AuthService, useValue: { usuario: signal({ id: 'cadastro-teste' }) } }] });
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => { http.verify(); localStorage.removeItem('configuracoes-front:cadastro-teste'); TestBed.resetTestingModule(); });
  async function abrir(valor = resposta) {
    const fixture = TestBed.createComponent(CadastrosConfig); http.expectOne(url).flush(valor); await fixture.whenStable(); return fixture;
  }
  it('salva as páginas selecionadas e restaura a seleção do banco', async () => {
    const fixture = TestBed.createComponent(CadastrosConfig);
    http.expectOne(url).flush({ ...resposta, atalhos: ['crm'] }); await fixture.whenStable();
    const element = fixture.nativeElement as HTMLElement;
    const selecionar = (nome: string) => Array.from(element.querySelectorAll<HTMLLabelElement>('.cfg-check')).find(l => l.textContent?.trim() === nome)!.querySelector<HTMLInputElement>('input')!;
    expect(selecionar('CRM').checked).toBe(true);
    expect(selecionar('Agenda').checked).toBe(false);
    selecionar('Agenda').click(); await fixture.whenStable();
    Array.from(element.querySelectorAll<HTMLButtonElement>('button')).find(b => b.textContent?.trim() === 'Salvar atalhos')!.click();
    const req = http.expectOne(url + '/atalhos');
    expect(req.request.method).toBe('PUT'); expect(req.request.body).toEqual({ atalhos: ['crm', 'agenda'] });
    req.flush(['crm', 'agenda']); await fixture.whenStable();
    expect(element.textContent).toContain('Salvo no banco de dados para toda a empresa');
  });
  it('usa o banco, conserva o rascunho após falha e só confirma a atualização após resposta', async () => {
    const fixture = await abrir(); const element = fixture.nativeElement as HTMLElement;
    element.querySelector<HTMLButtonElement>('.cadastro-cartao')!.click(); await fixture.whenStable();
    const input = element.querySelector<HTMLInputElement>('td input[aria-label=Grupo]')!;
    expect(input.value).toBe('Grupo do banco'); expect(element.textContent).not.toContain('Rascunho antigo');
    input.value = 'Grupo atualizado'; input.dispatchEvent(new Event('input')); await fixture.whenStable();
    element.querySelector<HTMLButtonElement>('td button')!.click();
    const req = http.expectOne(url + '/listas/grupos/grupo-a');
    expect(req.request.method).toBe('PUT'); expect(req.request.body.campos.nome).toBe('Grupo atualizado');
    req.flush({ detail: 'Falha temporária' }, { status: 503, statusText: 'Unavailable' }); await fixture.whenStable();
    expect(input.value).toBe('Grupo atualizado'); expect(element.querySelector('[role=alert]')?.textContent).toContain('Falha temporária');
    element.querySelector<HTMLButtonElement>('td button')!.click();
    http.expectOne(url + '/listas/grupos/grupo-a').flush({ id: 'grupo-a', tipo: 'grupos', ativo: true, campos: { nome: 'Grupo atualizado', sigla: 'GB' } }); await fixture.whenStable();
    expect(element.textContent).toContain('Salvo no banco de dados');
    expect(localStorage.getItem('configuracoes-front:cadastro-teste')).toContain('Rascunho antigo');
  });
  it('envia IDs nas relações e mantém um item que o servidor recusa excluir', async () => {
    const fixture = await abrir(); const element = fixture.nativeElement as HTMLElement;
    Array.from(element.querySelectorAll<HTMLButtonElement>('.cadastro-cartao')).find(b => b.textContent?.includes('Tipos de ação'))!.click(); await fixture.whenStable();
    const select = element.querySelector<HTMLSelectElement>('form select')!;
    expect(select.options[1].value).toBe('grupo-a'); expect(select.options[1].textContent).toContain('Grupo do banco');
    element.querySelector<HTMLButtonElement>('td button.cfg-texto')!.click();
    http.expectOne(url + '/listas/acoes/acao-a').flush({ detail: 'Item em uso' }, { status: 409, statusText: 'Conflict' }); await fixture.whenStable();
    expect(element.querySelector('td input')?.getAttribute('aria-label')).toBe('Tipo de ação'); expect(element.textContent).toContain('Item em uso');
  });
  it('mantém os sistemas editados após falha no salvamento em lote', async () => {
    const fixture = await abrir(); const element = fixture.nativeElement as HTMLElement;
    const input = element.querySelector<HTMLInputElement>('input[placeholder="Título (ex.: ADVBOX)"]')!;
    input.value = 'Sistema B'; input.dispatchEvent(new Event('input')); await fixture.whenStable();
    Array.from(element.querySelectorAll<HTMLButtonElement>('button')).find(b => b.textContent?.trim() === 'Salvar sistemas')!.click();
    const req = http.expectOne(url + '/sistemas'); expect(req.request.body.sistemas[0].nome).toBe('Sistema B');
    req.flush({ detail: 'Endereço inválido' }, { status: 400, statusText: 'Bad Request' }); await fixture.whenStable();
    expect(input.value).toBe('Sistema B'); expect(element.textContent).toContain('Endereço inválido');
  });
  it('mantém os controles bloqueados se não há empresa ou a consulta falha', async () => {
    const fixture = TestBed.createComponent(CadastrosConfig);
    http.expectOne(url).flush({}, { status: 503, statusText: 'Unavailable' }); await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('fieldset').disabled).toBe(true);
    Array.from((fixture.nativeElement as HTMLElement).querySelectorAll<HTMLButtonElement>('button')).find(b => b.textContent?.trim() === 'Tentar novamente')!.click();
    http.expectOne(url).flush({ empresaId: null, papel: null, listas: {}, rotinas: [], sistemas: [] }); await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('fieldset').disabled).toBe(true);
    expect(fixture.nativeElement.textContent).toContain('Cadastre sua empresa');
  });
});

