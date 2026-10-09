import { IdentidadeApiService } from '../../../core/branding/identidade-api.service';
import { of } from 'rxjs';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { API_URL } from '../../../core/config/api-url.token';
import { AuthService } from '../../../core/auth/auth.service';
import { EmpresaConfig } from './empresa-config';

describe('Empresa no servidor', () => {
  const url = '/api/empresas';
  const dados = { razao_social: 'Sociedade A', cnpj: '11222333000181', endereco: 'Rua A', cidade: 'São Paulo' };
  let http: HttpTestingController;
  beforeEach(() => {
    localStorage.setItem('configuracoes-front:empresa-teste', JSON.stringify({ versao: 1, empresa: { razao_social: 'Rascunho antigo' } }));
    TestBed.configureTestingModule({ imports: [EmpresaConfig], providers: [{ provide: IdentidadeApiService, useValue: { carregar: () => of({ empresaId: 'empresa-a', nome: 'Sociedade A', identidade: { imagens: {}, usos: {}, zoom: 1, posX: 50, posY: 50 } }) } }, provideHttpClient(), provideHttpClientTesting(), { provide: API_URL, useValue: '' }, { provide: AuthService, useValue: { usuario: signal({ id: 'empresa-teste', nome: 'Teste', email: 'teste@example.test' }) } }] });
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => { http.verify(); localStorage.removeItem('configuracoes-front:empresa-teste'); TestBed.resetTestingModule(); });
  async function abrir(papel: 'MASTER' | 'MEMBRO' | null, valores = dados) {
    const fixture = TestBed.createComponent(EmpresaConfig);
    http.expectOne(url + '/minha').flush({ id: papel ? 'empresa-a' : null, papel, dados: valores });
    await fixture.whenStable();
    return fixture;
  }
  it('usa os dados do banco e mantém o formulário após falha no salvamento', async () => {
    const fixture = await abrir('MASTER');
    const input = fixture.nativeElement.querySelector('input[name=razao_social]') as HTMLInputElement;
    expect(input.value).toBe('Sociedade A');
    input.value = 'Sociedade B'; input.dispatchEvent(new Event('input'));
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit', { cancelable: true }));
    const req = http.expectOne(url + '/minha');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body.dados.razao_social).toBe('Sociedade B');
    expect(Object.keys(req.request.body.dados)).toHaveLength(24);
    req.flush({ detail: 'Não permitido' }, { status: 403, statusText: 'Forbidden' });
    await fixture.whenStable();
    expect(input.value).toBe('Sociedade B');
    expect(fixture.nativeElement.querySelector('[role=alert]').textContent).toContain('Não permitido');
    expect(JSON.parse(localStorage.getItem('configuracoes-front:empresa-teste')!).empresa.razao_social).toBe('Rascunho antigo');
  });
  it('cadastra a primeira empresa e usa PUT nos próximos salvamentos', async () => {
    const fixture = await abrir(null);
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit', { cancelable: true }));
    const req = http.expectOne(url);
    expect(req.request.method).toBe('POST');
    req.flush({ id: 'empresa-a', papel: 'MASTER', dados });
    await fixture.whenStable();
    expect(fixture.nativeElement.textContent).toContain('Dados da empresa salvos com sucesso.');
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit', { cancelable: true }));
    const update = http.expectOne(url + '/minha'); expect(update.request.method).toBe('PUT');
    update.flush({ id: 'empresa-a', papel: 'MASTER', dados });
  });
  it('membro consulta os dados e não pode enviar alterações', async () => {
    const fixture = await abrir('MEMBRO');
    expect(fixture.nativeElement.querySelector('input[name=razao_social]').disabled).toBe(true);
    expect(fixture.nativeElement.querySelector('button[type=submit]')).toBeNull();
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit', { cancelable: true }));
    http.expectNone(req => req.method !== 'GET');
  });
  it('não libera o formulário quando a consulta falha e permite tentar novamente', async () => {
    const fixture = TestBed.createComponent(EmpresaConfig);
    http.expectOne(url + '/minha').flush({}, { status: 503, statusText: 'Unavailable' });
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('input[name=razao_social]').disabled).toBe(true);
    const retry = Array.from(fixture.nativeElement.querySelectorAll('button')).find((b: any) => b.textContent.includes('Tentar novamente')) as HTMLButtonElement;
    retry.click();
    http.expectOne(url + '/minha').flush({ id: null, papel: null, dados: {} });
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('input[name=razao_social]').disabled).toBe(false);
    expect(fixture.nativeElement.querySelector('button[type=submit]').disabled).toBe(true);
  });
});

