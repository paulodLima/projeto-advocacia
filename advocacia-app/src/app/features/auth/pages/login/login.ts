import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { DOCUMENT } from '@angular/common';
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
  private readonly document = inject(DOCUMENT);
  protected readonly aviso = signal('');
  protected readonly erroAviso = signal(true);
  protected readonly carregando = signal(false);
  protected readonly desafio = signal<string | null>(null);
  protected readonly proximoEnvio = signal(0);
  protected email = '';
  protected codigo = '';

  constructor() {
    const erro = inject(ActivatedRoute).snapshot.queryParamMap.get('erro');
    if (erro === 'google_acesso_negado') {
      this.aviso.set('Esta conta Google não está autorizada. Use o email cadastrado ou fale com o administrador.');
    } else if (erro === 'google_falhou') {
      this.aviso.set('Não foi possível entrar com Google. Tente novamente ou use o código por email.');
    } else if (erro === 'google_indisponivel') {
      this.erroAviso.set(false);
      this.aviso.set('O login Google ainda não está configurado. Use seu email para receber um código.');
    }
  }

  protected solicitarCodigo() {
    if (this.carregando()) return;
    this.aviso.set('');
    this.erroAviso.set(true);
    this.carregando.set(true);
    this.auth.solicitarCodigo(this.email.trim()).pipe(finalize(() => this.carregando.set(false))).subscribe({
      next: (resposta) => {
        this.desafio.set(resposta.desafioId);
        this.codigo = '';
        this.proximoEnvio.set(Date.now() + 60_000);
        this.erroAviso.set(false);
        this.aviso.set(resposta.mensagem);
      },
      error: (erro) => this.aviso.set(erro.error?.detail || 'Não foi possível enviar o código. Tente novamente.'),
    });
  }

  protected validarCodigo() {
    const desafioId = this.desafio();
    if (!desafioId || this.carregando()) return;
    this.aviso.set('');
    this.erroAviso.set(true);
    this.carregando.set(true);
    this.auth.validarCodigo(desafioId, this.codigo).pipe(finalize(() => this.carregando.set(false))).subscribe({
      next: (resultado) => { void this.router.navigateByUrl('cadastroPendente' in resultado ? '/cadastro' : '/inicio'); },
      error: (erro) => this.aviso.set(erro.error?.detail || 'Não foi possível validar o código. Tente novamente.'),
    });
  }

  protected reenviar() {
    if (Date.now() < this.proximoEnvio()) {
      this.erroAviso.set(false);
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
    if (this.carregando()) return;
    this.carregando.set(true);
    this.erroAviso.set(true);
    this.aviso.set('');
    this.auth.provedores().pipe(finalize(() => this.carregando.set(false))).subscribe({
      next: (provedores) => {
        if (provedores.google) {
          this.document.defaultView?.location.assign(this.auth.googleLoginUrl);
        } else {
          this.erroAviso.set(false);
          this.aviso.set('O login Google ainda não está configurado. Use seu email para receber um código.');
        }
      },
      error: () => this.aviso.set('Não foi possível iniciar o login Google. Tente novamente.'),
    });
  }
}
