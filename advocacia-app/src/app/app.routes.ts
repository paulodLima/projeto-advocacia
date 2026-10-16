import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { adminPlataformaGuard } from './core/guards/admin-plataforma.guard';
import { moduloGuard } from './core/guards/modulo.guard';
import { entradaGuard } from './core/guards/entrada.guard';
import { ITENS_MENU } from './core/navigation/menu.model';

export const routes: Routes = [
  {
    path: 'administracao/empresas', redirectTo: 'administracao', pathMatch: 'full',
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
    canActivateChild: [authGuard, moduloGuard],
    loadComponent: () =>
      import('./layouts/authenticated-layout/authenticated-layout').then(
        (module) => module.AuthenticatedLayout,
      ),
    children: [
      {
        path: 'inicio',
        canActivate: [entradaGuard],
        title: 'Início — Gestão Advocacia',
        loadComponent: () =>
          import('./features/auth/pages/inicio/inicio').then((module) => module.Inicio),
      },
      {
        path: 'config',
        title: 'Configurações — Gestão Advocacia',
        loadComponent: () =>
          import('./features/configuracoes/pages/configuracoes/configuracoes').then(
            (module) => module.Configuracoes,
          ),
      },
      {
        path: 'contatos',
        data: { modulo: 'contatos' },
        loadChildren: () =>
          import('./features/cliente/cliente.routes').then((m) => m.clienteRoutes),
      },
      {
        path: 'agenda',
        data: { modulo: 'agenda' },
        loadChildren: () => import('./features/agenda/agenda.routes').then(m => m.agendaRoutes),
      },
      {
        path: 'parceiros',
        data: { modulo: 'parceiros' },
        loadChildren: () =>
          import('./features/parceiros/parceiros.routes').then((m) => m.parceirosRoutes),
      },
      {
        path: 'crm',
        title: 'CRM — Gestão Advocacia',
        data: { modulo: 'crm' },
        loadComponent: () => import('./features/crm/pages/crm').then((m) => m.Crm),
      },
      {
        path: 'casos',
        title: 'Casos — Gestão Advocacia',
        data: { modulo: 'casos' },
        loadComponent: () => import('./features/casos/pages/casos').then((m) => m.Casos),
      },
      ...ITENS_MENU.filter(
        (item) => !['inicio', 'config', 'contatos', 'parceiros', 'agenda', 'crm', 'casos'].includes(item.id),
      ).map((item) => ({
        path: item.id,
        title: item.label + ' — Gestão Advocacia',
        data: { label: item.label, icon: item.icon },
        loadComponent: () =>
          import('./shared/pages/modulo-em-breve/modulo-em-breve').then(
            (module) => module.ModuloEmBreve,
          ),
      })),
    ],
  },
  { path: '**', redirectTo: 'login' },
];
