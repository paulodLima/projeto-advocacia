import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../../../core/auth/auth.service';
import { IdentidadeEmpresaService } from '../../../../core/branding/identidade-empresa.service';

@Component({
  selector: 'app-login',
  imports: [FormsModule],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class Login {
  protected readonly marca = inject(IdentidadeEmpresaService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  protected readonly aviso = signal('');
  protected readonly carregando = signal(false);
  protected readonly desafio = signal<string | null>(null);
  protected readonly proximoEnvio = signal(0);
  protected email = '';
  protected codigo = '';

  protected solicitarCodigo() {
    if (this.carregando()) return;
    this.aviso.set('');
    this.carregando.set(true);
    this.auth.solicitarCodigo(this.email.trim()).pipe(finalize(() => this.carregando.set(false))).subscribe({
      next: (resposta) => {
        this.desafio.set(resposta.desafioId);
        this.codigo = '';
        this.proximoEnvio.set(Date.now() + 60_000);
        this.aviso.set(resposta.mensagem);
      },
      error: (erro) => this.aviso.set(erro.error?.detail || 'Não foi possível enviar o código. Tente novamente.'),
    });
  }

  protected validarCodigo() {
    const desafioId = this.desafio();
    if (!desafioId || this.carregando()) return;
    this.aviso.set('');
    this.carregando.set(true);
    this.auth.validarCodigo(desafioId, this.codigo).pipe(finalize(() => this.carregando.set(false))).subscribe({
      next: () => { void this.router.navigateByUrl('/inicio'); },
      error: (erro) => this.aviso.set(erro.error?.detail || 'Não foi possível validar o código. Tente novamente.'),
    });
  }

  protected reenviar() {
    if (Date.now() < this.proximoEnvio()) {
      this.aviso.set('Aguarde um minuto entre os envios de código.');
      return;
    }
    this.solicitarCodigo();
  }

  protected trocarEmail() {
    this.desafio.set(null);
    this.codigo = '';
    this.aviso.set('');
  }

  protected entrarComGoogle() {
    this.aviso.set('O acesso com Google estará disponível em breve. Use seu email para receber um código.');
  }
}
