import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_URL } from '../config/api-url.token';
import { IdentidadeApiService } from './identidade-api.service';
import { IdentidadeEmpresaService } from './identidade-empresa.service';

describe('Identidade visual do servidor', () => {
  let http: HttpTestingController;
  const visual = { imagens: { foto_login: 'foto', logo_simples_branca: 'branca', logo_completa_marrom: 'marrom' },
    usos: { login: 'logo_completa_marrom', menu_expandido: 'logo_simples_branca', menu_recolhido: 'logo_completa_marrom' }, zoom: 1.7, posX: 20, posY: 70 };
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), { provide: API_URL, useValue: '' }] });
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => { http.verify(); TestBed.resetTestingModule(); });
  it('carrega e salva a empresa da sessão sem aceitar ID arbitrário na escrita', () => {
    const api = TestBed.inject(IdentidadeApiService);
    api.carregar().subscribe();
    http.expectOne('/api/empresas/minha/identidade').flush({ empresaId: 'a', nome: 'A', identidade: visual });
    api.salvar(visual).subscribe(resposta => expect(resposta.identidade.zoom).toBe(1.7));
    const req = http.expectOne('/api/empresas/minha/identidade');
    expect(req.request.method).toBe('PUT'); expect(req.request.body).toEqual(visual);
    req.flush({ empresaId: 'a', nome: 'A', identidade: visual });
  });
  it('aplica logos distintas e o recorte, e restaura os padrões ao sair', () => {
    const marca = TestBed.inject(IdentidadeEmpresaService);
    marca.receber({ empresaId: 'a', nome: 'Empresa A', identidade: visual });
    expect(marca.identidade()).toMatchObject({ nome: 'Empresa A', logoUrl: 'marrom', logoExpandidoUrl: 'branca', logoRecolhidoUrl: 'marrom', imagemLoginUrl: 'foto', zoom: 1.7, posX: 20, posY: 70 });
    marca.restaurar();
    expect(marca.identidade()).toMatchObject({ nome: 'Gestão Advocacia', logoUrl: '/images/logo-ga.svg', zoom: 1 });
  });
  it('consulta a identidade pública apenas pelo link do escritório', () => {
    TestBed.inject(IdentidadeApiService).publica('empresa-a').subscribe();
    const req = http.expectOne('/api/public/empresas/empresa-a/identidade');
    expect(req.request.method).toBe('GET'); req.flush({ empresaId: 'empresa-a', nome: 'A', identidade: visual });
  });
});
