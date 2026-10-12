import { Routes } from '@angular/router';

// Adicione as rotas com loadComponent quando as páginas forem implementadas.
export const authRoutes: Routes = [
  {
    path: 'cadastro',
    title: 'Complete seu cadastro — Gestão Advocacia',
    loadComponent: () => import('./pages/cadastro/cadastro').then((module) => module.Cadastro),
  },
  {
    path: 'login',
    title: 'Entrar — Gestão Advocacia',
    loadComponent: () => import('./pages/login/login').then((module) => module.Login),
  },
];
