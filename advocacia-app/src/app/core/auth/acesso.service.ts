import { HttpClient } from '@angular/common/http';
import { effect, inject, Injectable, signal } from '@angular/core';
import { tap } from 'rxjs';
import { AuthService } from './auth.service';
import { API_URL } from '../config/api-url.token';

export interface AcessoEquipe { empresaId: string | null; papel: 'MASTER' | 'MEMBRO' | null; perfil: string | null; modulos: string[]; enviaDocumento: boolean; }
@Injectable({ providedIn: 'root' })
export class AcessoService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly url = inject(API_URL).replace(/\/$/, '') + '/api/equipe/me';
  private usuarioId = this.auth.usuario()?.id;
  private sequencia = 0;
  readonly acesso = signal<AcessoEquipe | null>(null);
  constructor() {
    effect(() => { const id = this.auth.usuario()?.id; if (id !== this.usuarioId) { this.usuarioId = id; this.acesso.set(null); } });
  }
  carregar() {
    const id = this.auth.usuario()?.id;
    const sequencia = ++this.sequencia;
    return this.http.get<AcessoEquipe>(this.url).pipe(tap(acesso => { if (id === this.auth.usuario()?.id && sequencia === this.sequencia) this.acesso.set(acesso); }));
  }
  permite(modulo: string) { return this.acesso()?.modulos.includes(modulo) ?? ['inicio', 'config'].includes(modulo); }
  permiteConfiguracao(secao: string) { return ['perfil', 'tema', 'empresa'].includes(secao) || (secao === 'senhas' && !!this.acesso()?.empresaId) || this.acesso()?.papel === 'MASTER'; }
}

