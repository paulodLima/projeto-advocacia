export interface CadastroLead {
  nome: string;
  telefone: string;
  email: string;
  origem: string;
  necessidade: string;
  temperatura: string;
  observacoes: string;
  etiquetas: string[];
}
export interface EstadoLead {
  status: string;
  cadenciaPausada: boolean;
  retornoEm: string | null;
  consultaAgendada: boolean;
  consultaEm: string | null;
  consultaStatus: string | null;
  qualificacao: Record<string, boolean | null>;
}
export interface Tratativa {
  id: string;
  data: string;
  canal: string;
  resultado: string;
  observacao: string;
  autorId: string;
}
export interface Comentario {
  id: string;
  autorId: string;
  autor: string;
  texto: string;
  criadoEm: string;
  mencoes: string[];
}
export interface Lead {
  id: string;
  empresaId: string;
  versao: number;
  proximo: string | null;
  dados: {
    cadastro: CadastroLead;
    estado: EstadoLead;
    entrada: string;
    ultimoContato: string | null;
    passo: number;
    casoId: string | null;
  };
  contatos: Tratativa[];
  comentarios: Comentario[];
}
export interface OpcaoCrm {
  id: string;
  nome: string;
  cor: string;
}
export interface PassoCadencia {
  passo: number;
  dias: number;
  rotulo: string;
}
export interface OpcoesCrm {
  podeEditar: boolean;
  podeConverter: boolean;
  origens: string[];
  etiquetas: OpcaoCrm[];
  membros: OpcaoCrm[];
  cadencia: PassoCadencia[];
}
export interface PaginaLeads {
  itens: Lead[];
  total: number;
  pagina: number;
  tamanho: number;
  origens: string[];
  indicadores: {
    emCadencia: number;
    atrasados: number;
    quentes: number;
    ganhos: number;
    decididos: number;
    conversao: number | null;
  };
}
export interface Destinatario {
  chave: string;
  tipo: string;
  nome: string;
  contato: string;
  abordado: boolean;
}
export interface Campanha {
  id: string;
  versao: number;
  nome: string;
  mes: number | null;
  cor: string;
  descricao: string;
  etiquetas: string[];
  destinatarios: Destinatario[];
}
export interface Manutencao {
  caso_id: string;
  titulo: string;
  data_entrega: string | null;
  meses: number;
  ultimo: string | null;
  momento: string;
}
export interface Marketing {
  campanhas: Campanha[];
  manutencoes: Manutencao[];
  ano: number;
}
export const TEMPERATURAS = [
  { id: 'quente', nome: 'Quente', cor: '#b4442f' },
  { id: 'morno', nome: 'Morno', cor: '#9c5d19' },
  { id: 'frio', nome: 'Frio', cor: '#4e6e89' },
];
export const STATUS_LEAD = [
  { id: 'ativo', nome: 'Em cadência' },
  { id: 'ganho', nome: 'Ganhos' },
  { id: 'perdido', nome: 'Perdidos' },
  { id: 'todos', nome: 'Todos' },
];
export const CANAIS = ['WhatsApp', 'Ligação', 'E-mail', 'Instagram DM', 'SMS'];
export const RESULTADOS = [
  'Sem resposta',
  'Conversou — segue interessado',
  'Pediu para retornar depois',
  'Quer agendar consultoria',
  'Sem interesse',
];
export const FACA = [
  { campo: 'fit', letra: 'F', titulo: 'Fit', dica: 'O caso se encaixa nas áreas do escritório' },
  { campo: 'assunto', letra: 'A', titulo: 'Assunto', dica: 'A demanda está clara e é viável' },
  {
    campo: 'capacidade',
    letra: 'C',
    titulo: 'Capacidade',
    dica: 'Tem condição de arcar com os honorários',
  },
  {
    campo: 'autoridade',
    letra: 'A',
    titulo: 'Autoridade',
    dica: 'Fala com quem decide a contratação',
  },
];
export const MESES = [
  'Janeiro',
  'Fevereiro',
  'Março',
  'Abril',
  'Maio',
  'Junho',
  'Julho',
  'Agosto',
  'Setembro',
  'Outubro',
  'Novembro',
  'Dezembro',
];
export function hojeCrm() {
  return new Intl.DateTimeFormat('en-CA', {
    timeZone: 'America/Sao_Paulo',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(new Date());
}
export function diasRetorno(data: string) {
  return Math.round(
    (Date.parse(data + 'T12:00:00Z') - Date.parse(hojeCrm() + 'T12:00:00Z')) / 86400000,
  );
}
export function dataCrm(data: string | null) {
  return data ? data.slice(0, 10).split('-').reverse().join('/') : '—';
}
export function novoCadastro(): CadastroLead {
  return {
    nome: '',
    telefone: '',
    email: '',
    origem: 'Instagram Ads',
    necessidade: '',
    temperatura: 'morno',
    observacoes: '',
    etiquetas: [],
  };
}
