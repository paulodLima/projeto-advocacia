/** Contrato visual a ser preenchido pela empresa identificada no backend. */
export interface IdentidadeEmpresa {
  readonly nome: string;
  readonly logoUrl: string;
  readonly imagemLoginUrl: string;
  readonly logoExpandidoUrl: string;
  readonly logoRecolhidoUrl: string;
  readonly zoom: number;
  readonly posX: number;
  readonly posY: number;
  readonly logoLoginBranca: boolean;
}

export const IDENTIDADE_PADRAO: IdentidadeEmpresa = {
  nome: 'Gestão Advocacia',
  logoUrl: '/images/logo-ga.svg',
  imagemLoginUrl: '/images/login-justica.jpeg',
  logoExpandidoUrl: '/images/logo-ga.svg',
  logoRecolhidoUrl: '/images/logo-ga.svg',
  zoom: 1, posX: 50, posY: 20,
  logoLoginBranca: false,
};
