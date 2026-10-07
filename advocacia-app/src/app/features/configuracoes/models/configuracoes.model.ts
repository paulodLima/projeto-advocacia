export type AbaConfiguracao = 'perfil' | 'empresa' | 'equipe' | 'documentos' | 'cadastros' | 'workflows' | 'financeiro' | 'olivia' | 'senhas' | 'tema';
export const ABAS_CONFIGURACAO: { id: AbaConfiguracao; label: string }[] = [
  { id: 'perfil', label: 'Meu perfil' }, { id: 'empresa', label: 'Empresa' },
  { id: 'equipe', label: 'Equipe' }, { id: 'documentos', label: 'Documentos' },
  { id: 'cadastros', label: 'Cadastros e sistemas' }, { id: 'workflows', label: 'Workflows' },
  { id: 'financeiro', label: 'Financeiro' }, { id: 'olivia', label: 'Olívia' },
  { id: 'senhas', label: 'Senhas' }, { id: 'tema', label: 'Tema' },
];
export interface PerfilLocal { telefone: string; emailPessoal: string; endereco: string; foto: string; }
export interface IdentidadeLocal { imagens: Record<string, string>; usos: Record<string, string>; zoom: number; posX: number; posY: number; }
export interface MembroLocal { id: string; nome: string; email: string; perfil: string; status: string; telefone: string; admissao: string; nascimento: string; convidado: boolean; nomeExibicao?: string; abas?: string[]; enviaDocumento?: boolean; }
export interface CampoLista { id: string; label: string; tipo?: string; opcoes?: string[]; }
export interface ListaCadastro { id: string; nome: string; dica: string; campos: CampoLista[]; }
export interface RegistroLocal { id: string; ativo: boolean; [campo: string]: string | number | boolean; }
export interface EtapaLocal { id: string; nome: string; responsavel: string; dias: number; }
export interface WorkflowLocal { id: string; nome: string; buffer: number; gatilhos: string[]; etapas: EtapaLocal[]; }
export interface SistemaLocal { id: string; nome: string; url: string; ativo: boolean; icone: string; cor: string; logo: string; }
export interface RotinaLocal { id: string; nome: string; periodo: string; }
export interface ContaLocal { id: string; nome: string; banco: string; tipo: string; saldo: number; padrao: boolean; ativo: boolean; }
export interface CategoriaLocal { id: string; nome: string; grupo: string; direcao: string; ativo: boolean; }
export interface CustoLocal { id: string; nome: string; valor: number; }
export interface ConfiguracoesLocal {
  versao: 1; perfil: PerfilLocal; empresa: Record<string, string>; identidade: IdentidadeLocal;
  membros: MembroLocal[]; listas: Record<string, RegistroLocal[]>; workflows: WorkflowLocal[];
  sistemas: SistemaLocal[]; rotinas: RotinaLocal[]; modelos: Record<string, string>;
  documento: { logo: string; rodape: string; alinhamento: string; repetir: boolean; documento: boolean; endereco: boolean; contato: boolean };
  financeiro: { contas: ContaLocal[]; categorias: CategoriaLocal[]; centros: RegistroLocal[]; urh: number; competencia: string; custos: CustoLocal[]; horas: number; modoHoras: string; advogados: number; horasSemanais: number; percentualProdutivo: number; semanasPorMes: number; margemLucro: number; fatorPosicionamento: number };
  olivia: { persona: string; tamanho: string; tratamento: string; instrucoes: string };
}
export function configuracoesIniciais(): ConfiguracoesLocal {
  return { versao: 1, perfil: { telefone: '', emailPessoal: '', endereco: '', foto: '' }, empresa: {},
    identidade: { imagens: {}, usos: {}, zoom: 1, posX: 50, posY: 50 }, membros: [], listas: {}, workflows: [], sistemas: [], rotinas: [], modelos: {},
    documento: { logo: '', rodape: '', alinhamento: 'centro', repetir: false, documento: true, endereco: true, contato: true },
    financeiro: { contas: [], categorias: [], centros: [], urh: 0, competencia: '', custos: [], horas: 160, modoHoras: 'estrutura', advogados: 1, horasSemanais: 40, percentualProdutivo: 60, semanasPorMes: 4.2, margemLucro: 30, fatorPosicionamento: 3 },
    olivia: { persona: 'executiva', tamanho: 'curta', tratamento: '', instrucoes: '' } };
}
export const LISTAS_CADASTRO: ListaCadastro[] = [
  { id: 'grupos', nome: 'Grupos de ação', dica: 'as áreas de atuação do escritório — a sigla entra na numeração dos documentos', campos: [{ id: 'nome', label: 'Grupo' }, { id: 'sigla', label: 'Sigla' }] },
  { id: 'documentos', nome: 'Tipos de documento', dica: 'cada tipo tem a sua própria contagem, que reinicia a cada ano', campos: [{ id: 'nome', label: 'Tipo de documento' }, { id: 'sigla', label: 'Sigla' }] },
  { id: 'acoes', nome: 'Tipos de ação', dica: 'cada tipo pertence a um grupo', campos: [{ id: 'nome', label: 'Tipo de ação' }, { id: 'grupo', label: 'Grupo', tipo: 'grupo' }] },
  { id: 'fases', nome: 'Fases', dica: 'as colunas do funil processual', campos: [{ id: 'nome', label: 'Fase' }, { id: 'codigo', label: 'Código' }] },
  { id: 'etapas', nome: 'Etapas', dica: 'a classificação ordena a etapa dentro da fase', campos: [{ id: 'nome', label: 'Etapa' }, { id: 'fase', label: 'Fase', tipo: 'fase' }, { id: 'classificacao', label: 'Classif.', tipo: 'number' }] },
  { id: 'relacoes', nome: 'Tipos de relação', dica: 'como dois processos autônomos se ligam', campos: [{ id: 'nome', label: 'Relação' }] },
  { id: 'tarefas', nome: 'Tipos de tarefa', dica: 'a pontuação vira produtividade ao concluir', campos: [{ id: 'nome', label: 'Tarefa' }, { id: 'fase', label: 'Fase', tipo: 'fase' }, { id: 'pontos', label: 'Pontos', tipo: 'number' }] },
  { id: 'origens', nome: 'Origem dos clientes', dica: 'canais de captação', campos: [{ id: 'nome', label: 'Origem' }] },
  { id: 'etiquetas', nome: 'Etiquetas', dica: 'assuntos reutilizados nos contatos, leads e processos', campos: [{ id: 'nome', label: 'Etiqueta' }, { id: 'cor', label: 'Cor', tipo: 'color' }] },
];
