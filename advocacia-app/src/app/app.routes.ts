import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { adminPlataformaGuard } from './core/guards/admin-plataforma.guard';
import { escritorioGuard } from './core/guards/escritorio.guard';
import { ITENS_MENU } from './core/navigation/menu.model';

export const routes: Routes = [
  {
    path: 'administracao/empresas', title: 'Empresas — Administração da plataforma',
    canActivate: [authGuard, adminPlataformaGuard],
    loadComponent: () => import('./features/plataforma/admin-empresas').then((module) => module.AdminEmpresas),
  },
  {
    path: 'administracao', title: 'Administração da plataforma — Gestão Advocacia',
    canActivate: [authGuard, adminPlataformaGuard],
    loadComponent: () => import('./features/plataforma/admin-painel').then((module) => module.AdminPainel),
  },
  { path: '', pathMatch: 'full', redirectTo: 'login' },
  {
    path: '',
    loadChildren: () => import('./features/auth/auth.routes').then((module) => module.authRoutes),
  },
  {
    path: '',
    canActivate: [authGuard],
    canActivateChild: [authGuard, escritorioGuard],
    loadComponent: () => import('./layouts/authenticated-layout/authenticated-layout').then((module) => module.AuthenticatedLayout),
    children: [
      {
        path: 'escritorios', title: 'Escritórios — Gestão Advocacia',
        loadComponent: () => import('./features/escritorio/escritorios').then((module) => module.Escritorios),
      },
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
