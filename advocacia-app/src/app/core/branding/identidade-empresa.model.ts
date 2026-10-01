/** Contrato visual a ser preenchido pela empresa identificada no backend. */
export interface IdentidadeEmpresa {
  readonly nome: string;
  readonly logoUrl: string;
  readonly imagemLoginUrl: string;
}

export const IDENTIDADE_PADRAO: IdentidadeEmpresa = {
  nome: 'Gestão Advocacia',
  logoUrl: '/images/logo-ga.svg',
  imagemLoginUrl: '/images/login-justica.jpeg',
};
