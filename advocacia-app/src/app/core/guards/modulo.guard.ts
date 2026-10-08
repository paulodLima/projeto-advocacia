import { inject } from '@angular/core';
import { CanActivateChildFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { AcessoService } from '../auth/acesso.service';

export const moduloGuard: CanActivateChildFn = route => {
  const acesso = inject(AcessoService);
  const router = inject(Router);
  const modulo = route.data?.['modulo'] ?? route.routeConfig?.path ?? 'inicio';
  return acesso.carregar().pipe(map(() => acesso.permite(modulo) || router.createUrlTree(['/inicio'])), catchError(() => of(router.createUrlTree(['/login']))));
};
