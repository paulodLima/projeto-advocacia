import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { PlataformaService } from '../../core/plataforma/plataforma.service';

@Component({
  selector: 'app-admin-painel',
  imports: [RouterLink],
  template: `
    <div class="pagina-admin">
      <header>
        <a class="marca" routerLink="/administracao">Gestão Advocacia <span>Administração da plataforma</span></a>
        <div class="conta"><span>{{ auth.usuario()?.nome }}</span>
          <button type="button" (click)="sair()" [disabled]="saindo()">Sair</button></div>
      </header>
      <main>
        <p class="contexto">PLATAFORMA</p>
        <h1>Administração da plataforma</h1>
        <p class="introducao">Controle o acesso das empresas e dos seus usuários.</p>
        @if (erro()) { <p role="alert">{{ erro() }}</p> }
        @if (carregando()) { <p role="status">Carregando administração…</p> }
        @if (pronto()) {
          <div class="modulos">
            <article><h2>Empresas</h2><p>Cadastre empresas e acompanhe sua situação de acesso.</p><a class="empresas" routerLink="/administracao/empresas">Gerenciar empresas</a></article>
            <article><h2>Funcionalidades</h2><p>Defina os módulos disponíveis para cada empresa.</p><span>Em preparação</span></article>
            <article><h2>Usuários</h2><p>Cadastre usuários e gerencie seus vínculos com as empresas.</p><span>Em preparação</span></article>
          </div>
          <a class="empresas" routerLink="/escritorios">Acessar minhas empresas</a>
        }
      </main>
    </div>
  `,
  styleUrl: './admin-painel.scss',
})
export class AdminPainel {
  protected readonly auth = inject(AuthService);
  private readonly plataforma = inject(PlataformaService);
  private readonly router = inject(Router);
  protected readonly carregando = signal(true);
  protected readonly pronto = signal(false);
  protected readonly saindo = signal(false);
  protected readonly erro = signal('');
  constructor() {
    this.plataforma.carregarPainel().pipe(finalize(() => this.carregando.set(false))).subscribe({
      next: () => this.pronto.set(true),
      error: () => this.erro.set('Não foi possível acessar a administração. Entre novamente ou confira sua autorização.'),
    });
  }
  protected sair() {
    if (this.saindo()) return;
    this.saindo.set(true);
    this.auth.sair().pipe(finalize(() => this.saindo.set(false))).subscribe({
      next: () => { void this.router.navigateByUrl('/login'); },
      error: () => this.erro.set('Não foi possível sair. Tente novamente.'),
    });
  }
}
