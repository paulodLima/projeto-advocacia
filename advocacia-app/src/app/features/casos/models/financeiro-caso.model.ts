import { OpcaoCaso } from './caso.model';
export interface HonorarioCaso {
  id: string;
  tipo: string;
  valor: number | null;
  percentual: number | null;
  baseCalculo: number | null;
  observacoes: string;
}
export interface LancamentoCaso {
  id: string;
  grupo: string;
  descricao: string;
  valor: number;
  direcao: 'entrada' | 'saida';
  modalidade: string;
  vencimento: string;
  parcela: number;
  parcelas: number;
  categoriaId: string | null;
  categoria: string | null;
  contatoId: string | null;
  contato: string | null;
  observacoes: string;
  pago: boolean;
  pagoEm: string | null;
  titularidade: 'cliente' | 'escritorio';
}
export interface DespesaCaso {
  id: string;
  descricao: string;
  valor: number;
  data: string;
  reembolsavel: boolean;
  reembolsada: boolean;
}
export interface FinanceiroCasoFicha {
  versao: number;
  honorarios: HonorarioCaso[];
  lancamentos: LancamentoCaso[];
  despesas: DespesaCaso[];
  resumo: { honorarios: number; recebido: number; aReceber: number; despesasAReembolsar: number };
}
export interface FinanceiroCasoOpcoes {
  podeEditar: boolean;
  categorias: { id: string; nome: string; direcao: string }[];
  contatos: OpcaoCaso[];
}
export const MODALIDADES_CASO = [
  { id: 'pix', nome: 'Pix' },
  { id: 'boleto', nome: 'Boleto' },
  { id: 'cartao', nome: 'Cartão de crédito' },
  { id: 'recorrente', nome: 'Assinatura recorrente' },
  { id: 'transferencia', nome: 'Transferência' },
  { id: 'dinheiro', nome: 'Dinheiro' },
];
export function dividirParcelas(valor: number, quantidade: number) {
  if (
    !Number.isInteger(quantidade) ||
    quantidade < 1 ||
    quantidade > 60 ||
    !Number.isFinite(valor) ||
    valor <= 0
  )
    return [];
  const centavos = Math.round(valor * 100);
  const base = Math.floor(centavos / quantidade);
  if (base < 1) return [];
  const resto = centavos % quantidade;
  return Array.from({ length: quantidade }, (_, i) => (base + (i === 0 ? resto : 0)) / 100);
}
