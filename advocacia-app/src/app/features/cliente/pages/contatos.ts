import { Component, DestroyRef, ElementRef, ViewChild, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subject, catchError, debounceTime, distinctUntilChanged, of, switchMap, tap } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Icone } from '../../../shared/components/icone/icone';
import { ContatoForm } from '../components/contato-form';
import { ContatosApiService } from '../services/contatos-api.service';
import {
  Contato,
  IndicadorContato,
  OpcoesContato,
  PaginaContatos,
  TIPOS_CONTATO,
  corContato,
  enderecoContato,
  iniciaisContato,
} from '../models/contato.model';

@Component({
  selector: 'app-contatos',
  imports: [FormsModule, RouterLink, DatePipe, Icone, ContatoForm],
  templateUrl: './contatos.html',
  styles: ':host{display:block;min-width:0}',
})
export class Contatos {
  private readonly api = inject(ContatosApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  @ViewChild('novo') private dialog?: ElementRef<HTMLDialogElement>;
  protected readonly opcoes = signal<OpcoesContato | null>(null);
  protected readonly resultado = signal<PaginaContatos | null>(null);
  protected readonly contato = signal<Contato | null>(null);
  protected readonly erro = signal('');
  protected readonly erroOpcoes = signal('');
  protected readonly carregando = signal(false);
  protected readonly novoAberto = signal(false);
  protected readonly excluindo = signal(false);
  protected readonly confirmarExclusao = signal(false);
  protected readonly tipos = TIPOS_CONTATO;
  protected readonly tiposFiltro = ['Todos', ...TIPOS_CONTATO];
  protected readonly Boolean = Boolean;
  protected readonly cor = corContato;
  protected readonly iniciais = iniciaisContato;
  protected readonly endereco = enderecoContato;
  protected busca = '';
  protected tipo = '';
  protected indicador = '';
  protected pagina = 0;
  protected visual = 'grade';
  protected readonly editar = this.route.snapshot.data['editar'] === true;
  protected id = this.route.snapshot.paramMap.get('id');
  private readonly pedidos = new Subject<number>();
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
          this.api.listar(this.busca, this.tipo, this.indicador, this.pagina).pipe(
            catchError((e) => {
              this.erro.set(e.error?.detail || 'Não foi possível carregar os contatos.');
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
    if (!this.id) {
      const q = this.route.snapshot.queryParamMap;
      this.busca = q.get('busca') || '';
      this.tipo = q.get('tipo') || '';
      this.indicador = q.get('indicador') || '';
      this.visual = q.get('visual') === 'lista' ? 'lista' : 'grade';
      this.pagina = Math.max(0, Number(q.get('pagina')) || 0);
      this.carregar();
    }
    this.route.paramMap
      .pipe(
        distinctUntilChanged((a, b) => a.get('id') === b.get('id')),
        switchMap((params) => {
          this.id = params.get('id');
          this.contato.set(null);
          this.confirmarExclusao.set(false);
          if (!this.id) return of(null);
          this.carregando.set(true);
          this.erro.set('');
          return this.api.buscar(this.id).pipe(
            catchError((e) => {
              this.erro.set(e.error?.detail || 'Não foi possível abrir o contato.');
              return of(null);
            }),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((c) => {
        if (this.id) {
          this.contato.set(c);
          this.carregando.set(false);
        }
      });
  }
  protected carregarOpcoes() {
    this.erroOpcoes.set('');
    this.api
      .opcoes()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (o) => this.opcoes.set(o),
        error: (e) =>
          this.erroOpcoes.set(e.error?.detail || 'Não foi possível carregar os cadastros.'),
      });
  }
  protected carregar() {
    this.pedidos.next(0);
  }
  protected filtrar() {
    this.pagina = 0;
    this.carregar();
    this.salvarFiltros();
  }
  protected mudarTipo(tipo: string) {
    this.tipo = tipo;
    this.filtrar();
  }
  protected mudarIndicador(id: string) {
    this.indicador = this.indicador === id ? '' : id;
    this.filtrar();
  }
  protected mudarVisual(visual: string) {
    this.visual = visual;
    this.salvarFiltros();
  }
  protected mudarPagina(delta: number) {
    this.pagina += delta;
    this.carregar();
    this.salvarFiltros();
  }
  protected filtros() {
    return {
      busca: this.busca || null,
      tipo: this.tipo || null,
      indicador: this.indicador || null,
      visual: this.visual,
      pagina: this.pagina || null,
    };
  }
  private salvarFiltros() {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: this.filtros(),
      replaceUrl: true,
    });
  }
  protected abrirFicha() {
    this.carregando.set(true);
    this.erro.set('');
    this.api
      .buscar(this.id!)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (c) => {
          this.contato.set(c);
          this.carregando.set(false);
        },
        error: (e) => {
          this.erro.set(e.error?.detail || 'Não foi possível abrir o contato.');
          this.carregando.set(false);
        },
      });
  }
  protected abrirNovo() {
    if (!this.opcoes()?.podeEditar) return;
    this.novoAberto.set(true);
    this.dialog?.nativeElement.showModal();
  }
  protected fecharNovo() {
    this.dialog?.nativeElement.close();
    this.novoAberto.set(false);
  }
  protected salvo(c: Contato) {
    this.fecharNovo();
    void this.router.navigate(['/contatos', c.id], {
      queryParams: this.route.snapshot.queryParams,
    });
  }
  protected novoIndicador(i: IndicadorContato) {
    this.opcoes.update((o) =>
      o ? { ...o, indicadores: [...o.indicadores.filter((x) => x.id !== i.id), i] } : o,
    );
  }
  protected nomeIndicador(id: string) {
    return this.opcoes()?.indicadores.find((i) => i.id === id);
  }
  protected nomeOrigem(id: string) {
    return this.opcoes()?.origens.find((o) => o.id === id)?.nome || '';
  }
  protected nomeParceiro(id: string) {
    return this.opcoes()?.parceiros.find((o) => o.id === id)?.nome || '';
  }
  protected camposFicha(c: Contato) {
    const d = c.dados;
    return [
      ...(d['tipo_pessoa'] === 'PJ' ? [['Nome fantasia', d['nome_fantasia']]] : []),
      [d['tipo_pessoa'] === 'PJ' ? 'CNPJ' : 'CPF', d['documento']],
      ...(d['tipo_pessoa'] === 'PF'
        ? [
            ['RG', [d['rg'], d['rg_orgao_emissor']].filter(Boolean).join(' ')],
            ['Data de nascimento', d['data_nascimento']],
            ['Nacionalidade', d['nacionalidade']],
            ['Estado civil', d['estado_civil']],
            ['Profissão', d['profissao']],
          ]
        : []),
      [
        'Telefone',
        d['telefone'] + (d['telefone'] && d['whatsapp'] === 'telefone' ? ' (WhatsApp)' : ''),
      ],
      ...(d['telefone2']
        ? [['2º telefone', d['telefone2'] + (d['whatsapp'] === 'telefone2' ? ' (WhatsApp)' : '')]]
        : []),
      ['E-mail', d['email']],
      ['Endereço', enderecoContato(d)],
      ['Origem', this.nomeOrigem(d['origem_id'])],
      [
        'Carteira',
        d['carteira'] === 'parceiro'
          ? this.nomeParceiro(d['carteira_parceiro_id'])
          : 'Nossa carteira',
      ],
      ['Tipo', d['tipo']],
    ];
  }
  protected excluir() {
    const c = this.contato();
    if (!c || this.excluindo() || !this.opcoes()?.podeEditar) return;
    this.excluindo.set(true);
    this.api
      .excluir(c)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () =>
          void this.router.navigate(['/contatos'], {
            queryParams: this.route.snapshot.queryParams,
          }),
        error: (e) => {
          this.erro.set(
            e.error?.detail || 'Não foi possível excluir. Verifique os vínculos do contato.',
          );
          this.excluindo.set(false);
          this.confirmarExclusao.set(false);
        },
      });
  }
}
