import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { API_URL } from '../../../core/config/api-url.token';
import { ConfiguracoesLocal } from '../models/configuracoes.model';
export type PapelTimbrado = ConfiguracoesLocal['documento'];
export interface DocumentosConfigDados { empresaId: string | null; papel: 'MASTER' | 'MEMBRO' | null; documento: PapelTimbrado; modelos: Record<string, string>; }
@Injectable({ providedIn: 'root' })
export class DocumentosApiService {
  private readonly http = inject(HttpClient);
  private readonly url = inject(API_URL).replace(/\/$/, '') + '/api/config/documentos';
  carregar() { return this.http.get<DocumentosConfigDados>(this.url); }
  salvarPapel(papel: PapelTimbrado) { return this.http.put<PapelTimbrado>(this.url + '/papel', papel); }
  salvarModelos(modelos: Record<string, string>) { return this.http.put<Record<string, string>>(this.url + '/modelos', { modelos }); }
}
