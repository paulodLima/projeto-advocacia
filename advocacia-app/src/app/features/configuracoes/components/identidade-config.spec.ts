import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { AuthService } from '../../../core/auth/auth.service';
import { API_URL } from '../../../core/config/api-url.token';
import { IdentidadeEmpresaService } from '../../../core/branding/identidade-empresa.service';
import { EmpresaConfig } from './empresa-config';

describe('Formulário da identidade visual', () => {
  let http: HttpTestingController;
  const identidade = { imagens: { logo_simples_branca: 'data:image/png;base64,teste' }, usos: {}, zoom: 1, posX: 50, posY: 50 };
  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [EmpresaConfig], providers: [provideHttpClient(), provideHttpClientTesting(),
      { provide: API_URL, useValue: '' }, { provide: AuthService, useValue: { usuario: signal({ id: 'marca-teste' }) } }] });
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => { http.verify(); TestBed.resetTestingModule(); });
  async function abrir(papel: 'MASTER' | 'MEMBRO') {
    const fixture = TestBed.createComponent(EmpresaConfig);
    http.expectOne('/api/empresas/minha').flush({ id: 'empresa-a', papel, dados: {} });
    http.expectOne('/api/empresas/minha/identidade').flush({ empresaId: 'empresa-a', nome: 'Empresa A', identidade });
    await fixture.whenStable();
    return fixture;
  }
  it('publica a seleção apenas após salvar, conserva o rascunho em caso de falha e não usa localStorage', async () => {
    const fixture = await abrir('MASTER');
    const element = fixture.nativeElement as HTMLElement;
    const uso = Array.from(element.querySelectorAll<HTMLButtonElement>('.uso')).find(b => b.textContent?.trim() === 'Login' && !b.disabled)!;
    const salvar = Array.from(element.querySelectorAll<HTMLButtonElement>('button')).find(b => b.textContent?.trim() === 'Salvar identidade visual')!;
    uso.click(); await fixture.whenStable();
    expect(TestBed.inject(IdentidadeEmpresaService).identidade().logoUrl).toBe('/images/logo-ga.svg');
    salvar.click();
    const req = http.expectOne('/api/empresas/minha/identidade');
    expect(req.request.body.usos.login).toBe('logo_simples_branca');
    req.flush({ detail: 'Falha temporária' }, { status: 503, statusText: 'Unavailable' });
    await fixture.whenStable();
    expect(uso.classList.contains('selecionado')).toBe(true);
    expect(element.textContent).toContain('Falha temporária');
    expect(localStorage.getItem('configuracoes-front:marca-teste')).toBeNull();
    salvar.click();
    http.expectOne('/api/empresas/minha/identidade').flush({ empresaId: 'empresa-a', nome: 'Empresa A', identidade: { ...identidade, usos: { login: 'logo_simples_branca' } } });
    await fixture.whenStable();
    expect(TestBed.inject(IdentidadeEmpresaService).identidade().logoUrl).toBe(identidade.imagens.logo_simples_branca);
    expect(element.textContent).toContain('Identidade visual salva');
    expect(element.querySelector<HTMLInputElement>('input[readonly]')?.value).toContain('/login?empresa=empresa-a');
  });
  it('permite ao membro consultar as imagens com os controles de edição bloqueados', async () => {
    const fixture = await abrir('MEMBRO');
    expect(fixture.nativeElement.querySelector('fieldset').disabled).toBe(true);
    http.expectNone(req => req.method === 'PUT');
  });
});

