import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../../../core/auth/auth.service';
import { IdentidadeEmpresaService } from '../../../../core/branding/identidade-empresa.service';

@Component({
  selector: 'app-cadastro',
  imports: [FormsModule],
  templateUrl: './cadastro.html',
  styleUrl: './cadastro.scss',
})
export class Cadastro {
  protected readonly marca = inject(IdentidadeEmpresaService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  protected readonly email = signal('');
  protected readonly aviso = signal('');
  protected readonly carregando = signal(false);
  protected nome = '';

  constructor() {
    this.auth.cadastroPendente().subscribe({
      next: (dados) => this.email.set(dados.email),
      error: () => { void this.router.navigateByUrl('/login'); },
    });
  }

  protected cadastrar() {
    if (this.carregando() || !this.email()) return;
    this.aviso.set('');
    this.carregando.set(true);
    this.auth.concluirCadastro(this.nome.trim()).pipe(finalize(() => this.carregando.set(false))).subscribe({
      next: () => { void this.router.navigateByUrl('/inicio'); },
      error: (erro) => {
        if (erro.status === 401) {
          void this.router.navigateByUrl('/login');
          return;
        }
        this.aviso.set(erro.error?.detail || 'Não foi possível criar a conta. Tente novamente.');
      },
    });
  }

  protected cancelar() {
    this.auth.sair().subscribe({ next: () => { void this.router.navigateByUrl('/login'); } });
  }
}
