import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { API_URL } from '../../../core/config/api-url.token';
import { AuthService } from '../../../core/auth/auth.service';
import { EquipeConfig } from './equipe-config';
import { configuracaoMembroInicial } from '../services/equipe-api.service';

describe('Equipe conectada à API', () => {
  const url = '/api/equipe';
  const master = { id: 'master', nome: 'Master', email: 'master@example.test', papel: 'MASTER', configuracao: configuracaoMembroInicial() };
  const membro = { id: 'membro', nome: 'Pessoa', email: 'pessoa@example.test', papel: 'MEMBRO', configuracao: { ...configuracaoMembroInicial(), telefone: '123' } };
  const convite = { id: 'convite', nome: 'Nova pessoa', email: 'nova@example.test', perfil: 'ASSISTENTE', status: 'PENDENTE', expiraEm: '2026-10-09T12:00:00Z' };
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [EquipeConfig], providers: [provideHttpClient(), provideHttpClientTesting(), { provide: API_URL, useValue: '' }, { provide: AuthService, useValue: { usuario: signal(master) } }] });
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => { http.verify(); TestBed.resetTestingModule(); });
  async function abrir(convites: unknown[] = []) {
    const fixture = TestBed.createComponent(EquipeConfig);
    http.expectOne(url).flush({ membros: [master, membro], convites }); await fixture.whenStable(); return fixture;
  }
  function botao(elemento: HTMLElement, texto: string) { return Array.from(elemento.querySelectorAll('button')).find(b => b.textContent?.includes(texto))!; }
  it('salva a ficha pela API e mantém o rascunho quando a escrita falha', async () => {
    const fixture = await abrir(); const telefone = fixture.nativeElement.querySelector('input[type=tel]') as HTMLInputElement;
    expect(telefone.value).toBe('123'); telefone.value = '456'; telefone.dispatchEvent(new Event('input'));
    botao(fixture.nativeElement, 'Salvar membro').click();
    const req = http.expectOne(url + '/membros/membro'); expect(req.request.method).toBe('PUT'); expect(req.request.body.telefone).toBe('456');
    req.flush({ detail: 'Falha ao salvar' }, { status: 503, statusText: 'Unavailable' }); await fixture.whenStable();
    expect(telefone.value).toBe('456'); expect(fixture.nativeElement.querySelector('[role=alert]').textContent).toContain('Falha ao salvar');
    botao(fixture.nativeElement, 'Salvar membro').click();
    http.expectOne(url + '/membros/membro').flush({ ...membro, configuracao: { ...membro.configuracao, telefone: '456' } }); await fixture.whenStable();
    expect(fixture.nativeElement.textContent).toContain('Dados e permissões do membro salvos.');
  });
  it('envia o convite com perfil e permissões escolhidos', async () => {
    const fixture = await abrir(); botao(fixture.nativeElement, '+ Adicionar membro').click(); await fixture.whenStable();
    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    for (const [name, valor] of [['nome', 'Nova pessoa'], ['email', 'nova@example.test']]) { const input = form.querySelector(`input[name=${name}]`) as HTMLInputElement; input.value = valor; input.dispatchEvent(new Event('input')); }
    const perfil = form.querySelector('select')!; perfil.value = 'FINANCEIRO'; perfil.dispatchEvent(new Event('change'));
    (form.querySelector('input[type=checkbox]') as HTMLInputElement).click(); await fixture.whenStable();
    const agenda = Array.from(form.querySelectorAll('label')).find(l => l.textContent?.trim() === 'Agenda')!;
    (agenda.querySelector('input') as HTMLInputElement).click();
    form.dispatchEvent(new Event('submit', { cancelable: true }));
    const req = http.expectOne(url + '/convites'); expect(req.request.method).toBe('POST');
    expect(req.request.body.configuracao).toMatchObject({ perfil: 'FINANCEIRO', restrito: true, abas: ['agenda'] });
    req.flush(convite); await fixture.whenStable();
    expect(fixture.nativeElement.textContent).toContain('Convite enviado com sucesso.'); expect(fixture.nativeElement.querySelector('form')).toBeNull();
  });
  it('mantém o convite se cancelar falhar e remove após sucesso', async () => {
    const fixture = await abrir([convite]); botao(fixture.nativeElement, 'Cancelar').click();
    http.expectOne(url + '/convites/convite').flush({ detail: 'Falha' }, { status: 500, statusText: 'Error' }); await fixture.whenStable();
    expect(fixture.nativeElement.textContent).toContain('Nova pessoa'); botao(fixture.nativeElement, 'Cancelar').click();
    const req = http.expectOne(url + '/convites/convite'); expect(req.request.method).toBe('DELETE'); req.flush(null); await fixture.whenStable();
    expect(fixture.nativeElement.textContent).not.toContain('Nova pessoa');
  });
  it('não habilita ações se a consulta da equipe for negada', async () => {
    const fixture = TestBed.createComponent(EquipeConfig); http.expectOne(url).flush({ detail: 'Somente o master' }, { status: 403, statusText: 'Forbidden' }); await fixture.whenStable();
    expect(botao(fixture.nativeElement, '+ Adicionar membro').disabled).toBe(true); expect(fixture.nativeElement.querySelector('[role=alert]').textContent).toContain('Somente o master');
  });
});
