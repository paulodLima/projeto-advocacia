import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthService } from '../../../../core/auth/auth.service';
import { API_URL } from '../../../../core/config/api-url.token';
import { Inicio } from './inicio';

describe('Sistemas e rotinas no Início', () => {
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [Inicio], providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting(), { provide: API_URL, useValue: '' }, { provide: AuthService, useValue: { usuario: signal({ nome: 'Pessoa Teste' }) } }] });
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => { http.verify(); TestBed.resetTestingModule(); });
  it('mostra as rotinas do banco e somente os acessos rápidos habilitados', async () => {
    const fixture = TestBed.createComponent(Inicio);
    http.expectOne('/api/config/cadastros/inicio').flush({ empresaId: 'a', papel: 'MEMBRO', listas: {},
      rotinas: [{ id: 'rotina', nome: 'Revisar prazos', periodo: 'diaria' }], sistemas: [
        { id: 'ativo', nome: 'Sistema ativo', url: 'https://example.test', ativo: true, icone: 'link', cor: '#eeeeee', logo: '' },
        { id: 'inativo', nome: 'Sistema oculto', url: 'https://example.test', ativo: false, icone: 'link', cor: '#eeeeee', logo: '' },
      ] }); await fixture.whenStable();
    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('Revisar prazos');
    Array.from(element.querySelectorAll<HTMLButtonElement>('app-painel-inicio header button')).find(b => b.textContent?.includes('Acesso rápido'))!.click(); await fixture.whenStable();
    expect(element.querySelector<HTMLAnchorElement>('a[href="https://example.test"]')?.rel).toBe('noopener noreferrer');
    expect(element.textContent).toContain('Sistema ativo'); expect(element.textContent).not.toContain('Sistema oculto');
  });
  it('informa falha na consulta sem fingir que o escritório não possui rotinas', async () => {
    const fixture = TestBed.createComponent(Inicio);
    http.expectOne('/api/config/cadastros/inicio').flush({}, { status: 503, statusText: 'Unavailable' }); await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('[role=alert]').textContent).toContain('Não foi possível carregar');
  });
});

