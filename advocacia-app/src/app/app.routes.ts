import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { ITENS_MENU } from './core/navigation/menu.model';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'login' },
  {
    path: '',
    loadChildren: () => import('./features/auth/auth.routes').then((module) => module.authRoutes),
  },
  {
    path: '',
    canActivate: [authGuard],
    canActivateChild: [authGuard],
    loadComponent: () => import('./layouts/authenticated-layout/authenticated-layout').then((module) => module.AuthenticatedLayout),
    children: [
      {
        path: 'inicio', title: 'Início — Gestão Advocacia',
        loadComponent: () => import('./features/auth/pages/inicio/inicio').then((module) => module.Inicio),
      },
      ...ITENS_MENU.filter((item) => item.id !== 'inicio').map((item) => ({
        path: item.id, title: item.label + ' — Gestão Advocacia', data: { label: item.label, icon: item.icon },
        loadComponent: () => import('./shared/pages/modulo-em-breve/modulo-em-breve').then((module) => module.ModuloEmBreve),
      })),
    ],
  },
  { path: '**', redirectTo: 'login' },
];
