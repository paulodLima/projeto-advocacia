import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { registerLocaleData } from '@angular/common';
import pt from '@angular/common/locales/pt';
import { TestBed } from '@angular/core/testing';
import { API_URL } from '../../../core/config/api-url.token';
import { Caso } from '../models/caso.model';
import { FinanceiroCasoFicha, dividirParcelas } from '../models/financeiro-caso.model';
import { FinanceiroCaso } from './financeiro-caso';

const caso = { id: 'caso', status: 'ativo', titulo: 'Consultoria', clienteId: null } as Caso;
const ficha: FinanceiroCasoFicha = {
  versao: 3,
  honorarios: [],
  lancamentos: [],
  despesas: [],
  resumo: { honorarios: 0, recebido: 0, aReceber: 0, despesasAReembolsar: 0 },
};
describe('Financeiro na ficha de Casos', () => {
  let http: HttpTestingController;
  beforeEach(() => {
    registerLocaleData(pt);
    TestBed.configureTestingModule({
      imports: [FinanceiroCaso],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: API_URL, useValue: '' },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    Object.defineProperty(HTMLDialogElement.prototype, 'showModal', {
      configurable: true,
      value: vi.fn(),
    });
    Object.defineProperty(HTMLDialogElement.prototype, 'close', {
      configurable: true,
      value: vi.fn(),
    });
  });
  afterEach(() => {
    http.verify();
    Reflect.deleteProperty(HTMLDialogElement.prototype, 'showModal');
    Reflect.deleteProperty(HTMLDialogElement.prototype, 'close');
    TestBed.resetTestingModule();
  });
  async function preparar(status = 'ativo') {
    const f = TestBed.createComponent(FinanceiroCaso);
    f.componentRef.setInput('caso', { ...caso, status });
    f.detectChanges();
    http.expectOne('/api/casos/caso/financeiro').flush(structuredClone(ficha));
    http
      .expectOne('/api/casos/caso/financeiro/opcoes')
      .flush({ podeEditar: status === 'ativo', categorias: [], contatos: [] });
    await f.whenStable();
    return f;
  }
  it('divide centavos sem alterar o total ou criar parcelas de valor zero', () => {
    expect(dividirParcelas(100, 3)).toEqual([33.34, 33.33, 33.33]);
    expect(dividirParcelas(0.02, 3)).toEqual([]);
    expect(dividirParcelas(100, 61)).toEqual([]);
    const parcelas = dividirParcelas(1234.56, 60);
    expect(parcelas.reduce((s, v) => s + Math.round(v * 100), 0)).toBe(123456);
  });
  it('envia honorários em formato numérico e mantém o rascunho em caso de conflito', async () => {
    const f = await preparar();
    const input = async (nome: string, valor: string) => {
      const el = f.nativeElement.querySelector(`input[name=${nome}]`) as HTMLInputElement;
      el.value = valor;
      el.dispatchEvent(new Event('input'));
      await f.whenStable();
    };
    await input('base', '1.234,56');
    await input('percentual', '12,50');
    const form = f.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit', { cancelable: true }));
    form.dispatchEvent(new Event('submit', { cancelable: true }));
    const r = http.expectOne('/api/casos/caso/financeiro/honorarios');
    expect(r.request.body).toMatchObject({
      versao: 3,
      baseCalculo: 1234.56,
      percentual: 12.5,
      tipo: 'contratual',
    });
    r.flush(
      { detail: 'Financeiro atualizado em outra sessão.' },
      { status: 409, statusText: 'Conflict' },
    );
    await f.whenStable();
    expect(f.nativeElement.querySelector('[role=alert]').textContent).toContain('outra sessão');
    expect((f.nativeElement.querySelector('input[name=base]') as HTMLInputElement).value).toBe(
      '1.234,56',
    );
  });
  it('cria cobrança rápida parcelada usando a versão carregada', async () => {
    const f = await preparar();
    const abrir = [...f.nativeElement.querySelectorAll('button')].find((b: unknown) =>
      (b as HTMLButtonElement).textContent?.includes('Cobrança rápida'),
    ) as HTMLButtonElement;
    abrir.click();
    await f.whenStable();
    for (const [nome, valor] of [
      ['descricaoRapida', 'Contrato'],
      ['valorRapida', '100,00'],
      ['parcelasRapida', '3'],
      ['vencimentoRapida', '2027-01-31'],
    ]) {
      const el = f.nativeElement.querySelector(`input[name=${nome}]`) as HTMLInputElement;
      el.value = valor;
      el.dispatchEvent(new Event('input'));
      await f.whenStable();
    }
    const form = f.nativeElement.querySelector('form.cartao') as HTMLFormElement;
    form.dispatchEvent(new Event('submit', { cancelable: true }));
    const r = http.expectOne('/api/casos/caso/financeiro/lancamentos');
    expect(r.request.body).toMatchObject({
      versao: 3,
      descricao: 'Contrato',
      valor: 100,
      parcelas: 3,
      vencimento: '2027-01-31',
      direcao: 'entrada',
    });
    r.flush({ ...ficha, versao: 4 });
    await f.whenStable();
    expect(f.nativeElement.querySelector('form.cartao')).toBeNull();
  });
  it('caso encerrado permite consulta sem controles de escrita', async () => {
    const f = await preparar('encerrado');
    expect(f.nativeElement.textContent).toContain('Honorários combinados');
    expect(f.nativeElement.textContent).not.toContain('Nova entrada');
    expect(f.nativeElement.querySelector('form')).toBeNull();
  });
});
