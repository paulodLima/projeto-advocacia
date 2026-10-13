import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { API_URL } from '../../../core/config/api-url.token';
import { AtividadeAgenda, DadosAtividade, DiaAgenda, FiltroAgenda, OpcoesAgenda, PaginaAgenda, SituacaoAgenda } from '../models/agenda.model';
@Injectable({ providedIn: 'root' })
export class AgendaApiService {
  private readonly http = inject(HttpClient);
  private readonly url = inject(API_URL).replace(/\/$/,'') + '/api/agenda';
  private params(f: FiltroAgenda) {
    let params = new HttpParams().set('inicio',f.inicio).set('fim',f.fim).set('incluirSemData',f.incluirSemData);
    for (const campo of ['tipo','situacao','responsavel','busca'] as const) if (f[campo]) params = params.set(campo,f[campo]);
    return params;
  }
  opcoes() { return this.http.get<OpcoesAgenda>(this.url + '/opcoes'); }
  listar(f: FiltroAgenda, pagina = 0) { return this.http.get<PaginaAgenda>(this.url,{params:this.params(f).set('pagina',pagina).set('tamanho',50)}); }
  calendario(f: FiltroAgenda) { return this.http.get<DiaAgenda[]>(this.url + '/calendario',{params:this.params(f)}); }
  buscar(id: string) { return this.http.get<AtividadeAgenda>(this.url + '/' + id); }
  salvar(dados: DadosAtividade, base: AtividadeAgenda | null) {
    const request = { ...dados, versao: base?.versao ?? 0 };
    return base ? this.http.put<AtividadeAgenda>(this.url + '/' + base.id,request) : this.http.post<AtividadeAgenda>(this.url,request);
  }
  situacao(base: AtividadeAgenda, situacao: SituacaoAgenda, observacao: string) {
    return this.http.patch<AtividadeAgenda>(this.url + '/' + base.id + '/situacao',{ versao:base.versao,situacao,observacao });
  }
}
