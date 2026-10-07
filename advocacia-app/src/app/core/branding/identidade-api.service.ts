import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { API_URL } from '../config/api-url.token';

export interface IdentidadeVisual {
  imagens: Record<string, string>;
  usos: Record<string, string>;
  zoom: number;
  posX: number;
  posY: number;
}
export interface IdentidadeResposta { empresaId: string; nome: string; identidade: IdentidadeVisual; }

@Injectable({ providedIn: 'root' })
export class IdentidadeApiService {
  private readonly http = inject(HttpClient);
  private readonly url = inject(API_URL).replace(/\/$/, '');
  carregar() { return this.http.get<IdentidadeResposta>(this.url + '/api/empresas/minha/identidade'); }
  publica(id: string) { return this.http.get<IdentidadeResposta>(this.url + '/api/public/empresas/' + encodeURIComponent(id) + '/identidade'); }
  salvar(identidade: IdentidadeVisual) { return this.http.put<IdentidadeResposta>(this.url + '/api/empresas/minha/identidade', identidade); }
}
