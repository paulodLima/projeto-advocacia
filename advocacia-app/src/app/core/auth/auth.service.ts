import { HttpClient } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { tap } from 'rxjs';
import { API_URL } from '../config/api-url.token';
import { Usuario } from '../../features/usuario/models/usuario.model';
import { EscritorioService } from '../escritorio/escritorio.service';
import { PlataformaService } from '../plataforma/plataforma.service';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly escritorios = inject(EscritorioService);
  private readonly plataforma = inject(PlataformaService);
  private readonly url = inject(API_URL).replace(/\/$/, '') + '/api/auth';
  readonly usuario = signal<Usuario | null>(null);
  readonly googleLoginUrl = inject(API_URL).replace(/\/$/, '') + '/oauth2/authorization/google';

  provedores() {
    return this.http.get<{ google: boolean }>(this.url + '/providers');
  }

  solicitarCodigo(email: string) {
    return this.http.post<{ desafioId: string; mensagem: string }>(this.url + '/codigo', { email });
  }

  validarCodigo(desafioId: string, codigo: string) {
    return this.http.post<Usuario>(this.url + '/validar', { desafioId, codigo })
      .pipe(tap((usuario) => this.usuario.set(usuario)));
  }

  cadastroPendente() {
    return this.http.get<{ email: string }>(this.url + '/cadastro');
  }

  concluirCadastro(nome: string) {
    return this.http.post<Usuario>(this.url + '/cadastro', { nome })
      .pipe(tap((usuario) => this.usuario.set(usuario)));
  }

  carregarSessao() {
    return this.http.get<Usuario>(this.url + '/me').pipe(tap((usuario) => this.usuario.set(usuario)));
  }

  sair() {
    return this.http.post<void>(this.url + '/logout', {}).pipe(tap(() => {
      this.usuario.set(null);
      this.escritorios.ativo.set(null);
      this.plataforma.podeAdministrar.set(false);
    }));
  }
}
