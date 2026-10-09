import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { CrmApiService, NotificacaoCrm } from '../services/crm-api.service';

@Component({
  selector: 'app-crm-notificacoes',
  imports: [RouterLink],
  template: `
    @if (erro()) {
      <p role="alert">
        {{ erro() }} <button type="button" (click)="carregar()">Tentar novamente</button>
      </p>
    }
    @if (carregando()) {
      <p role="status">Lendo notificações…</p>
    }
    @for (n of itens(); track n.id) {
      <a routerLink="/crm" [queryParams]="{ lead: n.lead_id }" (click)="ler(n)"
        ><strong>{{ n.autor }} mencionou você em um lead</strong><span>{{ n.texto }}</span></a
      >
    }
    @if (!carregando() && !erro() && !itens().length) {
      <p>Nenhuma notificação por aqui.</p>
    }
  `,
  styles: `
    :host {
      display: grid;
      gap: 8px;
    }
    a {
      display: grid;
      gap: 4px;
      border-bottom: 1px solid #6b635b24;
      padding: 8px 0;
      color: inherit;
      text-decoration: none;
      font-size: 12px;
    }
    span {
      overflow-wrap: anywhere;
      white-space: pre-wrap;
    }
    p {
      font-size: 12px;
    }
    button {
      font: inherit;
    }
  `,
})
export class CrmNotificacoes {
  private readonly api = inject(CrmApiService);
  private readonly destroy = inject(DestroyRef);
  protected readonly itens = signal<NotificacaoCrm[]>([]);
  protected readonly erro = signal('');
  protected readonly carregando = signal(false);
  constructor() {
    this.carregar();
  }
  protected carregar() {
    this.carregando.set(true);
    this.erro.set('');
    this.api
      .notificacoes()
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (n) => {
          this.itens.set(n);
          this.carregando.set(false);
        },
        error: () => {
          this.erro.set('Não foi possível carregar as notificações.');
          this.carregando.set(false);
        },
      });
  }
  protected ler(n: NotificacaoCrm) {
    this.api
      .lerNotificacao(n.id)
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: () => this.itens.update((itens) => itens.filter((i) => i.id !== n.id)),
        error: () => this.erro.set('Não foi possível marcar a notificação como lida.'),
      });
  }
}
