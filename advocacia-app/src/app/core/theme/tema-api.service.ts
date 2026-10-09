import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { API_URL } from '../config/api-url.token';
import { TemaId } from './tema.model';
export interface PreferenciaTema { usuarioId:string; tema:TemaId; }
@Injectable({providedIn:'root'})
export class TemaApiService {
  private readonly http=inject(HttpClient);
  private readonly url=inject(API_URL).replace(/\/$/,'')+'/api/usuarios/me/tema';
  carregar() { return this.http.get<PreferenciaTema>(this.url); }
  salvar(tema:TemaId) { return this.http.put<PreferenciaTema>(this.url,{tema}); }
}
