import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { API_URL } from '../../../core/config/api-url.token';
import { ConfiguracoesLocal, ContaLocal, CategoriaLocal, RegistroLocal } from '../models/configuracoes.model';

export type Financeiro = ConfiguracoesLocal['financeiro'];
export const CAMPOS_CAPACIDADE = ['horas', 'modoHoras', 'advogados', 'horasSemanais', 'percentualProdutivo', 'semanasPorMes', 'margemLucro', 'fatorPosicionamento'] as const;
export interface FinanceiroDados {
  empresaId: string | null; papel: 'MASTER' | 'MEMBRO' | null; versao: number;
  contas: ContaLocal[]; categorias: CategoriaLocal[]; centros: RegistroLocal[]; custos: Financeiro['custos'];
  urh: number; competencia: string; capacidade: Pick<Financeiro, typeof CAMPOS_CAPACIDADE[number]>;
}
export function financeiroRecebido(d: FinanceiroDados): Financeiro { return { contas:d.contas, categorias:d.categorias, centros:d.centros, custos:d.custos, urh:d.urh, competencia:d.competencia, ...d.capacidade }; }
@Injectable({providedIn:'root'})
export class FinanceiroApiService {
  private readonly http=inject(HttpClient);
  private readonly url=inject(API_URL).replace(/\/$/,'')+'/api/config/financeiro';
  carregar() { return this.http.get<FinanceiroDados>(this.url); }
  private item(tipo:string, dados:unknown, versao:number, id?:string) { return id ? this.http.put<FinanceiroDados>(this.url+'/'+tipo+'/'+id,{versao,dados}) : this.http.post<FinanceiroDados>(this.url+'/'+tipo,{versao,dados}); }
  conta(dados:ContaLocal, versao:number, nova=false) { return this.item('contas',dados,versao,nova ? undefined : dados.id); }
  categoria(dados:CategoriaLocal, versao:number, nova=false) { return this.item('categorias',dados,versao,nova ? undefined : dados.id); }
  centro(dados:RegistroLocal, versao:number, novo=false) { const {id,...campos}=dados; return this.item('centros',campos,versao,novo ? undefined : id); }
  custo(nome:string, valor:number, versao:number) { return this.item('custos',{nome,valor},versao); }
  excluirCusto(id:string, versao:number) { return this.http.delete<FinanceiroDados>(this.url+'/custos/'+id,{params:{versao}}); }
  urh(f:Financeiro, versao:number) { return this.http.put<FinanceiroDados>(this.url+'/urh',{versao,dados:{urh:f.urh,competencia:f.competencia}}); }
  capacidade(f:Financeiro, versao:number) { return this.http.put<FinanceiroDados>(this.url+'/capacidade',{versao,dados:Object.fromEntries(CAMPOS_CAPACIDADE.map(c=>[c,f[c]]))}); }
}
