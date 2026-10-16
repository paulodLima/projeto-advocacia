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
import { CurrencyPipe, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Observable, finalize, forkJoin } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Caso, valorMonetario } from '../models/caso.model';
import {
  DespesaCaso,
  FinanceiroCasoFicha,
  FinanceiroCasoOpcoes,
  HonorarioCaso,
  LancamentoCaso,
  MODALIDADES_CASO,
  dividirParcelas,
} from '../models/financeiro-caso.model';
import { hojeTarefa } from '../models/tarefa-caso.model';
import { FinanceiroCasoApiService } from '../services/financeiro-caso-api.service';
@Component({
  selector: 'app-financeiro-caso',
  imports: [FormsModule, CurrencyPipe, DatePipe],
  templateUrl: './financeiro-caso.html',
})
export class FinanceiroCaso {
  readonly caso = input.required<Caso>();
  private readonly casoId = computed(() => this.caso().id);
  private readonly api = inject(FinanceiroCasoApiService);
  private readonly destroy = inject(DestroyRef);
  @ViewChild('painel') private painel?: ElementRef<HTMLDialogElement>;
  protected readonly ficha = signal<FinanceiroCasoFicha | null>(null);
  protected readonly opcoes = signal<FinanceiroCasoOpcoes | null>(null);
  protected readonly ocupado = signal(false);
  protected readonly erro = signal('');
  protected readonly carregando = signal(false);
  protected readonly aberta = signal(false);
  protected readonly rapida = signal(false);
  protected readonly exclusao = signal<{
    tipo: 'honorarios' | 'lancamentos' | 'despesas';
    id: string;
  } | null>(null);
  protected readonly vencimentoEditando = signal<string | null>(null);
  protected novoVencimento = '';
  protected readonly modalidades = MODALIDADES_CASO;
  protected readonly hoje = hojeTarefa();
  protected readonly podeEditar = computed(
    () => !!this.opcoes()?.podeEditar && this.caso().status === 'ativo',
  );
  protected readonly grupos = computed(() => {
    const mapa = new Map<
      string,
      {
        id: string;
        descricao: string;
        direcao: string;
        modalidade: string;
        itens: LancamentoCaso[];
        total: number;
        pagas: number;
      }
    >();
    for (const l of this.ficha()?.lancamentos || []) {
      if (!mapa.has(l.grupo))
        mapa.set(l.grupo, {
          id: l.grupo,
          descricao: l.descricao,
          direcao: l.direcao,
          modalidade: l.modalidade,
          itens: [],
          total: 0,
          pagas: 0,
        });
      const g = mapa.get(l.grupo)!;
      g.itens.push(l);
      g.total += Math.round(l.valor * 100);
      if (l.pago) g.pagas++;
    }
    return [...mapa.values()].map((g) => ({
      ...g,
      total: g.total / 100,
      itens: g.itens.sort((a, b) => a.parcela - b.parcela),
    }));
  });
  protected honorario = {
    tipo: 'contratual',
    valor: '',
    percentual: '',
    base: '',
    observacoes: '',
  };
  protected lancamento = {
    descricao: '',
    valor: '',
    direcao: 'entrada' as 'entrada' | 'saida',
    modalidade: 'boleto',
    parcelas: 1,
    vencimento: hojeTarefa(),
    categoriaId: null as string | null,
    contatoId: null as string | null,
    observacoes: '',
  };
  protected despesa = { descricao: '', valor: '', data: hojeTarefa(), reembolsavel: false };
  constructor() {
    effect((onCleanup) => {
      const id = this.casoId();
      this.carregando.set(true);
      const sub = forkJoin({ ficha: this.api.buscar(id), opcoes: this.api.opcoes(id) }).subscribe({
        next: (r) => {
          this.ficha.set(r.ficha);
          this.opcoes.set(r.opcoes);
          this.carregando.set(false);
        },
        error: (e) => {
          this.falha(e);
          this.carregando.set(false);
        },
      });
      onCleanup(() => sub.unsubscribe());
    });
  }
  private falha(e: { error?: { detail?: string } }) {
    this.erro.set(e.error?.detail || 'Não foi possível carregar ou salvar o financeiro do caso.');
  }
  protected carregar() {
    if (this.ocupado()) return;
    this.erro.set('');
    this.carregando.set(true);
    forkJoin({ ficha: this.api.buscar(this.casoId()), opcoes: this.api.opcoes(this.casoId()) })
      .pipe(
        takeUntilDestroyed(this.destroy),
        finalize(() => this.carregando.set(false)),
      )
      .subscribe({
        next: (r) => {
          this.ficha.set(r.ficha);
          this.opcoes.set(r.opcoes);
        },
        error: (e) => this.falha(e),
      });
  }
  private executar(req: Observable<FinanceiroCasoFicha>, sucesso?: () => void) {
    if (this.ocupado()) return;
    this.ocupado.set(true);
    this.erro.set('');
    req
      .pipe(
        takeUntilDestroyed(this.destroy),
        finalize(() => this.ocupado.set(false)),
      )
      .subscribe({
        next: (f) => {
          this.ficha.set(f);
          sucesso?.();
        },
        error: (e) => this.falha(e),
      });
  }
  protected calculado() {
    try {
      const base = valorMonetario(this.honorario.base);
      const pct = valorMonetario(this.honorario.percentual);
      return base && pct ? Math.round(base * pct) / 100 : null;
    } catch {
      return null;
    }
  }
  protected adicionarHonorario() {
    try {
      const h = this.honorario;
      this.executar(
        this.api.criar(this.casoId(), 'honorarios', {
          versao: this.ficha()!.versao,
          tipo: h.tipo,
          valor: valorMonetario(h.valor),
          baseCalculo: valorMonetario(h.base),
          percentual: valorMonetario(h.percentual),
          observacoes: h.observacoes,
        }),
        () =>
          (this.honorario = {
            tipo: 'contratual',
            valor: '',
            base: '',
            percentual: '',
            observacoes: '',
          }),
      );
    } catch (e) {
      this.erro.set((e as Error).message);
    }
  }
  protected abrir(direcao: 'entrada' | 'saida') {
    this.rapida.set(false);
    this.lancamento = {
      descricao: '',
      valor: '',
      direcao,
      modalidade: 'boleto',
      parcelas: 1,
      vencimento: hojeTarefa(),
      categoriaId: null,
      contatoId: direcao === 'entrada' ? this.caso().clienteId : null,
      observacoes: '',
    };
    this.erro.set('');
    this.aberta.set(true);
    this.painel?.nativeElement.showModal();
  }
  protected fechar(e?: Event) {
    if (this.ocupado()) {
      e?.preventDefault();
      return;
    }
    this.painel?.nativeElement.close();
    this.aberta.set(false);
  }
  protected cobrancaRapida() {
    this.lancamento = {
      descricao: '',
      valor: '',
      direcao: 'entrada',
      modalidade: 'boleto',
      parcelas: 1,
      vencimento: hojeTarefa(),
      categoriaId: null,
      contatoId: this.caso().clienteId,
      observacoes: '',
    };
    this.rapida.set(!this.rapida());
  }
  protected parcelas() {
    try {
      return dividirParcelas(
        valorMonetario(this.lancamento.valor) || 0,
        Number(this.lancamento.parcelas),
      );
    } catch {
      return [];
    }
  }
  protected categorias() {
    return (
      this.opcoes()?.categorias.filter(
        (c) => c.direcao === 'ambas' || c.direcao === this.lancamento.direcao,
      ) || []
    );
  }
  protected criarLancamento() {
    try {
      this.executar(
        this.api.criar(this.casoId(), 'lancamentos', {
          ...this.lancamento,
          versao: this.ficha()!.versao,
          valor: valorMonetario(this.lancamento.valor),
          parcelas: Number(this.lancamento.parcelas),
        }),
        () => {
          this.ocupado.set(false);
          this.fechar();
          this.rapida.set(false);
        },
      );
    } catch (e) {
      this.erro.set((e as Error).message);
    }
  }
  protected baixa(l: LancamentoCaso) {
    this.executar(
      this.api.alterar(this.casoId(), 'lancamentos', l.id, 'baixa', {
        versao: this.ficha()!.versao,
        pago: !l.pago,
        data: !l.pago ? hojeTarefa() : null,
      }),
    );
  }
  protected titularidade(l: LancamentoCaso) {
    this.executar(
      this.api.alterar(this.casoId(), 'lancamentos', l.id, 'titularidade', {
        versao: this.ficha()!.versao,
        titularidade: l.titularidade === 'cliente' ? 'escritorio' : 'cliente',
      }),
    );
  }
  protected mudarVencimento(l: LancamentoCaso) {
    this.novoVencimento = l.vencimento;
    this.vencimentoEditando.set(l.id);
  }
  protected salvarVencimento(l: LancamentoCaso) {
    this.executar(
      this.api.alterar(this.casoId(), 'lancamentos', l.id, 'vencimento', {
        versao: this.ficha()!.versao,
        data: this.novoVencimento,
      }),
      () => this.vencimentoEditando.set(null),
    );
  }
  protected excluir() {
    const e = this.exclusao();
    if (e)
      this.executar(this.api.excluir(this.casoId(), e.tipo, e.id, this.ficha()!.versao), () =>
        this.exclusao.set(null),
      );
  }
  protected adicionarDespesa() {
    try {
      this.executar(
        this.api.criar(this.casoId(), 'despesas', {
          ...this.despesa,
          versao: this.ficha()!.versao,
          valor: valorMonetario(this.despesa.valor),
        }),
        () =>
          (this.despesa = { descricao: '', valor: '', data: hojeTarefa(), reembolsavel: false }),
      );
    } catch (e) {
      this.erro.set((e as Error).message);
    }
  }
  protected reembolso(d: DespesaCaso) {
    this.executar(
      this.api.alterar(this.casoId(), 'despesas', d.id, 'reembolso', {
        versao: this.ficha()!.versao,
        reembolsada: !d.reembolsada,
      }),
    );
  }
  protected nomeTipo(h: HonorarioCaso) {
    return h.tipo === 'exito' ? 'Êxito' : h.tipo === 'sucumbencia' ? 'Sucumbência' : 'Contratual';
  }
  protected nomeModalidade(id: string) {
    return this.modalidades.find((m) => m.id === id)?.nome || id;
  }
}
