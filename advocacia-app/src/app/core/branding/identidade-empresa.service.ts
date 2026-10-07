import { Injectable, signal } from '@angular/core';
import { IDENTIDADE_PADRAO, IdentidadeEmpresa } from './identidade-empresa.model';
import { IdentidadeResposta } from './identidade-api.service';

@Injectable({ providedIn: 'root' })
export class IdentidadeEmpresaService {
  private readonly estado = signal<IdentidadeEmpresa>({ ...IDENTIDADE_PADRAO });
  readonly identidade = this.estado.asReadonly();

  /** Substitui a identidade anterior; campos ausentes usam os padrões do produto. */
  aplicar(empresa: Partial<IdentidadeEmpresa>) {
    this.estado.set({
      nome: empresa.nome?.trim() || IDENTIDADE_PADRAO.nome,
      logoUrl: empresa.logoUrl?.trim() || IDENTIDADE_PADRAO.logoUrl,
      imagemLoginUrl: empresa.imagemLoginUrl?.trim() || IDENTIDADE_PADRAO.imagemLoginUrl,
      logoExpandidoUrl: empresa.logoExpandidoUrl || IDENTIDADE_PADRAO.logoExpandidoUrl,
      logoRecolhidoUrl: empresa.logoRecolhidoUrl || IDENTIDADE_PADRAO.logoRecolhidoUrl,
      zoom: empresa.zoom ?? IDENTIDADE_PADRAO.zoom,
      posX: empresa.posX ?? IDENTIDADE_PADRAO.posX,
      posY: empresa.posY ?? IDENTIDADE_PADRAO.posY,
      logoLoginBranca: empresa.logoLoginBranca ?? false,
    });
  }

  restaurar() {
    this.estado.set({ ...IDENTIDADE_PADRAO });
  }

  receber(resposta: IdentidadeResposta) {
    const visual = resposta.identidade;
    const logo = (uso: string) => visual.imagens[visual.usos[uso]];
    this.aplicar({ nome: resposta.nome, logoUrl: logo('login'), logoExpandidoUrl: logo('menu_expandido'),
      logoRecolhidoUrl: logo('menu_recolhido'), imagemLoginUrl: visual.imagens['foto_login'],
      logoLoginBranca: !!logo('login') && visual.usos['login'].endsWith('_branca'),
      ...(visual.imagens['foto_login'] ? { zoom: visual.zoom, posX: visual.posX, posY: visual.posY } : {}) });
  }

  restaurarLogo() {
    this.estado.update((empresa) => ({ ...empresa, logoUrl: IDENTIDADE_PADRAO.logoUrl, logoExpandidoUrl: IDENTIDADE_PADRAO.logoExpandidoUrl, logoRecolhidoUrl: IDENTIDADE_PADRAO.logoRecolhidoUrl, logoLoginBranca: false }));
  }

  restaurarImagemLogin() {
    this.estado.update((empresa) => ({ ...empresa, imagemLoginUrl: IDENTIDADE_PADRAO.imagemLoginUrl, zoom: 1, posX: 50, posY: 20 }));
  }
}
