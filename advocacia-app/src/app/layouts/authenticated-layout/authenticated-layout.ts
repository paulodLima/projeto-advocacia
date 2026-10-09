import { Component, computed, effect, inject, signal } from '@angular/core';
import { MeuPerfilApiService } from '../../features/configuracoes/services/meu-perfil-api.service';
import { EmpresaApiService } from '../../features/configuracoes/services/empresa-api.service';
import { AcessoService } from '../../core/auth/acesso.service';
import { DOCUMENT } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter, finalize } from 'rxjs';
import { toSignal } from '@angular/core/rxjs-interop';
import { AuthService } from '../../core/auth/auth.service';
import { IdentidadeEmpresaService } from '../../core/branding/identidade-empresa.service';
import { IdentidadeApiService } from '../../core/branding/identidade-api.service';
import { CONFIGURACOES, GRUPOS_MENU, INICIO, ITENS_MENU } from '../../core/navigation/menu.model';
import { TemaService } from '../../core/theme/tema.service';
import { Icone } from '../../shared/components/icone/icone';
import { CrmNotificacoes } from '../../features/crm/components/crm-notificacoes';

@Component({
  selector: 'app-authenticated-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, FormsModule, Icone, CrmNotificacoes],
  templateUrl: './authenticated-layout.html',
  styleUrls: ['./authenticated-layout.scss', './menu-lateral.scss', './cabecalho.scss', './responsive.scss'],
})
export class AuthenticatedLayout {
  protected readonly auth = inject(AuthService);
  protected readonly meuPerfil = inject(MeuPerfilApiService);
  protected readonly minhaEmpresa = inject(EmpresaApiService);
  protected readonly acesso = inject(AcessoService);
  private readonly identidadeApi = inject(IdentidadeApiService);
  constructor() {
    effect(onCleanup => {
      const usuario = this.auth.usuario();
      const empresa = this.minhaEmpresa.empresa();
      this.marca.restaurar();
      if (!usuario || !empresa?.id) return;
      const request = this.identidadeApi.carregar().subscribe({ next: resposta => this.marca.receber(resposta), error: () => { /* Mantém a identidade padrão se indisponível. */ } });
      onCleanup(() => { request.unsubscribe(); this.marca.restaurar(); });
    });
    effect(onCleanup => {
      if (!this.auth.usuario()) return;
      const request = this.meuPerfil.carregar().subscribe({ error: () => { /* Perfil pode ser recarregado em Configurações. */ } });
      onCleanup(() => request.unsubscribe());
    });
    effect(onCleanup => {
      if (!this.minhaEmpresa.empresa()) return;
      const request = this.acesso.carregar().subscribe({ error: () => { /* A navegação verifica o acesso novamente. */ } });
      onCleanup(() => request.unsubscribe());
    });
    effect(onCleanup => {
      if (!this.auth.usuario()) return;
      const request = this.minhaEmpresa.carregar().subscribe({ error: () => { /* Os dados podem ser recarregados na aba Empresa. */ } });
      onCleanup(() => request.unsubscribe());
    });
  }
  protected readonly tema = inject(TemaService);
  protected readonly marca = inject(IdentidadeEmpresaService);
  private readonly router = inject(Router);
  private readonly document = inject(DOCUMENT);
  private readonly navigation = toSignal(this.router.events.pipe(filter((event) => event instanceof NavigationEnd)));
  protected readonly grupos = computed(() => GRUPOS_MENU.map(grupo => ({ ...grupo, itens: grupo.itens.filter(item => this.acesso.permite(item.id)) })).filter(grupo => grupo.itens.length));
  protected readonly inicio = INICIO;
  protected readonly config = CONFIGURACOES;
  protected readonly fixado = signal(this.lerPreferencia() === '1');
  protected readonly menuMobile = signal(false);
  protected readonly recolhidos = signal<string[]>([]);
  protected readonly painel = signal<'notificacoes' | 'assistente' | 'atalhos' | null>(null);
  protected readonly saindo = signal(false);
  protected readonly erro = signal('');
  protected busca = '';
  protected readonly titulo = computed(() => {
    this.navigation();
    const id = this.router.url.split('?')[0].split('/')[1];
    return ITENS_MENU.find((item) => item.id === id)?.label || 'Início';
  });
  protected readonly iniciais = computed(() => (this.auth.usuario()?.nome || 'Usuário').trim().split(/\s+/)
    .filter(Boolean).slice(0, 2).map((nome) => nome[0]).join('').toUpperCase());
  protected get resultados() {
    const busca = this.busca.trim().normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();
    return busca ? ITENS_MENU.filter((item) => this.acesso.permite(item.id) && item.label.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().includes(busca)) : [];
  }
  private lerPreferencia() {
    try { return this.document.defaultView?.localStorage.getItem('menu-fixo:' + this.auth.usuario()?.id); }
    catch { return null; }
  }
  protected pularParaConteudo(event: Event) {
    event.preventDefault();
    this.document.getElementById('conteudo')?.focus();
  }
  protected alternarMenu() {
    this.fixado.update((value) => !value);
    try { this.document.defaultView?.localStorage.setItem('menu-fixo:' + this.auth.usuario()?.id, this.fixado() ? '1' : '0'); }
    catch { /* Navegação continua disponível quando o armazenamento está bloqueado. */ }
  }
  protected alternarGrupo(titulo: string) {
    this.recolhidos.update((grupos) => grupos.includes(titulo) ? grupos.filter((grupo) => grupo !== titulo) : [...grupos, titulo]);
  }
  protected fecharMenu() { this.menuMobile.set(false); this.busca = ''; this.painel.set(null); }
  protected alternarPainel(painel: 'notificacoes' | 'assistente' | 'atalhos') {
    this.painel.update((atual) => atual === painel ? null : painel);
  }
  protected sair() {
    if (this.saindo()) return;
    this.saindo.set(true);
    this.erro.set('');
    this.auth.sair().pipe(finalize(() => this.saindo.set(false))).subscribe({
      next: () => { void this.router.navigateByUrl('/login'); },
      error: () => this.erro.set('Não foi possível sair. Tente novamente.'),
    });
  }
}
