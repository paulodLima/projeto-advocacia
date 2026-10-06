import { Routes } from '@angular/router';

// Adicione as rotas com loadComponent quando as páginas forem implementadas.
export const authRoutes: Routes = [
  {
    path: 'cadastro',
    pathMatch: 'full',
    redirectTo: 'login',
  },
  {
    path: 'login',
    title: 'Entrar — Gestão Advocacia',
    loadComponent: () => import('./pages/login/login').then((module) => module.Login),
  },
];
