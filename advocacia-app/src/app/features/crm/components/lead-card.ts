import { formatarNumero } from '../../../shared/directives/mascara.directive';
import {
  Component,
  DestroyRef,
  ElementRef,
  ViewChild,
  computed,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Observable, finalize } from 'rxjs';
import { Icone } from '../../../shared/components/icone/icone';
import { CrmApiService } from '../services/crm-api.service';
import {
  CANAIS,
  EstadoLead,
  FACA,
  Lead,
  OpcoesCrm,
  RESULTADOS,
  TEMPERATURAS,
  dataCrm,
  diasRetorno,
  hojeCrm,
} from '../models/crm.model';

@Component({
  selector: 'app-lead-card',
  imports: [FormsModule, Icone],
  templateUrl: './lead-card.html',
})
export class LeadCard {
  protected readonly mascarar = formatarNumero;
  readonly lead = input.required<Lead>();
  readonly opcoes = input.required<OpcoesCrm>();
  readonly destacado = input(false);
  readonly mudou = output<Lead>();
  readonly excluido = output<void>();
  readonly editar = output<Lead>();
  private readonly api = inject(CrmApiService);
  private readonly destroy = inject(DestroyRef);
  @ViewChild('ganho') private ganho?: ElementRef<HTMLDialogElement>;
  protected readonly ocupado = signal(false);
  protected readonly erro = signal('');
  protected readonly conflito = signal(false);
  protected readonly registrando = signal(false);
  protected readonly comentariosAbertos = signal(false);
  protected readonly excluirAberto = signal(false);
  protected readonly historicoCompleto = signal(false);
  protected readonly temperatura = computed(() =>
    TEMPERATURAS.find((t) => t.id === this.lead().dados.cadastro.temperatura)!,
  );
  protected readonly estado = computed(() => this.lead().dados.estado);
  protected readonly cadastro = computed(() => this.lead().dados.cadastro);
  protected readonly passo = computed(() =>
    this.opcoes().cadencia.find((p) => p.passo === this.lead().dados.passo),
  );
  protected readonly dias = computed(() =>
    this.lead().proximo ? diasRetorno(this.lead().proximo!) : null,
  );
  protected readonly qualificado = computed(
    () => FACA.filter((f) => this.estado().qualificacao[f.campo] === true).length,
  );
  protected readonly avaliacaoCompleta = computed(() =>
    FACA.every((f) => this.estado().qualificacao[f.campo] != null),
  );
  protected readonly faca = FACA;
  protected readonly canais = CANAIS;
  protected readonly resultados = RESULTADOS;
  protected readonly data = dataCrm;
  protected readonly hoje = hojeCrm();
  protected readonly situacoesConsulta = [
    { id: 'marcada', nome: 'Marcada' },
    { id: 'confirmada', nome: 'Confirmada' },
    { id: 'realizada', nome: 'Realizada' },
    { id: 'nao_compareceu', nome: 'Não compareceu' },
  ];
  protected tentativa = {
    data: hojeCrm(),
    canal: CANAIS[0],
    resultado: RESULTADOS[0],
    observacao: '',
  };
  protected comentario = '';
  protected mencoes: string[] = [];
  protected buscaMembro = '';
  protected membrosEncontrados() {
    const q = this.comentario.match(/@([^@\n]*)$/)?.[1]?.toLowerCase();
    return q == null
      ? []
      : this.opcoes()
          .membros.filter((m) => m.nome.toLowerCase().includes(q))
          .slice(0, 6);
  }
  protected mencionar(id: string, nome: string) {
    this.comentario = this.comentario.replace(/@([^@\n]*)$/, '@' + nome + ' ');
    if (!this.mencoes.includes(id)) this.mencoes.push(id);
  }
  protected iniciais() {
    return this.cadastro()
      .nome.split(/\s+/)
      .slice(0, 2)
      .map((n) => n[0])
      .join('')
      .toUpperCase();
  }
  protected rotuloStatus() {
    return (
      {
        ativo: 'Em cadência',
        agendou: 'Agendou consultoria',
        ganho: 'Ganho',
        perdido: 'Perdido',
      } as Record<string, string>
    )[this.estado().status];
  }
  protected rotuloRetorno() {
    const d = this.dias();
    return d == null
      ? ''
      : d < 0
        ? `atrasado ${Math.abs(d)}d`
        : d === 0
          ? 'contatar hoje'
          : `em ${d}d`;
  }
  protected rotuloCadencia() {
    return this.estado().cadenciaPausada
      ? this.estado().retornoEm
        ? 'Retorno marcado'
        : 'Cadência pausada'
      : this.passo()?.rotulo || 'Régua esgotada';
  }
  protected atualizar(patch: Partial<EstadoLead>) {
    this.executar(this.api.estado(this.lead(), { ...this.estado(), ...patch }));
  }
  protected qualificar(campo: string) {
    const valor = this.estado().qualificacao[campo];
    this.atualizar({
      qualificacao: {
        ...this.estado().qualificacao,
        [campo]: valor === true ? false : valor === false ? null : true,
      },
    });
  }
  protected registrar() {
    this.executar(this.api.contato(this.lead(), this.tentativa), () => {
      this.registrando.set(false);
      this.tentativa = {
        data: hojeCrm(),
        canal: CANAIS[0],
        resultado: RESULTADOS[0],
        observacao: '',
      };
    });
  }
  protected comentar() {
    const mencoes = this.mencoes.filter((id) =>
      this.comentario.includes('@' + this.opcoes().membros.find((m) => m.id === id)?.nome),
    );
    this.executar(this.api.comentar(this.lead(), this.comentario, mencoes), () => {
      this.comentario = '';
      this.mencoes = [];
    });
  }
  protected abrirGanho() {
    this.ganho?.nativeElement.showModal();
  }
  protected fecharGanho(event?: Event) {
    if (this.ocupado()) {
      event?.preventDefault();
      return;
    }
    this.ganho?.nativeElement.close();
  }
  protected converter(criarCaso: boolean) {
    this.executar(this.api.converter(this.lead(), criarCaso), () =>
      this.ganho?.nativeElement.close(),
    );
  }
  protected excluir() {
    if (this.ocupado()) return;
    this.ocupado.set(true);
    this.erro.set('');
    this.api
      .excluir(this.lead())
      .pipe(
        takeUntilDestroyed(this.destroy),
        finalize(() => this.ocupado.set(false)),
      )
      .subscribe({ next: () => this.excluido.emit(), error: (e) => this.falha(e) });
  }
  protected recarregar() {
    this.executar(this.api.buscar(this.lead().id));
  }
  private executar(pedido: Observable<Lead>, depois?: () => void) {
    if (this.ocupado()) return;
    this.ocupado.set(true);
    this.erro.set('');
    this.conflito.set(false);
    pedido
      .pipe(
        takeUntilDestroyed(this.destroy),
        finalize(() => this.ocupado.set(false)),
      )
      .subscribe({
        next: (l) => {
          this.mudou.emit(l);
          depois?.();
        },
        error: (e) => this.falha(e),
      });
  }
  private falha(e: { status?: number; error?: { detail?: string } }) {
    this.erro.set(e.error?.detail || 'Não foi possível salvar. Tente novamente.');
    this.conflito.set(e.status === 409);
  }
}
