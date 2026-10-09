import {
  afterNextRender,
  Component,
  DestroyRef,
  ElementRef,
  Injector,
  ViewChild,
  inject,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import {
  Subject,
  catchError,
  debounceTime,
  distinctUntilChanged,
  map,
  of,
  switchMap,
  tap,
} from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Icone } from '../../../shared/components/icone/icone';
import { enderecoContato, iniciaisContato } from '../../cliente/models/contato.model';
import { ParceiroForm } from '../components/parceiro-form';
import {
  AREAS_PARCEIRO,
  CarteiraParceiro,
  OpcoesParceiros,
  PaginaParceiros,
  Parceiro,
} from '../models/parceiro.model';
import { ParceirosApiService } from '../services/parceiros-api.service';

@Component({
  selector: 'app-parceiros',
  imports: [FormsModule, RouterLink, Icone, ParceiroForm],
  templateUrl: './parceiros.html',
  styles: ':host{display:block;min-width:0}',
})
export class Parceiros {
  private readonly api = inject(ParceirosApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly injector = inject(Injector);
  @ViewChild('novo') private dialog?: ElementRef<HTMLDialogElement>;
  protected readonly opcoes = signal<OpcoesParceiros | null>(null);
  protected readonly resultado = signal<PaginaParceiros | null>(null);
  protected readonly parceiro = signal<Parceiro | null>(null);
  protected readonly carteira = signal<CarteiraParceiro | null>(null);
  protected readonly erro = signal('');
  protected readonly erroOpcoes = signal('');
  protected readonly erroCarteira = signal('');
  protected readonly carregando = signal(false);
  protected readonly carregandoCarteira = signal(false);
  protected readonly novoAberto = signal(false);
  protected readonly ocupado = signal(false);
  protected readonly excluindo = signal(false);
  protected readonly confirmarExclusao = signal(false);
  protected readonly areas = AREAS_PARCEIRO;
  protected readonly iniciais = iniciaisContato;
  protected readonly endereco = enderecoContato;
  protected readonly editar = this.route.snapshot.data['editar'] === true;
  protected readonly routeParams = this.route.snapshot.queryParams;
  protected id = this.route.snapshot.paramMap.get('id');
  protected busca = this.route.snapshot.queryParamMap.get('busca') || '';
  protected uf = this.route.snapshot.queryParamMap.get('uf') || '';
  protected area = this.route.snapshot.queryParamMap.get('area') || '';
  protected pagina = Math.max(0, Number(this.route.snapshot.queryParamMap.get('pagina')) || 0);
  private readonly pedidos = new Subject<void>();
  private readonly pedidosFicha = new Subject<void>();
  private readonly pedidosCarteira = new Subject<{ id: string; pagina: number }>();
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
          this.api.listar(this.busca, this.uf, this.area, this.pagina).pipe(
            catchError((e) => {
              this.erro.set(e.error?.detail || 'Não foi possível carregar os parceiros.');
              return of(null);
            }),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((r) => {
        this.resultado.set(r);
        this.carregando.set(false);
      });
    this.pedidosCarteira
      .pipe(
        tap(() => {
          this.erroCarteira.set('');
          this.carregandoCarteira.set(true);
        }),
        switchMap((p) =>
          this.api.carteira(p.id, p.pagina).pipe(
            map((c) => ({ id: p.id, carteira: c })),
            catchError((e) => {
              if (p.id === this.id)
                this.erroCarteira.set(e.error?.detail || 'Não foi possível carregar a carteira.');
              return of({ id: p.id, carteira: null });
            }),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((c) => {
        if (c.id !== this.id) return;
        this.carteira.set(c.carteira);
        this.carregandoCarteira.set(false);
      });
    this.pedidosFicha
      .pipe(
        tap(() => {
          this.carregando.set(true);
          this.erro.set('');
        }),
        switchMap(() =>
          this.id
            ? this.api.buscar(this.id).pipe(
                catchError((e) => {
                  this.erro.set(e.error?.detail || 'Não foi possível abrir o parceiro.');
                  return of(null);
                }),
              )
            : of(null),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((p) => {
        this.parceiro.set(p);
        this.carregando.set(false);
        if (p && !this.editar && this.opcoes()?.podeVerContatos) this.carregarCarteira();
      });
    this.route.paramMap
      .pipe(
        distinctUntilChanged((a, b) => a.get('id') === b.get('id')),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((params) => {
        this.id = params.get('id');
        this.parceiro.set(null);
        this.carteira.set(null);
        this.erroCarteira.set('');
        this.carregandoCarteira.set(false);
        this.erro.set('');
        this.confirmarExclusao.set(false);
        this.pedidosFicha.next();
        if (!this.id) this.carregar();
      });
  }
  protected carregarOpcoes() {
    this.erroOpcoes.set('');
    this.api
      .opcoes()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (o) => {
          this.opcoes.set(o);
          if (this.parceiro() && !this.editar && o.podeVerContatos) this.carregarCarteira();
        },
        error: (e) =>
          this.erroOpcoes.set(e.error?.detail || 'Não foi possível consultar as permissões.'),
      });
  }
  protected carregar() {
    this.pedidos.next();
  }
  protected abrirFicha() {
    this.pedidosFicha.next();
  }
  protected filtrar() {
    this.pagina = 0;
    this.carregar();
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: this.filtros(),
      replaceUrl: true,
    });
  }
  protected mudarPagina(delta: number) {
    this.pagina += delta;
    this.carregar();
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: this.filtros(),
      replaceUrl: true,
    });
  }
  protected filtros() {
    return {
      busca: this.busca || null,
      uf: this.uf || null,
      area: this.area || null,
      pagina: this.pagina || null,
    };
  }
  protected abrirNovo() {
    this.novoAberto.set(true);
    this.dialog?.nativeElement.showModal();
    afterNextRender(
      () =>
        this.dialog?.nativeElement.querySelector<HTMLInputElement>('input[name="nome"]')?.focus(),
      { injector: this.injector },
    );
  }
  protected fecharNovo(event?: Event) {
    if (this.ocupado()) {
      event?.preventDefault();
      return;
    }
    this.dialog?.nativeElement.close();
    this.novoAberto.set(false);
  }
  protected salvo(p: Parceiro) {
    this.ocupado.set(false);
    this.fecharNovo();
    void this.router.navigate(['/parceiros', p.id], {
      queryParams: this.route.snapshot.queryParams,
    });
  }
  protected carregarCarteira(pagina = 0) {
    if (this.id) this.pedidosCarteira.next({ id: this.id, pagina });
  }
  protected campos(p: Parceiro) {
    const d = p.dados;
    return [
      [d['tipo_pessoa'] === 'PJ' ? 'CNPJ' : 'CPF', d['documento']],
      ['OAB', d['oab']],
      ['Advogado responsável', d['advogado_responsavel']],
      ['Telefone', d['telefone']],
      ['E-mail', d['email']],
      ['Endereço', enderecoContato(d)],
      ['Site', d['site']],
      ['Instagram', d['instagram']],
    ].filter((c) => c[1]);
  }
  protected excluir() {
    const p = this.parceiro();
    if (!p || this.excluindo() || !this.opcoes()?.podeEditar) return;
    this.excluindo.set(true);
    this.api
      .excluir(p)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () =>
          void this.router.navigate(['/parceiros'], {
            queryParams: this.route.snapshot.queryParams,
          }),
        error: (e) => {
          this.erro.set(e.error?.detail || 'Não foi possível excluir o parceiro.');
          this.excluindo.set(false);
          this.confirmarExclusao.set(false);
        },
      });
  }
}
