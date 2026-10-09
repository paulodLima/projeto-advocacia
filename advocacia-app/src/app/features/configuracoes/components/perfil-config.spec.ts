import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { API_URL } from '../../../core/config/api-url.token';
import { AuthService } from '../../../core/auth/auth.service';
import { PerfilConfig } from './perfil-config';

describe('Meu perfil no banco', () => {
  const url = '/api/usuarios/me/perfil';
  const perfil = { usuarioId: 'perfil-api-teste', telefone: '123', emailPessoal: '', endereco: 'Rua A', foto: '' };
  let http: HttpTestingController;
  beforeEach(() => {
    localStorage.setItem('configuracoes-front:perfil-api-teste', JSON.stringify({ versao: 1, perfil: { telefone: 'rascunho', emailPessoal: '', endereco: '', foto: '' } }));
    TestBed.configureTestingModule({ imports: [PerfilConfig], providers: [provideHttpClient(), provideHttpClientTesting(), { provide: API_URL, useValue: '' }, { provide: AuthService, useValue: { usuario: signal({ id: perfil.usuarioId, nome: 'Teste', email: 'acesso@example.test' }) } }] });
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => { http.verify(); localStorage.removeItem('configuracoes-front:perfil-api-teste'); TestBed.resetTestingModule(); });
  it('carrega do servidor, salva os campos e preserva o formulário quando o servidor falha', async () => {
    const fixture = TestBed.createComponent(PerfilConfig);
    http.expectOne(url).flush(perfil);
    await fixture.whenStable();
    const input = fixture.nativeElement.querySelector('input[name=telefone]') as HTMLInputElement;
    expect(input.value).toBe('(12) 3');
    input.value = '456'; input.dispatchEvent(new Event('input'));
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit', { cancelable: true }));
    const escrita = http.expectOne(url);
    expect(escrita.request.method).toBe('PUT');
    expect(escrita.request.body).toEqual({ telefone: '456', emailPessoal: '', endereco: 'Rua A' });
    escrita.flush({ detail: 'Falha ao salvar' }, { status: 500, statusText: 'Error' });
    await fixture.whenStable();
    expect(input.value).toBe('(45) 6');
    expect(fixture.nativeElement.querySelector('[role=alert]').textContent).toContain('Falha ao salvar');
    expect(JSON.parse(localStorage.getItem('configuracoes-front:perfil-api-teste')!).perfil.telefone).toBe('rascunho');
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit', { cancelable: true }));
    http.expectOne(url).flush({ ...perfil, telefone: '456' });
    await fixture.whenStable();
    expect(fixture.nativeElement.textContent).toContain('Dados salvos com sucesso.');
  });
  it('bloqueia edição se a consulta falhar e permite tentar novamente', async () => {
    const fixture = TestBed.createComponent(PerfilConfig);
    http.expectOne(url).flush({}, { status: 503, statusText: 'Unavailable' });
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('button[type=submit]').disabled).toBe(true);
    const retry = Array.from(fixture.nativeElement.querySelectorAll('button')).find((b: any) => b.textContent.includes('Tentar novamente')) as HTMLButtonElement;
    retry.click();
    http.expectOne(url).flush(perfil);
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('button[type=submit]').disabled).toBe(false);
  });
});
