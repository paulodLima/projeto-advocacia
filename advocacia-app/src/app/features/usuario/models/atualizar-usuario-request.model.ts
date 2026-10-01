import { StatusUsuario } from './usuario.model';

export interface AtualizarUsuarioRequest {
  nome: string;
  email: string;
  status: StatusUsuario;
}
