import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthService } from '../../../../core/auth/auth.service';
import { API_URL } from '../../../../core/config/api-url.token';
import { AcessoService } from '../../../../core/auth/acesso.service';
import { Inicio } from './inicio';

describe('Sistemas e rotinas no Início', () => {
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [Inicio], providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting(), { provide: API_URL, useValue: '' }, { provide: AuthService, useValue: { usuario: signal({ nome: 'Pessoa Teste' }) } }] });
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => { http.verify(); TestBed.resetTestingModule(); });
  it('exibe somente páginas selecionadas e permitidas, inclusive nenhuma', async () => {
    const acesso = TestBed.inject(AcessoService);
    acesso.acesso.set({ empresaId: 'a', papel: 'MEMBRO', perfil: 'ASSISTENTE', modulos: ['inicio', 'crm', 'contatos'], enviaDocumento: false });
    const fixture = TestBed.createComponent(Inicio);
    http.expectOne('/api/config/cadastros/inicio').flush({ empresaId: 'a', rotinas: [], sistemas: [], atalhos: ['crm', 'financeiro', 'contatos'] });
    await fixture.whenStable();
    Array.from(fixture.nativeElement.querySelectorAll('app-painel-inicio header button') as NodeListOf<HTMLButtonElement>).find(b => b.textContent?.includes('Acesso rápido'))!.click();
    await fixture.whenStable();
    const painel = fixture.nativeElement.querySelector('.atalhos') as HTMLElement;
    expect(painel.querySelector('a[href="/crm"]')).not.toBeNull();
    expect(painel.querySelector('a[href="/contatos"]')).not.toBeNull();
    expect(painel.querySelector('a[href="/financeiro"]')).toBeNull();
    (fixture.componentInstance as unknown as { carregarCadastros(): void }).carregarCadastros();
    http.expectOne('/api/config/cadastros/inicio').flush({ empresaId: 'a', rotinas: [], sistemas: [], atalhos: [] });
    await fixture.whenStable();
    expect(painel.querySelectorAll('a').length).toBe(0);
  });
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

