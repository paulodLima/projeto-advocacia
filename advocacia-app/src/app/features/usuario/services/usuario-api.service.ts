import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { API_URL } from '../../../core/config/api-url.token';
import { AtualizarUsuarioRequest } from '../models/atualizar-usuario-request.model';
import { CriarUsuarioRequest } from '../models/criar-usuario-request.model';
import { Usuario } from '../models/usuario.model';

@Injectable({ providedIn: 'root' })
export class UsuarioApiService {
  private readonly http = inject(HttpClient);
  private readonly url = inject(API_URL).replace(/\/$/, '') + '/api/usuarios';

  criar(request: CriarUsuarioRequest) {
    return this.http.post<Usuario>(this.url, request);
  }

  buscarPorId(id: string) {
    return this.http.get<Usuario>(this.usuarioUrl(id));
  }

  atualizar(id: string, request: AtualizarUsuarioRequest) {
    return this.http.put<Usuario>(this.usuarioUrl(id), request);
  }

  excluir(id: string) {
    return this.http.delete<void>(this.usuarioUrl(id));
  }

  private usuarioUrl(id: string) {
    return `${this.url}/${encodeURIComponent(id)}`;
  }
}
