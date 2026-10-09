export interface SocioParceiro {
  nome: string;
  oab: string;
}
export interface ParceiroFormulario {
  versao: number | null;
  dados: Record<string, string>;
  socios: SocioParceiro[];
  areasAtuacao: string[];
}
export interface Parceiro extends ParceiroFormulario {
  id: string;
  empresaId: string;
  versao: number;
}
export interface PaginaParceiros {
  itens: Parceiro[];
  total: number;
  pagina: number;
  tamanho: number;
}
export interface OpcoesParceiros {
  podeEditar: boolean;
  podeVerContatos: boolean;
  estados: string[];
  areas: string[];
}
export interface CarteiraParceiro {
  itens: { id: string; nome: string; tipo: string }[];
  total: number;
  pagina: number;
  tamanho: number;
}
export const CAMPOS_PARCEIRO = [
  'nome',
  'tipo_pessoa',
  'nome_fantasia',
  'documento',
  'email',
  'telefone',
  'cep',
  'logradouro',
  'numero',
  'complemento',
  'bairro',
  'cidade',
  'uf',
  'observacoes',
  'oab',
  'advogado_responsavel',
  'foto_propria',
  'site',
  'instagram',
];
export const ESTADOS = [
  'AC',
  'AL',
  'AP',
  'AM',
  'BA',
  'CE',
  'DF',
  'ES',
  'GO',
  'MA',
  'MT',
  'MS',
  'MG',
  'PA',
  'PB',
  'PR',
  'PE',
  'PI',
  'RJ',
  'RN',
  'RS',
  'RO',
  'RR',
  'SC',
  'SP',
  'SE',
  'TO',
];
export const AREAS_PARCEIRO = [
  'Família',
  'Sucessões',
  'Agronegócio',
  'Consumidor',
  'Cível',
  'Trabalhista',
  'Tributário',
  'Empresarial',
  'Criminal',
  'Previdenciário',
  'Imobiliário',
  'Ambiental',
];
export function parceiroVazio(): ParceiroFormulario {
  return {
    versao: null,
    dados: { ...Object.fromEntries(CAMPOS_PARCEIRO.map((k) => [k, ''])), tipo_pessoa: 'PF' },
    socios: [],
    areasAtuacao: [],
  };
}
