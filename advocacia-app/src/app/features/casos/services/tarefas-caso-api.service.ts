import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { API_URL } from '../../../core/config/api-url.token';
import { DadosTarefaCaso, OpcoesTarefasCaso, TarefaCaso } from '../models/tarefa-caso.model';
@Injectable({ providedIn: 'root' })
export class TarefasCasoApiService {
  private readonly http = inject(HttpClient);
  private readonly url = inject(API_URL).replace(/\/$/, '') + '/api/casos/';
  private base(caso: string) {
    return this.url + caso + '/tarefas';
  }
  opcoes(caso: string) {
    return this.http.get<OpcoesTarefasCaso>(this.base(caso) + '/opcoes');
  }
  listar(caso: string) {
    return this.http.get<TarefaCaso[]>(this.base(caso));
  }
  buscar(caso: string, id: string) {
    return this.http.get<TarefaCaso>(this.base(caso) + '/' + id);
  }
  salvar(caso: string, id: string | null, versao: number | null, dados: DadosTarefaCaso) {
    return id
      ? this.http.put<TarefaCaso>(this.base(caso) + '/' + id, { versao, dados })
      : this.http.post<TarefaCaso>(this.base(caso), { versao, dados });
  }
  estado(caso: string, t: TarefaCaso, status: string, providencia = '') {
    return this.http.put<TarefaCaso>(this.base(caso) + '/' + t.id + '/estado', {
      versao: t.versao,
      status,
      providencia,
    });
  }
  adiar(caso: string, t: TarefaCaso, data: string) {
    return this.http.put<TarefaCaso>(this.base(caso) + '/' + t.id + '/adiamento', {
      versao: t.versao,
      data,
    });
  }
  delegar(caso: string, t: TarefaCaso, responsaveis: string[]) {
    return this.http.put<TarefaCaso>(this.base(caso) + '/' + t.id + '/responsaveis', {
      versao: t.versao,
      responsaveis,
    });
  }
  comentar(caso: string, t: TarefaCaso, texto: string) {
    return this.http.post<TarefaCaso>(this.base(caso) + '/' + t.id + '/comentarios', {
      versao: t.versao,
      texto,
    });
  }
  excluir(caso: string, t: TarefaCaso) {
    return this.http.delete<void>(this.base(caso) + '/' + t.id, { params: { versao: t.versao } });
  }
}
