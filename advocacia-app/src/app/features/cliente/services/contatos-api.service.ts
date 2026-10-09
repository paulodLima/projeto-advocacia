import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { timeout } from 'rxjs';
import { API_URL } from '../../../core/config/api-url.token';
import {
  Contato,
  ContatoFormulario,
  IndicadorContato,
  OpcoesContato,
  PaginaContatos,
} from '../models/contato.model';
@Injectable({ providedIn: 'root' })
export class ContatosApiService {
  private readonly http = inject(HttpClient);
  private readonly url = inject(API_URL).replace(/\/$/, '') + '/api/contatos';
  listar(busca = '', tipo = '', indicador = '', pagina = 0) {
    return this.http.get<PaginaContatos>(this.url, {
      params: { busca, tipo, pagina, tamanho: 24, ...(indicador ? { indicador } : {}) },
    });
  }
  opcoes() {
    return this.http.get<OpcoesContato>(this.url + '/opcoes');
  }
  buscar(id: string) {
    return this.http.get<Contato>(this.url + '/' + id);
  }
  salvar(f: ContatoFormulario, id?: string) {
    return id
      ? this.http.put<Contato>(this.url + '/' + id, f)
      : this.http.post<Contato>(this.url, f);
  }
  excluir(c: Contato) {
    return this.http.delete<void>(this.url + '/' + c.id, { params: { versao: c.versao } });
  }
  indicador(nome: string) {
    return this.http.post<IndicadorContato>(this.url + '/indicadores', { nome });
  }
  cep(cep: string) {
    return this.http
      .get<{ erro?: boolean; logradouro: string; bairro: string; localidade: string; uf: string }>(
        'https://viacep.com.br/ws/' + cep.replace(/\D/g, '') + '/json/',
      )
      .pipe(timeout(5000));
  }
}
