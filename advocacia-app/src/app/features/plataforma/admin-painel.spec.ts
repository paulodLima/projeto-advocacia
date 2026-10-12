import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { API_URL } from '../../core/config/api-url.token';
import { AuthService } from '../../core/auth/auth.service';
import { AdminPainel } from './admin-painel';

describe('Primeiro convite do responsável', () => {
  let http: HttpTestingController;
  const url = '/api/plataforma/admin/convites';
  const convite = { id: 'convite', nome: 'Responsável', email: 'responsavel@example.test', status: 'PENDENTE', expiraEm: '2026-10-16T12:00:00Z', empresaId: null };
  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [AdminPainel], providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([]),
      { provide: API_URL, useValue: '' }, { provide: AuthService, useValue: { usuario: signal({ nome: 'Admin' }) } }] });
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => { http.verify(); TestBed.resetTestingModule(); });
  it('envia convite sem cadastrar a empresa e permite reenviar e cancelar', async () => {
    const fixture = TestBed.createComponent(AdminPainel);
    http.expectOne(url).flush([]); await fixture.whenStable();
    const nome = fixture.nativeElement.querySelector('#nome') as HTMLInputElement;
    const email = fixture.nativeElement.querySelector('#email') as HTMLInputElement;
    nome.value = convite.nome; nome.dispatchEvent(new Event('input'));
    email.value = convite.email; email.dispatchEvent(new Event('input'));
    fixture.detectChanges(); await fixture.whenStable();
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit', { cancelable: true }));
    const envio = http.expectOne(url); expect(envio.request.method).toBe('POST');
    expect(envio.request.body).toEqual({ nome: convite.nome, email: convite.email }); envio.flush(convite);
    await fixture.whenStable();
    expect(fixture.nativeElement.textContent).toContain('Aguardando primeiro acesso');
    fixture.nativeElement.querySelector('[aria-label^="Reenviar convite"]').click();
    http.expectOne(url + '/convite/reenviar').flush(convite); await fixture.whenStable();
    fixture.nativeElement.querySelector('[aria-label^="Cancelar convite"]').click();
    const cancelamento = http.expectOne(url + '/convite'); expect(cancelamento.request.method).toBe('DELETE'); cancelamento.flush(null);
    await fixture.whenStable(); expect(fixture.nativeElement.textContent).toContain('Cancelado');
    expect(fixture.nativeElement.querySelector('[aria-label^="Reenviar convite"]')).toBeNull();
  });
  it('preserva os dados digitados quando o envio falha', async () => {
    const fixture = TestBed.createComponent(AdminPainel); http.expectOne(url).flush([]); await fixture.whenStable();
    const componente = fixture.componentInstance as any; componente.nome = convite.nome; componente.email = convite.email;
    componente.enviar();
    http.expectOne(url).flush({ detail: 'Email indisponível' }, { status: 503, statusText: 'Unavailable' });
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('#email').value).toBe(convite.email);
    expect(fixture.nativeElement.querySelector('[role=alert]').textContent).toContain('Email indisponível');
  });
});
