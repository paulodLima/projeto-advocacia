import { HttpClient } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { tap } from 'rxjs';
import { API_URL } from '../config/api-url.token';

export interface AcessoEscritorio { id: string; nome: string; perfil: 'PROPRIETARIO' | 'MEMBRO'; }

@Injectable({ providedIn: 'root' })
export class EscritorioService {
  private readonly http = inject(HttpClient);
  private readonly url = inject(API_URL).replace(/\/$/, '') + '/api/escritorios';
  readonly ativo = signal<AcessoEscritorio | null>(null);

  listar() { return this.http.get<AcessoEscritorio[]>(this.url); }
  selecionar(id: string) {
    return this.http.put<AcessoEscritorio>(this.url + '/' + encodeURIComponent(id) + '/contexto', {})
      .pipe(tap((acesso) => this.ativo.set(acesso)));
  }
  carregarContexto() {
    return this.http.get<AcessoEscritorio>(this.url + '/contexto')
      .pipe(tap((acesso) => this.ativo.set(acesso)));
  }
}
