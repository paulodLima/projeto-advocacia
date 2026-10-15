import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { API_URL } from '../../../core/config/api-url.token';
import { FichaCaso, FormularioCaso, OpcoesCasos, PaginaCasos } from '../models/caso.model';
@Injectable({ providedIn: 'root' })
export class CasosApiService {
  private readonly http = inject(HttpClient);
  private readonly url = inject(API_URL).replace(/\/$/, '') + '/api/casos';
  opcoes() {
    return this.http.get<OpcoesCasos>(this.url + '/opcoes');
  }
  listar(busca: string, status: string, pagina: number) {
    return this.http.get<PaginaCasos>(this.url, { params: { busca, status, pagina, tamanho: 24 } });
  }
  buscar(id: string) {
    return this.http.get<FichaCaso>(this.url + '/' + id);
  }
  salvar(id: string | null, form: FormularioCaso) {
    return id
      ? this.http.put<FichaCaso>(this.url + '/' + id, form)
      : this.http.post<FichaCaso>(this.url, form);
  }
  comercial(
    id: string,
    versao: number,
    desfecho: string,
    valorFechado: number | null,
    motivoPerda: string,
  ) {
    return this.http.put<FichaCaso>(this.url + '/' + id + '/comercial', {
      versao,
      desfecho,
      valorFechado,
      motivoPerda,
    });
  }
  anotar(id: string, versao: number, data: string, descricao: string) {
    return this.http.post<FichaCaso>(this.url + '/' + id + '/anotacoes', {
      versao,
      data,
      descricao,
    });
  }
  situacao(id: string, versao: number, status: string) {
    return this.http.put<FichaCaso>(this.url + '/' + id + '/situacao', { versao, status });
  }
  excluir(id: string, versao: number) {
    return this.http.delete<void>(this.url + '/' + id, { params: { versao } });
  }
}
