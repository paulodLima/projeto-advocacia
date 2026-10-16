import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_URL } from '../../../core/config/api-url.token';
import { MarketingSazonal } from './marketing-sazonal';
import { OpcoesCrm, hojeCrm } from '../models/crm.model';

describe('Campanhas do CRM', () => {
  let http: HttpTestingController;
  const opcoes: OpcoesCrm = {
    podeEditar: true,
    podeConverter: true,
    origens: [],
    etiquetas: [{ id: 'tag', nome: 'Família', cor: '#4f5a49' }],
    membros: [],
    cadencia: [],
  };
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [MarketingSazonal],
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
  it('marca o destinatário para o ano atual e atualiza o progresso a partir do servidor', async () => {
    const f = TestBed.createComponent(MarketingSazonal);
    f.componentRef.setInput('opcoes', opcoes);
    const campanha = {
      id: 'campanha',
      versao: 0,
      nome: 'Dia dos Pais',
      mes: 8,
      cor: '#4f5a49',
      descricao: '',
      etiquetas: ['tag'],
      destinatarios: [
        {
          chave: 'lead:lead',
          tipo: 'Lead',
          nome: 'Maria',
          contato: '61999991234',
          abordado: false,
        },
      ],
    };
    const ano = Number(hojeCrm().slice(0, 4));
    http
      .expectOne((r) => r.url === '/api/crm/campanhas' && r.params.get('ano') === String(ano))
      .flush({ campanhas: [campanha], manutencoes: [], ano });
    await f.whenStable();
    expect(f.nativeElement.textContent).toContain('0 de 1');
    (f.nativeElement.querySelector('.destinatario') as HTMLButtonElement).click();
    const r = http.expectOne('/api/crm/campanhas/campanha/abordagens');
    expect(r.request.body).toEqual({ destinatario: 'lead:lead', ano, abordado: true });
    r.flush(null);
    http
      .expectOne((r) => r.url === '/api/crm/campanhas')
      .flush({
        campanhas: [
          { ...campanha, destinatarios: [{ ...campanha.destinatarios[0], abordado: true }] },
        ],
        manutencoes: [],
        ano,
      });
    await f.whenStable();
    expect(f.nativeElement.textContent).toContain('1 de 1');
    expect(f.nativeElement.querySelector('.destinatario').getAttribute('aria-pressed')).toBe(
      'true',
    );
  });
  it('expõe falha de carregamento em vez de apresentar uma campanha vazia', async () => {
    const f = TestBed.createComponent(MarketingSazonal);
    f.componentRef.setInput('opcoes', opcoes);
    http
      .expectOne((r) => r.url === '/api/crm/campanhas')
      .flush({ detail: 'Serviço indisponível' }, { status: 503, statusText: 'Unavailable' });
    await f.whenStable();
    expect(f.nativeElement.querySelector('[role=alert]').textContent).toContain(
      'Serviço indisponível',
    );
    expect(f.nativeElement.textContent).not.toContain('Nenhuma campanha cadastrada');
  });
});
