import { Routes } from '@angular/router';

export const clienteRoutes: Routes = [
  {
    path: '',
    title: 'Contatos — Gestão Advocacia',
    loadComponent: () => import('./pages/contatos').then((m) => m.Contatos),
  },
  {
    path: ':id/editar',
    title: 'Editar contato — Gestão Advocacia',
    data: { editar: true },
    loadComponent: () => import('./pages/contatos').then((m) => m.Contatos),
  },
  {
    path: ':id',
    title: 'Contato — Gestão Advocacia',
    loadComponent: () => import('./pages/contatos').then((m) => m.Contatos),
  },
];
