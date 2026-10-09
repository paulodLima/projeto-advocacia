import { formatarNumero } from '../../../shared/directives/mascara.directive';
import { Component, DestroyRef, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Observable, Subject, catchError, finalize, of, switchMap } from 'rxjs';
import { Icone } from '../../../shared/components/icone/icone';
import {
  Campanha,
  Destinatario,
  Manutencao,
  Marketing,
  MESES,
  OpcoesCrm,
  dataCrm,
  diasRetorno,
  hojeCrm,
} from '../models/crm.model';
import { CrmApiService } from '../services/crm-api.service';

@Component({
  selector: 'app-marketing-sazonal',
  imports: [FormsModule, Icone],
  templateUrl: './marketing-sazonal.html',
})
export class MarketingSazonal {
  protected readonly mascarar = formatarNumero;
  readonly opcoes = input.required<OpcoesCrm>();
  private readonly api = inject(CrmApiService);
  private readonly destroy = inject(DestroyRef);
  private readonly pedidos = new Subject<void>();
  protected readonly dados = signal<Marketing | null>(null);
  protected readonly carregando = signal(false);
  protected readonly erro = signal('');
  protected readonly ocupado = signal(false);
  protected readonly criando = signal(false);
  protected readonly excluirId = signal<string | null>(null);
  protected readonly meses = MESES;
  protected readonly data = dataCrm;
  protected readonly ano = Number(hojeCrm().slice(0, 4));
  protected campanha: Pick<Campanha, 'nome' | 'mes' | 'cor' | 'descricao' | 'etiquetas'> = {
    nome: '',
    mes: null,
    cor: '#6d7462',
    descricao: '',
    etiquetas: [],
  };
  constructor() {
    this.pedidos
      .pipe(
        switchMap(() => {
          this.carregando.set(true);
          return this.api.marketing(this.ano).pipe(
            catchError((e) => {
              this.erro.set(e.error?.detail || 'Não foi possível carregar as campanhas.');
              return of(null);
            }),
          );
        }),
        takeUntilDestroyed(this.destroy),
      )
      .subscribe((d) => {
        this.dados.set(d);
        this.carregando.set(false);
      });
    this.carregar();
  }
  protected carregar() {
    this.pedidos.next();
  }
  protected etiqueta(id: string) {
    this.campanha.etiquetas = this.campanha.etiquetas.includes(id)
      ? this.campanha.etiquetas.filter((t) => t !== id)
      : [...this.campanha.etiquetas, id];
  }
  protected criar() {
    this.executar(this.api.criarCampanha(this.campanha), () => {
      this.criando.set(false);
      this.campanha = { nome: '', mes: null, cor: '#6d7462', descricao: '', etiquetas: [] };
    });
  }
  protected excluir(c: Campanha) {
    this.executar(this.api.excluirCampanha(c), () => this.excluirId.set(null));
  }
  protected abordar(c: Campanha, d: Destinatario) {
    this.executar(this.api.abordar(c, d.chave, this.ano, !d.abordado));
  }
  protected abordados(c: Campanha) {
    return c.destinatarios.filter((d) => d.abordado).length;
  }
  protected revisao(m: Manutencao) {
    const base = m.ultimo || m.data_entrega;
    if (!base) return null;
    const d = new Date(base.slice(0, 10) + 'T12:00:00Z');
    d.setUTCMonth(d.getUTCMonth() + m.meses);
    return d.toISOString().slice(0, 10);
  }
  protected dias(m: Manutencao) {
    const data = this.revisao(m);
    return data ? diasRetorno(data) : null;
  }
  protected manutencao(m: Manutencao) {
    this.executar(this.api.manutencao(m.caso_id));
  }
  private executar(pedido: Observable<void>, depois?: () => void) {
    if (this.ocupado()) return;
    this.ocupado.set(true);
    this.erro.set('');
    pedido
      .pipe(
        takeUntilDestroyed(this.destroy),
        finalize(() => this.ocupado.set(false)),
      )
      .subscribe({
        next: () => {
          depois?.();
          this.carregar();
        },
        error: (e) => this.erro.set(e.error?.detail || 'Não foi possível salvar. Tente novamente.'),
      });
  }
}
