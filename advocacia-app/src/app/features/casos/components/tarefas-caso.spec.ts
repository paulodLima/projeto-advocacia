import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_URL } from '../../../core/config/api-url.token';
import { Caso } from '../models/caso.model';
import { novaTarefa, TarefaCaso } from '../models/tarefa-caso.model';
import { TarefasCaso } from './tarefas-caso';

const caso: Caso = {
  id: 'caso',
  versao: 0,
  titulo: 'Consultoria',
  status: 'ativo',
  clienteId: null,
  cliente: null,
  grupoId: null,
  grupo: null,
  responsavelId: null,
  responsavel: null,
  partesAdversas: '',
  observacoes: '',
  valorProposta: null,
  propostaEm: null,
  desfecho: '',
  valorFechado: null,
  motivoPerda: '',
  fechadoEm: null,
  leadId: null,
  criadoEm: '2026-10-09T12:00:00Z',
  atualizadoEm: '2026-10-09T12:00:00Z',
};
const tarefa: TarefaCaso = {
  id: 'tarefa',
  versao: 2,
  status: 'pendente',
  dados: { ...novaTarefa(), titulo: 'Revisar contrato', data: '2026-10-01' },
  tipo: null,
  responsaveis: [],
  concluidaPor: null,
  concluidaEm: null,
  comentarios: [],
};
describe('Tarefas na ficha do caso', () => {
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [TarefasCaso],
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
    vi.restoreAllMocks();
    TestBed.resetTestingModule();
  });
  async function preparar(podeEditar = true, status: 'ativo' | 'encerrado' = 'ativo') {
    const f = TestBed.createComponent(TarefasCaso);
    f.componentRef.setInput('caso', { ...caso, status });
    f.detectChanges();
    http
      .expectOne('/api/casos/caso/tarefas')
      .flush([
        structuredClone(tarefa),
        { ...structuredClone(tarefa), id: 'concluida', status: 'concluida' },
      ]);
    http.expectOne('/api/casos/caso/tarefas/opcoes').flush({
      podeEditar,
      tipos: [],
      membros: [
        { id: 'ana', nome: 'Ana' },
        { id: 'joao', nome: 'João' },
      ],
    });
    await f.whenStable();
    return f;
  }
  it('separa concluídas e bloqueia controles de escrita para leitores e casos encerrados', async () => {
    const f = await preparar(false);
    expect(f.nativeElement.textContent).toContain('Concluídas (1)');
    expect(f.nativeElement.textContent).not.toContain('Nova tarefa');
    (f.nativeElement.querySelector('.tarefa-linha') as HTMLButtonElement).click();
    await f.whenStable();
    expect(f.nativeElement.querySelector('.acoes-tarefa')).toBeNull();
    f.componentRef.setInput('caso', { ...caso, status: 'encerrado' });
    await f.whenStable();
    expect(f.nativeElement.textContent).not.toContain('Nova tarefa');
  });
  it('registra cumprimento com versão e providência, impedindo pedidos duplicados', async () => {
    const f = await preparar();
    (f.nativeElement.querySelector('.tarefa-linha') as HTMLButtonElement).click();
    await f.whenStable();
    const botao = (texto: string) =>
      [...f.nativeElement.querySelectorAll('dialog button')].find(
        (b: unknown) => (b as HTMLButtonElement).textContent?.trim() === texto,
      ) as HTMLButtonElement;
    botao('Realizar').click();
    await f.whenStable();
    const campo = f.nativeElement.querySelector(
      'textarea[placeholder="Anotação opcional"]',
    ) as HTMLTextAreaElement;
    campo.value = 'Contrato revisado';
    campo.dispatchEvent(new Event('input'));
    await f.whenStable();
    botao('Confirmar cumprimento').click();
    botao('Confirmar cumprimento').click();
    const r = http.expectOne('/api/casos/caso/tarefas/tarefa/estado');
    expect(r.request.body).toEqual({
      versao: 2,
      status: 'concluida',
      providencia: 'Contrato revisado',
    });
    r.flush({ ...tarefa, versao: 3, status: 'concluida' });
    http.expectOne('/api/casos/caso/tarefas').flush([]);
    http
      .expectOne('/api/casos/caso/tarefas/opcoes')
      .flush({ podeEditar: true, tipos: [], membros: [] });
    await f.whenStable();
    expect(f.nativeElement.textContent).toContain('Nenhuma tarefa neste caso ainda.');
  });
  it('preserva a anotação ao receber conflito e recarrega a versão sob ação do usuário', async () => {
    const f = await preparar();
    (f.nativeElement.querySelector('.tarefa-linha') as HTMLButtonElement).click();
    await f.whenStable();
    const campo = f.nativeElement.querySelector('textarea[name=comentario]') as HTMLTextAreaElement;
    campo.value = 'Novo comentário';
    campo.dispatchEvent(new Event('input'));
    await f.whenStable();
    (f.nativeElement.querySelector('dialog section form') as HTMLFormElement).dispatchEvent(
      new Event('submit', { cancelable: true }),
    );
    http
      .expectOne('/api/casos/caso/tarefas/tarefa/comentarios')
      .flush(
        { detail: 'Esta tarefa foi alterada em outra sessão.' },
        { status: 409, statusText: 'Conflict' },
      );
    await f.whenStable();
    expect(campo.value).toBe('Novo comentário');
    expect(f.nativeElement.querySelector('[role=alert]').textContent).toContain('outra sessão');
    const b = [...f.nativeElement.querySelectorAll('button')].find((b: unknown) =>
      (b as HTMLButtonElement).textContent?.includes('Recarregar tarefa'),
    ) as HTMLButtonElement;
    b.click();
    http.expectOne('/api/casos/caso/tarefas/tarefa').flush({ ...tarefa, versao: 3 });
    await f.whenStable();
    expect(f.nativeElement.querySelector('[role=alert]')).toBeNull();
    expect(campo.value).toBe('Novo comentário');
  });
});
