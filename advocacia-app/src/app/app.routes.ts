import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { moduloGuard } from './core/guards/modulo.guard';
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
    canActivateChild: [authGuard, moduloGuard],
    loadComponent: () =>
      import('./layouts/authenticated-layout/authenticated-layout').then(
        (module) => module.AuthenticatedLayout,
      ),
    children: [
      {
        path: 'inicio',
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
        path: 'parceiros',
        data: { modulo: 'parceiros' },
        loadChildren: () => import('./features/parceiros/parceiros.routes').then(m=>m.parceirosRoutes),
      },
      ...ITENS_MENU.filter((item) => !['inicio', 'config', 'contatos', 'parceiros'].includes(item.id)).map(
        (item) => ({
          path: item.id,
          title: item.label + ' — Gestão Advocacia',
          data: { label: item.label, icon: item.icon },
          loadComponent: () =>
            import('./shared/pages/modulo-em-breve/modulo-em-breve').then(
              (module) => module.ModuloEmBreve,
            ),
        }),
      ),
    ],
  },
  { path: '**', redirectTo: 'login' },
];
