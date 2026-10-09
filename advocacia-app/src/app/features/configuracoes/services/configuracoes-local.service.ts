import { DOCUMENT } from '@angular/common';
import { effect, inject, Injectable, signal } from '@angular/core';
import { AuthService } from '../../../core/auth/auth.service';
import { ConfiguracoesLocal, configuracoesIniciais } from '../models/configuracoes.model';

@Injectable({ providedIn: 'root' })
export class ConfiguracoesLocalService {
  private readonly document = inject(DOCUMENT);
  private readonly auth = inject(AuthService);
  private usuarioId: string | null = null;
  readonly dados = signal<ConfiguracoesLocal>(configuracoesIniciais());
  readonly aviso = signal('');
  constructor() {
    this.carregar(this.auth.usuario()?.id ?? null);
    effect(() => {
      const id = this.auth.usuario()?.id ?? null;
      if (id !== this.usuarioId) this.carregar(id);
    });
  }
  private carregar(id: string | null) {
    this.usuarioId = id;
    let dados = configuracoesIniciais();
    try {
      const salvo = id && this.document.defaultView?.localStorage.getItem('configuracoes-front:' + id);
      if (salvo) {
        const valor = JSON.parse(salvo);
        if (valor.versao === 1) dados = this.restaurar(dados, valor);
      }
    } catch { this.aviso.set('Não foi possível restaurar os dados locais.'); }
    this.dados.set(dados);
  }
  private restaurar<T>(padrao: T, salvo: unknown): T {
    if (Array.isArray(padrao)) return (Array.isArray(salvo) ? salvo : padrao) as T;
    if (padrao !== null && typeof padrao === 'object') {
      if (salvo === null || typeof salvo !== 'object' || Array.isArray(salvo)) return padrao;
      const chaves = Object.keys(padrao);
      if (!chaves.length) return salvo as T;
      return Object.fromEntries(chaves.map(chave => [chave, this.restaurar((padrao as Record<string, unknown>)[chave], (salvo as Record<string, unknown>)[chave])])) as T;
    }
    return typeof salvo === typeof padrao ? salvo as T : padrao;
  }
  salvar(atualizar: (dados: ConfiguracoesLocal) => void) {
    const dados = structuredClone(this.dados());
    atualizar(dados);
    try {
      if (!this.usuarioId) { this.aviso.set('Entre na sua conta para salvar.'); return false; }
      this.document.defaultView?.localStorage.setItem('configuracoes-front:' + this.usuarioId, JSON.stringify(dados));
      this.dados.set(dados);
      this.aviso.set('Salvo neste navegador.');
      return true;
    } catch { this.aviso.set('Não foi possível salvar. O armazenamento do navegador está indisponível ou cheio.'); return false; }
  }
  async imagem(event: Event, tamanho = 1400, formato?: string): Promise<string | null> {
    const input = event.target as HTMLInputElement;
    const arquivo = input.files?.[0];
    input.value = '';
    if (!arquivo) return null;
    if (!['image/png', 'image/jpeg', 'image/webp', 'image/svg+xml'].includes(arquivo.type)) {
      this.aviso.set('Escolha uma imagem PNG, JPEG, WebP ou SVG.'); return null;
    }
    if (arquivo.size > 2 * 1024 * 1024) { this.aviso.set('Escolha uma imagem de até 2 MB.'); return null; }
    const url = await new Promise<string | null>((resolve) => {
      const leitor = new FileReader();
      leitor.onload = () => resolve(String(leitor.result));
      leitor.onerror = () => { this.aviso.set('Não foi possível ler a imagem.'); resolve(null); };
      leitor.readAsDataURL(arquivo);
    });
    if (!url || (arquivo.type === 'image/svg+xml' && !formato)) return url;
    return new Promise(resolve => {
      const imagem = this.document.createElement('img');
      imagem.onload = () => {
        const escala = Math.min(1, tamanho / Math.max(imagem.naturalWidth, imagem.naturalHeight));
        const canvas = this.document.createElement('canvas');
        canvas.width = Math.max(1, Math.round(imagem.naturalWidth * escala));
        canvas.height = Math.max(1, Math.round(imagem.naturalHeight * escala));
        const contexto = canvas.getContext('2d');
        if (!contexto) { resolve(url); return; }
        contexto.drawImage(imagem, 0, 0, canvas.width, canvas.height);
        resolve(canvas.toDataURL(formato ?? arquivo.type, 0.85));
      };
      imagem.onerror = () => { this.aviso.set('Não foi possível abrir a imagem.'); resolve(null); };
      imagem.src = url;
    });
  }
  id() { return crypto.randomUUID(); }
}
