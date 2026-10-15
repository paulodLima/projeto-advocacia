import { OpcaoCaso } from './caso.model';
export interface DadosTarefaCaso {
  tipoId: string | null;
  titulo: string;
  data: string | null;
  dataFim: string | null;
  hora: string | null;
  horaFim: string | null;
  prazoFatal: string | null;
  local: string;
  descricao: string;
  mostrarAgenda: boolean;
  diaInteiro: boolean;
  repeticao: string;
  flags: string[];
  responsaveis: string[];
}
export interface TarefaCaso {
  id: string;
  versao: number;
  status: 'pendente' | 'concluida';
  dados: DadosTarefaCaso;
  tipo: string | null;
  responsaveis: OpcaoCaso[];
  concluidaPor: string | null;
  concluidaEm: string | null;
  comentarios: { id: string; texto: string; autor: string; criadoEm: string }[];
}
export interface OpcoesTarefasCaso {
  podeEditar: boolean;
  tipos: OpcaoCaso[];
  membros: OpcaoCaso[];
}
export function hojeTarefa() {
  return new Intl.DateTimeFormat('en-CA', {
    timeZone: 'America/Sao_Paulo',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(new Date());
}
export function novaTarefa(): DadosTarefaCaso {
  return {
    tipoId: null,
    titulo: '',
    data: hojeTarefa(),
    dataFim: null,
    hora: null,
    horaFim: null,
    prazoFatal: null,
    local: '',
    descricao: '',
    mostrarAgenda: true,
    diaInteiro: false,
    repeticao: 'nunca',
    flags: [],
    responsaveis: [],
  };
}
