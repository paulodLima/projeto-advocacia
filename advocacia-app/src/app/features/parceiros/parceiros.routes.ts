import { Routes } from '@angular/router';
export const parceirosRoutes: Routes = [
  {
    path: '',
    title: 'Parceiros — Gestão Advocacia',
    loadComponent: () => import('./pages/parceiros').then((m) => m.Parceiros),
  },
  {
    path: ':id/editar',
    title: 'Editar parceiro — Gestão Advocacia',
    data: { editar: true },
    loadComponent: () => import('./pages/parceiros').then((m) => m.Parceiros),
  },
  {
    path: ':id',
    title: 'Parceiro — Gestão Advocacia',
    loadComponent: () => import('./pages/parceiros').then((m) => m.Parceiros),
  },
];
