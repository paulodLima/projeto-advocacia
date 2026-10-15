export interface OpcaoCaso {
  id: string;
  nome: string;
}
export interface OpcoesCasos {
  podeEditar: boolean;
  podeCriarCliente: boolean;
  podeVerFinanceiro?: boolean;
  clientes: OpcaoCaso[];
  grupos: OpcaoCaso[];
  membros: OpcaoCaso[];
}
export interface Caso {
  id: string;
  versao: number;
  titulo: string;
  status: 'ativo' | 'convertido' | 'encerrado';
  clienteId: string | null;
  cliente: string | null;
  grupoId: string | null;
  grupo: string | null;
  responsavelId: string | null;
  responsavel: string | null;
  partesAdversas: string;
  observacoes: string;
  valorProposta: number | null;
  propostaEm: string | null;
  desfecho: string;
  valorFechado: number | null;
  motivoPerda: string;
  fechadoEm: string | null;
  leadId: string | null;
  criadoEm: string;
  atualizadoEm: string;
}
export interface AnotacaoCaso {
  id: string;
  data: string;
  descricao: string;
  autor: string;
  criadoEm: string;
}
export interface HistoricoLead {
  entrada?: string;
  origem?: string;
  necessidade?: string;
  contatos?: { id: string; data: string; canal: string; resultado: string; observacao: string }[];
  comentarios?: { id: string; texto: string; criadoEm: string; autor: string }[];
}
export interface FichaCaso {
  caso: Caso;
  anotacoes: AnotacaoCaso[];
  historicoLead: HistoricoLead;
}
export interface PaginaCasos {
  itens: Caso[];
  total: number;
  pagina: number;
  tamanho: number;
}
export interface FormularioCaso {
  versao: number | null;
  titulo: string;
  clienteId: string | null;
  grupoId: string | null;
  responsavelId: string | null;
  partesAdversas: string;
  observacoes: string;
  valorProposta: number | null;
  propostaEm: string | null;
  novoCliente: { nome: string; documento: string; telefone: string } | null;
}
export const STATUS_CASOS = [
  { id: 'ativo', nome: 'Ativos' },
  { id: 'convertido', nome: 'Convertidos' },
  { id: 'encerrado', nome: 'Encerrados' },
  { id: 'todos', nome: 'Todos' },
];
export const DESFECHOS_CASOS = [
  { id: '', nome: 'em aberto' },
  { id: 'fechado', nome: 'Fechado' },
  { id: 'perdido', nome: 'Perdido' },
  { id: 'desistiu', nome: 'Cliente desistiu' },
  { id: 'sem_resposta', nome: 'Sem resposta' },
];
export function valorMonetario(valor: string): number | null {
  if (!valor.trim()) return null;
  const limpo = valor.trim().replace(/\s|R\$/g, '');
  if (!/^(?:\d+|\d{1,3}(?:\.\d{3})+)(?:,\d{1,2})?$/.test(limpo))
    throw new Error('Informe o valor no formato 1.234,56.');
  const n = Number(limpo.replace(/\./g, '').replace(',', '.'));
  if (!Number.isFinite(n) || n > 99999999999999.99) throw new Error('Valor fora do limite.');
  return n;
}
export const dinheiroCampo = (valor: number | null) =>
  valor == null
    ? ''
    : valor.toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
