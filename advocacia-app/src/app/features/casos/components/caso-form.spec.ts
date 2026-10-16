import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_URL } from '../../../core/config/api-url.token';
import { CasoForm } from './caso-form';
import { OpcoesCasos, valorMonetario } from '../models/caso.model';

describe('Cadastro de casos', () => {
  let http: HttpTestingController;
  const opcoes: OpcoesCasos = {
    podeEditar: true,
    podeCriarCliente: true,
    clientes: [],
    grupos: [],
    membros: [],
  };
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [CasoForm],
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
  async function preparar() {
    const f = TestBed.createComponent(CasoForm);
    f.componentRef.setInput('opcoes', opcoes);
    await f.whenStable();
    return f;
  }
  it('interpreta reais sem transformar decimal digitado em milhares', () => {
    expect(valorMonetario('1.234,56')).toBe(1234.56);
    expect(valorMonetario('1234,50')).toBe(1234.5);
    expect(valorMonetario('')).toBeNull();
    expect(() => valorMonetario('1234.50')).toThrow();
    expect(() => valorMonetario('-1,00')).toThrow();
  });
  it('envia CPF e telefone sem máscara e preserva o formulário quando a API rejeita', async () => {
    const f = await preparar();
    const mudar = async (nome: string, valor: string) => {
      const el = f.nativeElement.querySelector(`input[name=${nome}]`) as HTMLInputElement;
      el.value = valor;
      el.dispatchEvent(new Event('input'));
      await f.whenStable();
    };
    const botao = [...f.nativeElement.querySelectorAll('button')].find((b: unknown) =>
      (b as HTMLButtonElement).textContent?.includes('novo cliente'),
    ) as HTMLButtonElement;
    botao.click();
    await f.whenStable();
    await mudar('clienteNome', 'Maria Silva');
    await mudar('clienteCpf', '123.456.789-01');
    await mudar('clienteTelefone', '(61) 99999-1234');
    await mudar('titulo', 'Consultoria');
    await mudar('proposta', '1.234,56');
    const form = f.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit', { cancelable: true }));
    form.dispatchEvent(new Event('submit', { cancelable: true }));
    const r = http.expectOne('/api/casos');
    expect(r.request.body.novoCliente.documento).toBe('12345678901');
    expect(r.request.body.novoCliente.telefone).toBe('61999991234');
    expect(r.request.body.valorProposta).toBe(1234.56);
    r.flush({ detail: 'CPF já cadastrado.' }, { status: 409, statusText: 'Conflict' });
    await f.whenStable();
    expect(f.nativeElement.querySelector('[role=alert]').textContent).toContain(
      'CPF já cadastrado',
    );
    expect((f.nativeElement.querySelector('input[name=titulo]') as HTMLInputElement).value).toBe(
      'Consultoria',
    );
    expect(
      (f.nativeElement.querySelector('input[name=clienteTelefone]') as HTMLInputElement).value,
    ).toBe('(61) 99999-1234');
  });
});
