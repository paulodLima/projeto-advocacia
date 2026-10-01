import { Routes } from '@angular/router';
import { cadastroGuard } from '../../core/guards/cadastro.guard';

// Adicione as rotas com loadComponent quando as páginas forem implementadas.
export const authRoutes: Routes = [
  {
    path: 'cadastro',
    title: 'Criar conta — Gestão Advocacia',
    canActivate: [cadastroGuard],
    loadComponent: () => import('./pages/cadastro/cadastro').then((module) => module.Cadastro),
  },
  {
    path: 'login',
    title: 'Entrar — Gestão Advocacia',
    loadComponent: () => import('./pages/login/login').then((module) => module.Login),
  },
];
