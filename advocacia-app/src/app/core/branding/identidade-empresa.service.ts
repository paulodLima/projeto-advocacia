import { Injectable, signal } from '@angular/core';
import { IDENTIDADE_PADRAO, IdentidadeEmpresa } from './identidade-empresa.model';

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
    });
  }

  restaurar() {
    this.estado.set({ ...IDENTIDADE_PADRAO });
  }

  restaurarLogo() {
    this.estado.update((empresa) => ({ ...empresa, logoUrl: IDENTIDADE_PADRAO.logoUrl }));
  }

  restaurarImagemLogin() {
    this.estado.update((empresa) => ({ ...empresa, imagemLoginUrl: IDENTIDADE_PADRAO.imagemLoginUrl }));
  }
}
