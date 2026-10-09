export const TIPOS_CONTATO = [
  'Cliente',
  'Parte adversa',
  'Parte interessada',
  'Advogado(a)',
  'Fornecedor',
  'Parceiro',
];
export const CAMPOS_CONTATO = [
  'nome',
  'tipo',
  'tipo_pessoa',
  'documento',
  'nome_fantasia',
  'email',
  'telefone',
  'telefone2',
  'whatsapp',
  'cep',
  'logradouro',
  'numero',
  'complemento',
  'bairro',
  'cidade',
  'uf',
  'origem_id',
  'observacoes',
  'data_nascimento',
  'rg',
  'rg_orgao_emissor',
  'nacionalidade',
  'estado_civil',
  'profissao',
  'carteira',
  'carteira_parceiro_id',
];
export interface Contato {
  id: string;
  empresaId: string;
  versao: number;
  dados: Record<string, string>;
  representantes: Record<string, string>[];
  indicadores: string[];
}
export interface IndicadorContato {
  id: string;
  nome: string;
  cor: string;
}
export interface OpcoesContato {
  podeEditar: boolean;
  indicadores: IndicadorContato[];
  origens: { id: string; nome: string }[];
  parceiros: { id: string; nome: string }[];
}
export interface PaginaContatos {
  itens: Contato[];
  total: number;
  pagina: number;
  tamanho: number;
}
export type ContatoFormulario = Pick<Contato, 'dados' | 'representantes' | 'indicadores'> & {
  versao: number | null;
};
export function contatoVazio(): ContatoFormulario {
  return {
    versao: null,
    dados: {
      ...Object.fromEntries(CAMPOS_CONTATO.map((c) => [c, ''])),
      tipo: 'Cliente',
      tipo_pessoa: 'PF',
      whatsapp: 'telefone',
      carteira: 'casa',
    },
    representantes: [],
    indicadores: [],
  };
}
export function corContato(tipo: string) {
  return (
    (
      {
        Cliente: 'var(--tema-primaria)',
        'Parte adversa': '#9a4b3f',
        'Parte interessada': '#7d6c5e',
        'Advogado(a)': 'var(--tema-clara)',
        Fornecedor: '#cdb9af',
        Parceiro: '#7d6c5e',
      } as Record<string, string>
    )[tipo] || '#7d6c5e'
  );
}
export function iniciaisContato(nome: string) {
  return nome
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((n) => n[0])
    .join('')
    .toUpperCase();
}
export function enderecoContato(d: Record<string, string>) {
  return [
    [d['logradouro'], d['numero']].filter(Boolean).join(', '),
    [d['complemento'], d['bairro']].filter(Boolean).join(' - '),
    [d['cidade'], d['uf']].filter(Boolean).join('/'),
    d['cep'] ? 'CEP ' + d['cep'] : '',
  ]
    .filter(Boolean)
    .join(' — ');
}
