export const TEMAS = [
  { id: 'verde', nome: 'Verde · Original', cor: '#4f5a49' },
  { id: 'vermelho', nome: 'Vermelho', cor: '#694b48' },
  { id: 'amarelo', nome: 'Amarelo', cor: '#655d38' },
  { id: 'azul', nome: 'Azul', cor: '#46596b' },
  { id: 'preto', nome: 'Preto', cor: '#343434' },
  { id: 'roxo', nome: 'Roxo', cor: '#5b4d69' },
] as const;
export type TemaId = typeof TEMAS[number]['id'];
export function temaValido(valor: unknown): valor is TemaId {
  return TEMAS.some((tema) => tema.id === valor);
}
