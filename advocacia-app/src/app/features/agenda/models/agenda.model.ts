export type TipoAtividade = 'TAREFA' | 'PRAZO' | 'REUNIAO' | 'AUDIENCIA';
export type SituacaoAgenda = 'PENDENTE' | 'CONCLUIDA' | 'CANCELADA';
export const TIPOS_AGENDA: { codigo: TipoAtividade; nome: string }[] = [{ codigo: 'TAREFA', nome: 'Tarefa' }, { codigo: 'PRAZO', nome: 'Prazo' }, { codigo: 'REUNIAO', nome: 'Reunião' }, { codigo: 'AUDIENCIA', nome: 'Audiência' }];
export const SITUACOES_AGENDA = [{ codigo: 'PENDENTE', nome: 'Pendente' }, { codigo: 'CONCLUIDA', nome: 'Concluída' }, { codigo: 'CANCELADA', nome: 'Cancelada' }];
export interface DadosAtividade {
  tipo: TipoAtividade; titulo: string; tipoTarefaId: string | null; data: string | null; dataFim: string | null;
  hora: string | null; horaFim: string | null; prazoFatal: string | null; diaInteiro: boolean;
  modalidade: string | null; local: string; descricao: string; responsaveis: string[];
}
export interface EventoAgenda { id: string; autor: string; acao: string; observacao: string; criadoEm: string; }
export interface AtividadeAgenda { id: string; versao: number; dados: DadosAtividade; situacao: SituacaoAgenda; concluidaEm: string | null; criadoEm: string; atualizadoEm: string; historico: EventoAgenda[]; }
export interface OpcaoAgenda { id: string; nome: string; ativo: boolean; }
export interface OpcoesAgenda { podeEditar: boolean; responsaveis: OpcaoAgenda[]; tiposTarefa: OpcaoAgenda[]; fusoHorario: string; }
export interface PaginaAgenda { itens: AtividadeAgenda[]; total: number; pagina: number; tamanho: number; }
export interface DiaAgenda { data: string; total: number; pendentes: number; prazosFatais: number; }
export interface FiltroAgenda { inicio: string; fim: string; tipo: string; situacao: string; responsavel: string; busca: string; incluirSemData: boolean; }
export function dataCivil(date: Date) { return `${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')}`; }
export function hojeEscritorio() {
  const partes = new Intl.DateTimeFormat('en-CA', { timeZone: 'America/Sao_Paulo', year: 'numeric', month: '2-digit', day: '2-digit' }).formatToParts(new Date());
  const valor = (tipo: string) => partes.find(p => p.type === tipo)?.value;
  return `${valor('year')}-${valor('month')}-${valor('day')}`;
}
export function nomeTipo(tipo: string) { return TIPOS_AGENDA.find(t => t.codigo === tipo)?.nome ?? tipo; }
export function nomeSituacao(situacao: string) { return SITUACOES_AGENDA.find(s => s.codigo === situacao)?.nome ?? situacao; }
export function atividadeAtrasada(a: AtividadeAgenda) { const data = a.dados.prazoFatal ?? a.dados.dataFim ?? a.dados.data; return a.situacao === 'PENDENTE' && !!data && data < hojeEscritorio(); }
