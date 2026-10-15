import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { API_URL } from '../../../core/config/api-url.token';
import { FinanceiroCasoFicha, FinanceiroCasoOpcoes } from '../models/financeiro-caso.model';
@Injectable({ providedIn: 'root' })
export class FinanceiroCasoApiService {
  private readonly http = inject(HttpClient);
  private readonly url = inject(API_URL).replace(/\/$/, '') + '/api/casos/';
  private base(caso: string) {
    return this.url + caso + '/financeiro';
  }
  buscar(caso: string) {
    return this.http.get<FinanceiroCasoFicha>(this.base(caso));
  }
  opcoes(caso: string) {
    return this.http.get<FinanceiroCasoOpcoes>(this.base(caso) + '/opcoes');
  }
  criar(caso: string, tipo: 'honorarios' | 'lancamentos' | 'despesas', dados: object) {
    return this.http.post<FinanceiroCasoFicha>(this.base(caso) + '/' + tipo, dados);
  }
  alterar(
    caso: string,
    tipo: 'lancamentos' | 'despesas',
    id: string,
    acao: 'baixa' | 'vencimento' | 'titularidade' | 'reembolso',
    dados: object,
  ) {
    return this.http.put<FinanceiroCasoFicha>(
      this.base(caso) + '/' + tipo + '/' + id + '/' + acao,
      dados,
    );
  }
  excluir(
    caso: string,
    tipo: 'honorarios' | 'lancamentos' | 'despesas',
    id: string,
    versao: number,
  ) {
    return this.http.delete<FinanceiroCasoFicha>(this.base(caso) + '/' + tipo + '/' + id, {
      params: { versao },
    });
  }
}
