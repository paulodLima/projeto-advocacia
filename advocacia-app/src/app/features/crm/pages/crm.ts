import {
  Component,
  DestroyRef,
  ElementRef,
  Injector,
  ViewChild,
  afterNextRender,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject, catchError, debounceTime, of, switchMap, tap } from 'rxjs';
import { Icone } from '../../../shared/components/icone/icone';
import { LeadForm } from '../components/lead-form';
import { LeadCard } from '../components/lead-card';
import { MarketingSazonal } from '../components/marketing-sazonal';
import { Lead, OpcoesCrm, PaginaLeads, STATUS_LEAD, TEMPERATURAS } from '../models/crm.model';
import { CrmApiService, FiltrosCrm } from '../services/crm-api.service';

@Component({
  selector: 'app-crm',
  imports: [FormsModule, Icone, LeadForm, LeadCard, MarketingSazonal],
  templateUrl: './crm.html',
})
export class Crm {
  private readonly api = inject(CrmApiService);
  private readonly destroy = inject(DestroyRef);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly injector = inject(Injector);
  @ViewChild('novo') private novo?: ElementRef<HTMLDialogElement>;
  protected readonly opcoes = signal<OpcoesCrm | null>(null);
  protected readonly resultado = signal<PaginaLeads | null>(null);
  protected readonly erro = signal('');
  protected readonly erroOpcoes = signal('');
  protected readonly carregando = signal(false);
  protected readonly editando = signal<Lead | null>(null);
  protected readonly novoAberto = signal(false);
  protected readonly ocupado = signal(false);
  protected readonly temperaturas = TEMPERATURAS;
  protected readonly status = STATUS_LEAD;
  protected aba = this.route.snapshot.queryParamMap.get('aba') === 'sazonal' ? 'sazonal' : 'leads';
  protected destaque = this.route.snapshot.queryParamMap.get('lead');
  protected filtros: FiltrosCrm = {
    busca: '',
    status: 'ativo',
    temperatura: '',
    origem: '',
    atrasados: false,
    pagina: 0,
  };
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
          this.api.listar(this.filtros).pipe(
            catchError((e) => {
              this.erro.set(e.error?.detail || 'Não foi possível carregar os leads.');
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
        tap((q) => {
          this.aba = q.get('aba') === 'sazonal' ? 'sazonal' : 'leads';
          this.destaque = q.get('lead');
        }),
        switchMap((q) => {
          const id = q.get('lead');
          return id
            ? this.api.buscar(id).pipe(
                catchError((e) => {
                  this.erro.set(e.error?.detail || 'Lead não encontrado.');
                  return of(null);
                }),
              )
            : of(null);
        }),
        takeUntilDestroyed(this.destroy),
      )
      .subscribe((lead) => {
        if (lead) {
          this.filtros = {
            busca: lead.dados.cadastro.nome,
            status: 'todos',
            temperatura: '',
            origem: '',
            atrasados: false,
            pagina: 0,
          };
          this.carregar();
        }
      });
    this.carregar();
  }
  protected carregarOpcoes() {
    this.api
      .opcoes()
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (o) => {
          this.opcoes.set(o);
          this.erroOpcoes.set('');
        },
        error: (e) =>
          this.erroOpcoes.set(
            e.error?.detail || 'Não foi possível consultar as permissões do CRM.',
          ),
      });
  }
  protected carregar() {
    this.pedidos.next();
  }
  protected filtrar() {
    this.filtros.pagina = 0;
    this.carregar();
  }
  protected mudarPagina(delta: number) {
    this.filtros.pagina += delta;
    this.carregar();
  }
  protected mudarAba(aba: string) {
    this.aba = aba;
    this.editando.set(null);
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { aba: aba === 'sazonal' ? 'sazonal' : null, lead: null },
      queryParamsHandling: 'merge',
    });
  }
  protected abrirNovo() {
    this.novoAberto.set(true);
    this.novo?.nativeElement.showModal();
    afterNextRender(
      () => this.novo?.nativeElement.querySelector<HTMLInputElement>('input[name="nome"]')?.focus(),
      { injector: this.injector },
    );
  }
  protected fecharNovo(event?: Event) {
    if (this.ocupado()) {
      event?.preventDefault();
      return;
    }
    this.novo?.nativeElement.close();
    this.novoAberto.set(false);
  }
  protected salvo(lead: Lead) {
    this.ocupado.set(false);
    this.fecharNovo();
    this.editando.set(null);
    this.destaque = lead.id;
    this.filtros = {
      busca: '',
      status: lead.dados.estado.status === 'agendou' ? 'todos' : lead.dados.estado.status,
      temperatura: '',
      origem: '',
      atrasados: false,
      pagina: 0,
    };
    this.carregar();
  }
  protected mudou(lead: Lead) {
    this.resultado.update((r) =>
      r ? { ...r, itens: r.itens.map((l) => (l.id === lead.id ? lead : l)) } : r,
    );
    this.carregar();
  }
  protected indicadores() {
    const i = this.resultado()?.indicadores;
    return i
      ? [
          {
            nome: 'Em cadência',
            apoio: 'leads em acompanhamento',
            valor: i.emCadencia,
            icon: 'users',
            alerta: false,
          },
          {
            nome: 'Contatar hoje ou atrasados',
            apoio: 'aguardando seu retorno',
            valor: i.atrasados,
            icon: 'bell',
            alerta: i.atrasados > 0,
          },
          {
            nome: 'Leads quentes',
            apoio: 'prontos para proposta',
            valor: i.quentes,
            icon: 'crm',
            alerta: false,
          },
          {
            nome: 'Taxa de conversão',
            apoio: i.decididos ? `${i.ganhos} de ${i.decididos} decididos` : 'sem decisões ainda',
            valor: i.conversao == null ? '—' : `${i.conversao}%`,
            icon: 'chart',
            alerta: false,
          },
        ]
      : [];
  }
}
