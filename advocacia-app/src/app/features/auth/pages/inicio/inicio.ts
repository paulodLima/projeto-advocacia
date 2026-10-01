import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../../core/auth/auth.service';
import { IdentidadeEmpresaService } from '../../../../core/branding/identidade-empresa.service';

@Component({
  selector: 'app-inicio',
  template: `
    <main>
      <section>
        <p class="marca">{{ marca.identidade().nome }}</p>
        <h1>Bem-vindo, {{ auth.usuario()?.nome }}</h1>
        <p>Seu acesso foi validado.</p>
        <p>{{ auth.usuario()?.email }}</p>
        <button type="button" (click)="sair()" [disabled]="saindo()">Sair</button>
        @if (erro()) { <p role="alert">{{ erro() }}</p> }
      </section>
    </main>
  `,
  styles: `
    :host { display: block; color: #2a2723; }
    main { min-height: 100dvh; display: grid; place-items: center; padding: 24px; background: #f7f4ef; }
    section { max-width: 560px; width: 100%; padding: 40px; border-radius: 24px; background: white; box-shadow: 0 12px 40px #3a342d15; }
    .marca { color: #7d6c5e; margin-bottom: 24px; }
    h1 { font-size: 24px; margin-bottom: 16px; }
    p { overflow-wrap: anywhere; }
    button { margin-top: 24px; border: 0; padding: 12px 24px; border-radius: 8px; background: #4f5a49; color: white; font: inherit; cursor: pointer; }
    button:disabled { opacity: .6; }
  `,
})
export class Inicio {
  protected readonly marca = inject(IdentidadeEmpresaService);
  protected readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  protected readonly saindo = signal(false);
  protected readonly erro = signal('');

  protected sair() {
    this.saindo.set(true);
    this.auth.sair().subscribe({
      next: () => { void this.router.navigateByUrl('/login'); },
      error: () => { this.saindo.set(false); this.erro.set('Não foi possível sair. Tente novamente.'); },
    });
  }
}
