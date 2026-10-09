import { HttpClient } from '@angular/common/http';
import { effect, inject, Injectable, signal } from '@angular/core';
import { tap } from 'rxjs';
import { API_URL } from '../../../core/config/api-url.token';
import { AuthService } from '../../../core/auth/auth.service';
import { PerfilLocal } from '../models/configuracoes.model';

export interface MeuPerfil extends PerfilLocal { usuarioId: string; }
export type DadosMeuPerfil = Pick<MeuPerfil, 'telefone' | 'emailPessoal' | 'endereco'>;

@Injectable({ providedIn: 'root' })
export class MeuPerfilApiService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly url = inject(API_URL).replace(/\/$/, '') + '/api/usuarios/me/perfil';
  readonly perfil = signal<MeuPerfil | null>(null);
  constructor() {
    effect(() => {
      const id = this.auth.usuario()?.id;
      const atual = this.perfil();
      if (atual && atual.usuarioId !== id) this.perfil.set(null);
    });
  }
  carregar() { return this.http.get<MeuPerfil>(this.url).pipe(this.receber()); }
  salvar(dados: DadosMeuPerfil) { return this.http.put<MeuPerfil>(this.url, dados).pipe(this.receber()); }
  salvarFoto(foto: string) { return this.http.put<MeuPerfil>(this.url + '/foto', { foto }).pipe(this.receber()); }
  removerFoto() { return this.http.delete<MeuPerfil>(this.url + '/foto').pipe(this.receber()); }
  private receber() {
    return tap<MeuPerfil>(perfil => {
      if (perfil.usuarioId === this.auth.usuario()?.id) this.perfil.set(perfil);
    });
  }
}
