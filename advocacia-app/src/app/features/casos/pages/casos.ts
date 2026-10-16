import { Component, DestroyRef, ElementRef, ViewChild, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Observable, Subject, catchError, debounceTime, finalize, of, switchMap, tap } from 'rxjs';
import { Icone } from '../../../shared/components/icone/icone';
import { FinanceiroCaso } from '../components/financeiro-caso';
import { TarefasCaso } from '../components/tarefas-caso';
import { CasoForm } from '../components/caso-form';
import { CasosApiService } from '../services/casos-api.service';
import {
  DESFECHOS_CASOS,
  FichaCaso,
  OpcoesCasos,
  PaginaCasos,
  STATUS_CASOS,
  dinheiroCampo,
  valorMonetario,
} from '../models/caso.model';
@Component({
  selector: 'app-casos',
  imports: [FormsModule, RouterLink, CurrencyPipe, DatePipe, Icone, CasoForm, TarefasCaso, FinanceiroCaso],
  templateUrl: './casos.html',
})
export class Casos {
  private readonly api = inject(CasosApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroy = inject(DestroyRef);
  @ViewChild('novo') private novo?: ElementRef<HTMLDialogElement>;
  protected readonly opcoes = signal<OpcoesCasos | null>(null);
  protected readonly resultado = signal<PaginaCasos | null>(null);
  protected readonly ficha = signal<FichaCaso | null>(null);
  protected readonly erro = signal('');
  protected readonly carregando = signal(false);
  protected readonly ocupado = signal(false);
  protected readonly novoAberto = signal(false);
  protected readonly editando = signal(false);
  protected readonly confirmarExcluir = signal(false);
  protected readonly statuses = STATUS_CASOS;
  protected readonly desfechos = DESFECHOS_CASOS;
  protected busca = '';
  protected status = 'ativo';
  protected pagina = 0;
  protected aba = 'dados';
  protected desfecho = '';
  protected valorFechado = '';
  protected motivo = '';
  protected data = new Intl.DateTimeFormat('en-CA', {
    timeZone: 'America/Sao_Paulo',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(new Date());
  protected descricao = '';
  private buscandoOpcoes = false;
  private readonly pedidos = new Subject<void>();
  constructor() {
    this.carregarOpcoes();
    this.pedidos
      .pipe(
        debounceTime(180),
        tap(() => {
          this.carregando.set(true);
          this.erro.set('');
        }),
        switchMap(() =>
          this.api.listar(this.busca, this.status, this.pagina).pipe(
            catchError((e) => {
              this.falha(e);
              return of(null);
            }),
          ),
        ),
        takeUntilDestroyed(this.destroy),
      )
      .subscribe((r) => {
        this.resultado.set(r);
        this.carregando.set(false);
      });
    this.route.queryParamMap
      .pipe(
        tap(() => {
          this.ficha.set(null);
          this.editando.set(false);
          this.aba = this.route.snapshot.queryParamMap.get('aba') === 'tarefas' ? 'tarefas' : 'dados';
          this.confirmarExcluir.set(false);
          this.descricao = '';
          this.erro.set('');
        }),
        switchMap((q) =>
          q.get('id')
            ? this.api.buscar(q.get('id')!).pipe(
                catchError((e) => {
                  this.falha(e);
                  return of(null);
                }),
              )
            : of(null),
        ),
        takeUntilDestroyed(this.destroy),
      )
      .subscribe((f) => {
        if (f) this.receber(f);
        else if (!this.route.snapshot.queryParamMap.get('id')) this.carregar();
      });
  }
  private falha(e: { error?: { detail?: string } }) {
    this.erro.set(e.error?.detail || 'Não foi possível carregar ou salvar o caso.');
  }
  protected get tarefaVinculada(){return this.route.snapshot.queryParamMap.get('tarefa');}
  protected get routeId() {
    return this.route.snapshot.queryParamMap.get('id');
  }
  private carregarOpcoes() {
    if (this.buscandoOpcoes) return;
    this.buscandoOpcoes = true;
    this.api
      .opcoes()
      .pipe(
        takeUntilDestroyed(this.destroy),
        finalize(() => (this.buscandoOpcoes = false)),
      )
      .subscribe({ next: (o) => this.opcoes.set(o), error: (e) => this.falha(e) });
  }
  protected carregar() {
    if (!this.opcoes()) this.carregarOpcoes();
    const id = this.route.snapshot.queryParamMap.get('id');
    if (id) {
      this.api
        .buscar(id)
        .pipe(takeUntilDestroyed(this.destroy))
        .subscribe({ next: (f) => this.receber(f), error: (e) => this.falha(e) });
    } else this.pedidos.next();
  }
  protected filtrar() {
    this.pagina = 0;
    this.carregar();
  }
  protected paginar(delta: number) {
    this.pagina += delta;
    this.carregar();
  }
  protected abrir(id: string | null) {
    void this.router.navigate(['/casos'], { queryParams: id ? { id } : {} });
  }
  protected novoCaso() {
    this.novoAberto.set(true);
    this.novo?.nativeElement.showModal();
  }
  protected fecharNovo(e?: Event) {
    if (this.ocupado()) {
      e?.preventDefault();
      return;
    }
    this.novo?.nativeElement.close();
    this.novoAberto.set(false);
  }
  protected salvo(f: FichaCaso) {
    this.ocupado.set(false);
    this.fecharNovo();
    this.editando.set(false);
    this.receber(f);
    this.abrir(f.caso.id);
  }
  private receber(f: FichaCaso) {
    this.ficha.set(f);
    this.desfecho = f.caso.desfecho;
    this.valorFechado = dinheiroCampo(f.caso.valorFechado);
    this.motivo = f.caso.motivoPerda;
    this.erro.set('');
  }
  private executar(request: Observable<FichaCaso>, receber?: (f: FichaCaso) => void) {
    if (this.ocupado()) return;
    this.ocupado.set(true);
    this.erro.set('');
    request
      .pipe(
        takeUntilDestroyed(this.destroy),
        finalize(() => this.ocupado.set(false)),
      )
      .subscribe({
        next: (f) => {
          this.receber(f);
          receber?.(f);
        },
        error: (e) => this.falha(e),
      });
  }
  protected comercial() {
    const c = this.ficha()!.caso;
    try {
      const valor = this.desfecho === 'fechado' ? valorMonetario(this.valorFechado) : null;
      this.executar(this.api.comercial(c.id, c.versao, this.desfecho, valor, this.motivo));
    } catch (e) {
      this.erro.set((e as Error).message);
    }
  }
  protected anotar() {
    const c = this.ficha()!.caso;
    this.executar(
      this.api.anotar(c.id, c.versao, this.data, this.descricao),
      () => (this.descricao = ''),
    );
  }
  protected situacao() {
    const c = this.ficha()!.caso;
    this.executar(this.api.situacao(c.id, c.versao, c.status === 'ativo' ? 'encerrado' : 'ativo'));
  }
  protected excluir() {
    const c = this.ficha()!.caso;
    if (this.ocupado()) return;
    this.ocupado.set(true);
    this.api
      .excluir(c.id, c.versao)
      .pipe(
        takeUntilDestroyed(this.destroy),
        finalize(() => this.ocupado.set(false)),
      )
      .subscribe({
        next: () => {
          this.confirmarExcluir.set(false);
          this.abrir(null);
        },
        error: (e) => this.falha(e),
      });
  }
  protected nomeDesfecho(id: string) {
    return this.desfechos.find((d) => d.id === id)?.nome || 'em aberto';
  }
}
