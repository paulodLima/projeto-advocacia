import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { AcessoEscritorio, EscritorioService } from '../../core/escritorio/escritorio.service';

@Component({
  selector: 'app-escritorios',
  template: `
    <section class="escritorios">
      <h2>Seus escritórios</h2>
      <p>Escolha a empresa em que deseja trabalhar.</p>
      @if (erro()) { <p role="alert">{{ erro() }}</p> }
      @if (carregando()) { <p role="status">Carregando escritórios…</p> }
      @else {
        <ul>
          @for (escritorio of lista(); track escritorio.id) {
            <li>
              <div><strong>{{ escritorio.nome }}</strong>
                <small>{{ escritorio.perfil === 'PROPRIETARIO' ? 'Proprietário' : 'Membro' }}</small></div>
              <button type="button" [disabled]="ocupado()" (click)="selecionar(escritorio.id)">Entrar</button>
            </li>
          } @empty { <li>Seu acesso ainda não foi vinculado a uma empresa. Solicite a liberação ao administrador da plataforma.</li> }
        </ul>
        <button type="button" [disabled]="ocupado()" (click)="carregar()">Atualizar lista</button>
      }
    </section>
  `,
  styles: [`
    .escritorios { max-width: 720px; margin: 0 auto; padding: 24px; color: #3d382f; }
    h2, h3 { color: #4f5a49; } p { line-height: 1.6; }
    ul { list-style: none; padding: 0; } li { display: flex; gap: 16px; justify-content: space-between;
      align-items: center; padding: 16px; margin-bottom: 10px; background: #fff; border-radius: 12px; }
    small { display: block; margin-top: 5px; }
    button { font: inherit; border-radius: 8px; padding: 10px 14px; }
    button { border: 0; background: #4f5a49; color: #fff; cursor: pointer; }
    button:disabled { opacity: .6; cursor: default; }
    [role=alert] { color: #9a4b3f; }
  `],
})
export class Escritorios {
  private readonly service = inject(EscritorioService);
  private readonly router = inject(Router);
  protected readonly lista = signal<AcessoEscritorio[]>([]);
  protected readonly erro = signal('');
  protected readonly carregando = signal(false);
  protected readonly ocupado = signal(false);

  constructor() { this.carregar(); }

  protected carregar() {
    this.carregando.set(true);
    this.erro.set('');
    this.service.listar().pipe(finalize(() => this.carregando.set(false))).subscribe({
      next: (lista) => this.lista.set(lista),
      error: () => this.erro.set('Não foi possível carregar os escritórios. Tente atualizar a lista.'),
    });
  }

  protected selecionar(id: string) {
    if (this.ocupado()) return;
    this.ocupado.set(true);
    this.erro.set('');
    this.service.selecionar(id).pipe(finalize(() => this.ocupado.set(false))).subscribe({
      next: () => { void this.router.navigateByUrl('/inicio'); },
      error: () => this.erro.set('Não foi possível entrar. Confira seu vínculo e tente novamente.'),
    });
  }

}
