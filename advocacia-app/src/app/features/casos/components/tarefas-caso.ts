import {
  Component,
  DestroyRef,
  ElementRef,
  ViewChild,
  computed,
  effect,
  inject,
  input,
  signal,
} from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Observable, finalize, forkJoin } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Icone } from '../../../shared/components/icone/icone';
import { Caso } from '../models/caso.model';
import {
  DadosTarefaCaso,
  OpcoesTarefasCaso,
  TarefaCaso,
  hojeTarefa,
  novaTarefa,
} from '../models/tarefa-caso.model';
import { TarefasCasoApiService } from '../services/tarefas-caso-api.service';
@Component({
  selector: 'app-tarefas-caso',
  imports: [FormsModule, DatePipe, Icone],
  templateUrl: './tarefas-caso.html',
})
export class TarefasCaso {
  readonly caso = input.required<Caso>();
  private readonly api = inject(TarefasCasoApiService);
  private readonly destroy = inject(DestroyRef);
  private readonly casoId = computed(() => this.caso().id);
  @ViewChild('painel') private painel?: ElementRef<HTMLDialogElement>;
  protected readonly tarefas = signal<TarefaCaso[]>([]);
  protected readonly opcoes = signal<OpcoesTarefasCaso | null>(null);
  protected readonly responsaveisInativos = computed(() =>
    (this.selecionada()?.responsaveis || []).filter(
      (m) => !this.opcoes()?.membros.some((ativo) => ativo.id === m.id),
    ),
  );
  protected readonly pendentes = computed(() =>
    this.tarefas().filter((t) => t.status === 'pendente'),
  );
  protected readonly concluidas = computed(() =>
    this.tarefas().filter((t) => t.status === 'concluida'),
  );
  protected readonly podeEditar = computed(
    () => !!this.opcoes()?.podeEditar && this.caso().status === 'ativo',
  );
  protected readonly carregando = signal(false);
  protected readonly ocupado = signal(false);
  protected readonly erro = signal('');
  protected readonly selecionada = signal<TarefaCaso | null>(null);
  protected readonly aberto = signal(false);
  protected readonly editando = signal(false);
  protected readonly acao = signal('');
  protected readonly flags = ['Importante', 'Urgente', 'Recorrente', 'Privada'];
  protected readonly tipoAtual = (tipo: { id: string }) =>
    tipo.id === this.selecionada()?.dados.tipoId;
  protected form: DadosTarefaCaso = novaTarefa();
  protected delegados: string[] = [];
  protected adiarData = '';
  protected providencia = '';
  protected comentario = '';
  constructor() {
    effect((onCleanup) => {
      const id = this.casoId();
      this.carregando.set(true);
      const sub = forkJoin({ tarefas: this.api.listar(id), opcoes: this.api.opcoes(id) }).subscribe(
        {
          next: (r) => {
            this.tarefas.set(r.tarefas);
            this.opcoes.set(r.opcoes);
            this.carregando.set(false);
          },
          error: (e) => {
            this.falha(e);
            this.carregando.set(false);
          },
        },
      );
      onCleanup(() => sub.unsubscribe());
    });
  }
  protected carregar() {
    if (this.ocupado()) return;
    this.carregando.set(true);
    this.erro.set('');
    forkJoin({ tarefas: this.api.listar(this.casoId()), opcoes: this.api.opcoes(this.casoId()) })
      .pipe(
        takeUntilDestroyed(this.destroy),
        finalize(() => this.carregando.set(false)),
      )
      .subscribe({
        next: (r) => {
          this.tarefas.set(r.tarefas);
          this.opcoes.set(r.opcoes);
        },
        error: (e) => this.falha(e),
      });
  }
  private falha(e: { error?: { detail?: string } }) {
    this.erro.set(e.error?.detail || 'Não foi possível carregar ou salvar a tarefa.');
  }
  protected abrir(t: TarefaCaso | null) {
    this.selecionada.set(t);
    this.form = t ? structuredClone(t.dados) : novaTarefa();
    this.delegados = t ? [...t.dados.responsaveis] : [];
    this.providencia = '';
    this.comentario = '';
    this.adiarData = '';
    this.acao.set('');
    this.erro.set('');
    this.editando.set(!t);
    this.aberto.set(true);
    this.painel?.nativeElement.showModal();
  }
  protected fechar(e?: Event) {
    if (this.ocupado()) {
      e?.preventDefault();
      return;
    }
    this.painel?.nativeElement.close();
    this.aberto.set(false);
    this.acao.set('');
    this.erro.set('');
  }
  protected editar() {
    this.form = structuredClone(this.selecionada()!.dados);
    this.editando.set(true);
    this.acao.set('');
  }
  protected cancelarEdicao() {
    if (this.selecionada()) this.editando.set(false);
    else this.fechar();
  }
  protected alternar(lista: string[], id: string) {
    const i = lista.indexOf(id);
    if (i < 0) lista.push(id);
    else lista.splice(i, 1);
  }
  protected atrasada(t: TarefaCaso) {
    const prazo = t.dados.prazoFatal || t.dados.data;
    return t.status !== 'concluida' && !!prazo && prazo < hojeTarefa();
  }
  protected nomes(t: TarefaCaso) {
    return t.responsaveis.map((m) => m.nome.split(' ')[0]).join(', ');
  }
  private executar<T>(req: Observable<T>, fechar = true) {
    if (this.ocupado()) return;
    this.ocupado.set(true);
    this.erro.set('');
    req
      .pipe(
        takeUntilDestroyed(this.destroy),
        finalize(() => this.ocupado.set(false)),
      )
      .subscribe({
        next: (r) => {
          if (fechar) {
            this.ocupado.set(false);
            this.fechar();
            this.carregar();
          } else {
            this.selecionada.set(r as TarefaCaso);
            this.comentario = '';
            this.tarefas.update((lista) =>
              lista.map((t) => (t.id === (r as TarefaCaso).id ? (r as TarefaCaso) : t)),
            );
          }
        },
        error: (e) => this.falha(e),
      });
  }
  protected salvar() {
    const t = this.selecionada();
    const dados = {
      ...this.form,
      data: this.form.data || null,
      dataFim: this.form.dataFim || null,
      prazoFatal: this.form.prazoFatal || null,
      hora: this.form.diaInteiro ? null : this.form.hora || null,
      horaFim: this.form.diaInteiro ? null : this.form.horaFim || null,
    };
    this.executar(this.api.salvar(this.casoId(), t?.id ?? null, t?.versao ?? null, dados));
  }
  protected realizar() {
    const t = this.selecionada()!;
    if (t.status === 'concluida') this.executar(this.api.estado(this.casoId(), t, 'pendente'));
    else this.acao.set('realizar');
  }
  protected cumprir() {
    this.executar(
      this.api.estado(this.casoId(), this.selecionada()!, 'concluida', this.providencia),
    );
  }
  protected adiar(dias?: number) {
    const t = this.selecionada()!;
    let data = this.adiarData;
    if (dias) {
      const base = new Date((t.dados.prazoFatal || t.dados.data || hojeTarefa()) + 'T12:00:00');
      base.setDate(base.getDate() + dias);
      data = `${base.getFullYear()}-${String(base.getMonth() + 1).padStart(2, '0')}-${String(base.getDate()).padStart(2, '0')}`;
    }
    if (data) this.executar(this.api.adiar(this.casoId(), t, data));
  }
  protected delegar() {
    this.executar(this.api.delegar(this.casoId(), this.selecionada()!, this.delegados));
  }
  protected excluir() {
    this.executar(this.api.excluir(this.casoId(), this.selecionada()!));
  }
  protected comentar() {
    if (this.comentario.trim())
      this.executar(this.api.comentar(this.casoId(), this.selecionada()!, this.comentario), false);
  }
  protected recarregarTarefa() {
    const t = this.selecionada();
    if (!t || this.ocupado()) return;
    this.ocupado.set(true);
    this.api
      .buscar(this.casoId(), t.id)
      .pipe(
        takeUntilDestroyed(this.destroy),
        finalize(() => this.ocupado.set(false)),
      )
      .subscribe({
        next: (r) => {
          this.selecionada.set(r);
          this.form = structuredClone(r.dados);
          this.delegados = [...r.dados.responsaveis];
          this.erro.set('');
        },
        error: (e) => this.falha(e),
      });
  }
}
