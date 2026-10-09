import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { API_URL } from '../../../core/config/api-url.token';
import {
  Parceiro,
  ParceiroFormulario,
  PaginaParceiros,
  OpcoesParceiros,
  CarteiraParceiro,
} from '../models/parceiro.model';
@Injectable({ providedIn: 'root' })
export class ParceirosApiService {
  private readonly http = inject(HttpClient);
  private readonly url = inject(API_URL).replace(/\/$/, '') + '/api/parceiros';
  listar(busca = '', uf = '', area = '', pagina = 0) {
    return this.http.get<PaginaParceiros>(this.url, {
      params: { busca, uf, area, pagina, tamanho: 24 },
    });
  }
  opcoes() {
    return this.http.get<OpcoesParceiros>(this.url + '/opcoes');
  }
  buscar(id: string) {
    return this.http.get<Parceiro>(this.url + '/' + id);
  }
  carteira(id: string, pagina = 0) {
    return this.http.get<CarteiraParceiro>(this.url + '/' + id + '/carteira', {
      params: { pagina, tamanho: 24 },
    });
  }
  salvar(f: ParceiroFormulario, id?: string) {
    return id
      ? this.http.put<Parceiro>(this.url + '/' + id, f)
      : this.http.post<Parceiro>(this.url, f);
  }
  excluir(p: Parceiro) {
    return this.http.delete<void>(this.url + '/' + p.id, { params: { versao: p.versao } });
  }
}
