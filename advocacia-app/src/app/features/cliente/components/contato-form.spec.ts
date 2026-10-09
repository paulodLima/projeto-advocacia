import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_URL } from '../../../core/config/api-url.token';
import { ContatoForm } from './contato-form';
import { contatoVazio } from '../models/contato.model';

describe('Formulário de contatos', () => {
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ContatoForm],
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
  async function abrir() {
    const f = TestBed.createComponent(ContatoForm);
    f.componentRef.setInput('opcoes', {
      podeEditar: true,
      origens: [],
      parceiros: [],
      indicadores: [],
    });
    await f.whenStable();
    return f;
  }
  function botao(e: HTMLElement, t: string) {
    return Array.from(e.querySelectorAll<HTMLButtonElement>('button')).find(
      (b) => b.textContent?.trim() === t,
    )!;
  }
  it('salva contato com indicadores e preserva formulário quando a API rejeita duplicidade', async () => {
    const f = await abrir();
    const e = f.nativeElement as HTMLElement;
    for (const [nome, valor] of Object.entries({
      nome: 'Contato teste',
      email: 'teste@example.test',
      documento: '12345678901',
      telefone: '61999999999',
    })) {
      const input = e.querySelector<HTMLInputElement>(`input[name="${nome}"]`)!;
      input.value = valor;
      input.dispatchEvent(new Event('input'));
    }
    await f.whenStable();
    e.querySelector('form')!.dispatchEvent(
      new Event('submit', { bubbles: true, cancelable: true }),
    );
    const r = http.expectOne('/api/contatos');
    expect(r.request.body.dados.documento).toBe('12345678901');
    expect(r.request.body.versao).toBeNull();
    r.flush(
      { detail: 'Já existe um contato com esse CPF/CNPJ.' },
      { status: 409, statusText: 'Conflict' },
    );
    await f.whenStable();
    expect(e.textContent).toContain('Já existe um contato');
    expect(e.querySelector<HTMLInputElement>('input[name="nome"]')!.value).toBe('Contato teste');
  });
  it('edita pessoa jurídica mantendo versão e todos os dados dos representantes', async () => {
    const f = await abrir();
    const base = contatoVazio();
    base.dados['tipo_pessoa'] = 'PJ';
    base.dados['nome'] = 'Empresa';
    base.dados['tipo'] = 'Fornecedor';
    base.representantes = [
      {
        nome: 'Sócia',
        cpf: '123.456.789-01',
        cargo: 'Administradora',
        telefone: '',
        rg: '123',
        data_nascimento: '1990-01-01',
        rg_orgao_emissor: 'SSP',
        nacionalidade: 'Brasileira',
        estado_civil: 'Solteiro(a)',
      },
    ];
    f.componentRef.setInput('contato', { ...base, id: 'abc', empresaId: 'empresa', versao: 4 });
    await f.whenStable();
    const e = f.nativeElement as HTMLElement;
    expect(e.textContent).toContain('Representantes');
    e.querySelector('form')!.dispatchEvent(
      new Event('submit', { bubbles: true, cancelable: true }),
    );
    const r = http.expectOne('/api/contatos/abc');
    expect(r.request.method).toBe('PUT');
    expect(r.request.body.versao).toBe(4);
    expect(r.request.body.representantes[0].rg).toBe('123');
    r.flush({ ...base, id: 'abc', empresaId: 'empresa', versao: 5 });
    await f.whenStable();
  });
  it('não sobrescreve endereço digitado durante consulta de CEP e permite entrada manual em falha', async () => {
    const f = await abrir();
    const e = f.nativeElement as HTMLElement;
    const cep = e.querySelector<HTMLInputElement>('input[name="cep"]')!;
    cep.value = '70000000';
    cep.dispatchEvent(new Event('input'));
    cep.dispatchEvent(new Event('blur'));
    const consulta = http.expectOne('https://viacep.com.br/ws/70000000/json/');
    const rua = e.querySelector<HTMLInputElement>('input[name="logradouro"]')!;
    rua.value = 'Rua digitada';
    rua.dispatchEvent(new Event('input'));
    consulta.flush({
      logradouro: 'Rua automática',
      bairro: 'Centro',
      localidade: 'Brasília',
      uf: 'DF',
    });
    await f.whenStable();
    expect(rua.value).toBe('Rua digitada');
    expect(e.querySelector<HTMLInputElement>('input[name="cidade"]')!.value).toBe('Brasília');
    cep.dispatchEvent(new Event('blur'));
    http
      .expectOne('https://viacep.com.br/ws/70000000/json/')
      .flush({}, { status: 503, statusText: 'Unavailable' });
    await f.whenStable();
    expect(e.textContent).toContain('Preencha o endereço manualmente');
    expect(rua.value).toBe('Rua digitada');
  });
  it('cria indicador antes de salvar e impede envio por perfil de consulta', async () => {
    const f = await abrir();
    const e = f.nativeElement as HTMLElement;
    const input = e.querySelector<HTMLInputElement>('input[name="novoIndicador"]')!;
    input.value = 'Mãe';
    input.dispatchEvent(new Event('input'));
    await f.whenStable();
    const emit = f.componentInstance.indicadorCriado.subscribe((i) => expect(i.nome).toBe('Mãe'));
    botao(e, '+ Adicionar').click();
    http.expectOne('/api/contatos/indicadores').flush({ id: 'tag', nome: 'Mãe', cor: '#6A7662' });
    await f.whenStable();
    expect(input.value).toBe('');
    emit.unsubscribe();
    f.componentRef.setInput('opcoes', {
      podeEditar: false,
      origens: [],
      parceiros: [],
      indicadores: [],
    });
    await f.whenStable();
    e.querySelector('form')!.dispatchEvent(
      new Event('submit', { bubbles: true, cancelable: true }),
    );
    http.expectNone('/api/contatos');
  });
});
