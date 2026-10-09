import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_URL } from '../../../core/config/api-url.token';
import { LeadCard } from './lead-card';
import { Lead, OpcoesCrm, novoCadastro } from '../models/crm.model';

const opcoes: OpcoesCrm = {
  podeEditar: true,
  podeConverter: true,
  origens: [],
  etiquetas: [],
  membros: [{ id: 'membro', nome: 'Ana', cor: '' }],
  cadencia: [{ passo: 0, dias: 0, rotulo: 'Primeiro contato' }],
};
const lead: Lead = {
  id: 'lead',
  empresaId: 'empresa',
  versao: 3,
  proximo: '2026-10-09',
  dados: {
    cadastro: {
      ...novoCadastro(),
      nome: 'Maria Silva',
      telefone: '61999991234',
      necessidade: 'Inventário',
    },
    estado: {
      status: 'ativo',
      cadenciaPausada: false,
      retornoEm: null,
      consultaAgendada: false,
      consultaEm: null,
      consultaStatus: null,
      qualificacao: {},
    },
    entrada: '2026-10-09',
    ultimoContato: null,
    passo: 0,
    casoId: null,
  },
  contatos: [],
  comentarios: [],
};
describe('Controles de um lead', () => {
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [LeadCard],
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
  async function preparar(podeEditar = true) {
    const f = TestBed.createComponent(LeadCard);
    f.componentRef.setInput('lead', structuredClone(lead));
    f.componentRef.setInput('opcoes', { ...opcoes, podeEditar });
    await f.whenStable();
    return f;
  }
  it('qualifica F.A.C.A. em três estados e envia a versão atual', async () => {
    const f = await preparar();
    const botao = () => f.nativeElement.querySelector('.faca') as HTMLButtonElement;
    for (const esperado of [true, false, null]) {
      botao().click();
      const r = http.expectOne('/api/crm/leads/lead/estado');
      expect(r.request.body.versao).toBe(3);
      expect(r.request.body.estado.qualificacao.fit).toBe(esperado);
      const l = structuredClone(lead);
      l.dados.estado.qualificacao['fit'] = esperado;
      r.flush(l);
      f.componentRef.setInput('lead', l);
      await f.whenStable();
    }
  });
  it('registra contato com o resultado escolhido e não duplica pedidos enquanto salva', async () => {
    const f = await preparar();
    (f.nativeElement.querySelector('.lead-acoes button') as HTMLButtonElement).click();
    await f.whenStable();
    const form = f.nativeElement.querySelector('form.tentativa') as HTMLFormElement;
    const resultado = form.querySelector('select[name=resultado]') as HTMLSelectElement;
    resultado.value = 'Quer agendar consultoria';
    resultado.dispatchEvent(new Event('change'));
    await f.whenStable();
    form.dispatchEvent(new Event('submit', { cancelable: true }));
    form.dispatchEvent(new Event('submit', { cancelable: true }));
    const r = http.expectOne('/api/crm/leads/lead/contatos');
    expect(r.request.body.resultado).toBe('Quer agendar consultoria');
    expect(r.request.body.versao).toBe(3);
    r.flush(lead);
    await f.whenStable();
    expect(f.nativeElement.querySelector('form.tentativa')).toBeNull();
  });
  it('mantém erro de conflito visível e permite recarregar a versão antes de tentar de novo', async () => {
    const f = await preparar();
    (f.nativeElement.querySelector('.faca') as HTMLButtonElement).click();
    http
      .expectOne('/api/crm/leads/lead/estado')
      .flush(
        { detail: 'Lead atualizado em outra sessão.' },
        { status: 409, statusText: 'Conflict' },
      );
    await f.whenStable();
    expect(f.nativeElement.querySelector('[role=alert]').textContent).toContain('outra sessão');
    const reload = [...f.nativeElement.querySelectorAll('button')].find((b: unknown) =>
      (b as HTMLButtonElement).textContent?.includes('Recarregar lead'),
    ) as HTMLButtonElement;
    reload.click();
    http.expectOne('/api/crm/leads/lead').flush({ ...lead, versao: 4 });
    await f.whenStable();
    expect(f.nativeElement.querySelector('[role=alert]')).toBeNull();
  });
  it('consulta sem apresentar controles de escrita quando o perfil é somente leitura', async () => {
    const f = await preparar(false);
    expect(f.nativeElement.textContent).toContain('Maria Silva');
    expect(f.nativeElement.querySelector('.faca')).toBeNull();
    expect(f.nativeElement.textContent).not.toContain('Registrar contato');
    expect(f.nativeElement.querySelector('[aria-label="Excluir lead"]')).toBeNull();
  });
  it('envia menção apenas para o membro selecionado e presente no texto', async () => {
    const f = await preparar();
    const botao = [...f.nativeElement.querySelectorAll('button')].find((b: unknown) =>
      (b as HTMLButtonElement).textContent?.includes('Comentários'),
    ) as HTMLButtonElement;
    botao.click();
    await f.whenStable();
    const textarea = f.nativeElement.querySelector(
      'textarea[name=comentario]',
    ) as HTMLTextAreaElement;
    textarea.value = '@An';
    textarea.dispatchEvent(new Event('input'));
    await f.whenStable();
    (f.nativeElement.querySelector('.mencoes button') as HTMLButtonElement).click();
    await f.whenStable();
    (f.nativeElement.querySelector('.comentarios form') as HTMLFormElement).dispatchEvent(
      new Event('submit', { cancelable: true }),
    );
    const r = http.expectOne('/api/crm/leads/lead/comentarios');
    expect(r.request.body.mencoes).toEqual(['membro']);
    expect(r.request.body.texto).toContain('@Ana');
    r.flush(lead);
  });
});
