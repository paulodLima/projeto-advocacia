import { Routes } from '@angular/router';
import { authGuard } from '../../core/guards/auth.guard';

// Adicione as rotas com loadComponent quando as páginas forem implementadas.
export const authRoutes: Routes = [
  {
    path: 'inicio',
    title: 'Início — Gestão Advocacia',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/inicio/inicio').then((module) => module.Inicio),
  },
  {
    path: 'login',
    title: 'Entrar — Gestão Advocacia',
    loadComponent: () => import('./pages/login/login').then((module) => module.Login),
  },
];
