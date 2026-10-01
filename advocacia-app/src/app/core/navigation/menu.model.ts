export interface ItemMenu { id: string; label: string; icon: string; emBreve?: boolean; }
export interface GrupoMenu { titulo: string; itens: ItemMenu[]; }
export const INICIO: ItemMenu = { id: 'inicio', label: 'Início', icon: 'home' };
export const CONFIGURACOES: ItemMenu = { id: 'config', label: 'Configurações', icon: 'settings' };
export const GRUPOS_MENU: GrupoMenu[] = [
  { titulo: 'Principal', itens: [
    { id: 'agenda', label: 'Agenda', icon: 'calendar' },
    { id: 'crm', label: 'CRM', icon: 'crm' },
    { id: 'casos', label: 'Casos', icon: 'folder' },
    { id: 'processual', label: 'Gestão Processual', icon: 'scale' },
  ] },
  { titulo: 'Relacionamento', itens: [
    { id: 'contatos', label: 'Contatos', icon: 'folder' },
    { id: 'parceiros', label: 'Parceiros', icon: 'handshake' },
    { id: 'equipe', label: 'Equipe', icon: 'users' },
  ] },
  { titulo: 'Ferramentas', itens: [
    { id: 'documentos', label: 'Documentos', icon: 'file' },
    { id: 'calculadoras', label: 'Calculadoras', icon: 'calculator' },
    { id: 'teses', label: 'Super Teses', icon: 'book' },
    { id: 'pops', label: 'POPs', icon: 'file' },
  ] },
  { titulo: 'Gestão', itens: [
    { id: 'financeiro', label: 'Financeiro', icon: 'wallet' },
    { id: 'relatorios', label: 'Relatórios', icon: 'chart', emBreve: true },
    { id: 'produtos', label: 'Produtos', icon: 'package', emBreve: true },
  ] },
];
export const ITENS_MENU = [INICIO, ...GRUPOS_MENU.flatMap((grupo) => grupo.itens), CONFIGURACOES];
