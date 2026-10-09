import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { API_URL } from '../../../core/config/api-url.token';
export interface Credencial { id:string; nome:string; url:string; usuario:string; descricao:string; observacao:string; soAdmin:boolean; versao:number; }
export interface CofreDados { empresaId:string|null; podeEditar:boolean; configurado:boolean; credenciais:Credencial[]; }
export type CredencialForm=Omit<Credencial,'id'|'versao'> & {versao:number|null;senha:string|null};
@Injectable({providedIn:'root'})
export class CofreApiService {
  private readonly http=inject(HttpClient);
  private readonly url=inject(API_URL).replace(/\/$/,'')+'/api/config/cofre';
  carregar() { return this.http.get<CofreDados>(this.url); }
  salvar(form:CredencialForm,id?:string) { return id ? this.http.put<Credencial>(this.url+'/'+id,form) : this.http.post<Credencial>(this.url,form); }
  revelar(id:string) { return this.http.post<{senha:string}>(this.url+'/'+id+'/revelar',{}); }
  excluir(c:Credencial) { return this.http.delete<void>(this.url+'/'+c.id,{params:{versao:c.versao}}); }
}
