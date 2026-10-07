import { HttpClient } from '@angular/common/http';
import { effect, inject, Injectable, signal } from '@angular/core';
import { tap } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import { API_URL } from '../../../core/config/api-url.token';

export interface MinhaEmpresa {
  id: string | null;
  papel: 'MASTER' | 'MEMBRO' | null;
  dados: Record<string, string>;
}
export const LIMITES_EMPRESA: Record<string, number> = {
  razao_social: 150, nome_fantasia: 150, cnpj: 32, oab_sociedade: 50, inscricao_municipal: 50,
  cnae: 20, constituida_em: 10, titular_nome: 150, titular_cpf: 20, oab: 50, endereco: 500,
  bairro: 100, cidade: 100, uf: 2, cep: 12, telefone: 30, whatsapp: 30, email: 254, site: 500,
  regime: 100, codigo_servico: 50, iss_aliquota: 20, observacao_fiscal: 1000, banco_dados: 1000,
};

@Injectable({ providedIn: 'root' })
export class EmpresaApiService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly url = inject(API_URL).replace(/\/$/, '') + '/api/empresas';
  readonly empresa = signal<MinhaEmpresa | null>(null);
  private usuarioId = this.auth.usuario()?.id;
  private sequencia = 0;
  constructor() {
    effect(() => {
      const id = this.auth.usuario()?.id;
      if (id !== this.usuarioId) { this.usuarioId = id; this.empresa.set(null); }
    });
  }
  carregar() { return this.http.get<MinhaEmpresa>(this.url + '/minha').pipe(this.receber()); }
  criar(dados: Record<string, string>) { return this.http.post<MinhaEmpresa>(this.url, { dados }).pipe(this.receber()); }
  salvar(dados: Record<string, string>) { return this.http.put<MinhaEmpresa>(this.url + '/minha', { dados }).pipe(this.receber()); }
  private receber() {
    const usuarioId = this.auth.usuario()?.id;
    const sequencia = ++this.sequencia;
    return tap<MinhaEmpresa>(empresa => {
      if (usuarioId === this.auth.usuario()?.id && sequencia === this.sequencia) this.empresa.set(empresa);
    });
  }
}
