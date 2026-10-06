import { inject } from '@angular/core';
import { CanActivateChildFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { EscritorioService } from '../escritorio/escritorio.service';
import { PlataformaService } from '../plataforma/plataforma.service';

export const escritorioGuard: CanActivateChildFn = (route) => {
  if (route.routeConfig?.path === 'escritorios') return true;
  const escritorios = inject(EscritorioService);
  const router = inject(Router);
  if (route.routeConfig?.path === 'inicio' && inject(PlataformaService).podeAdministrar()) {
    return router.createUrlTree(['/administracao']);
  }
  return escritorios.carregarContexto().pipe(
    map(() => true),
    catchError(() => {
      escritorios.ativo.set(null);
      return of(router.createUrlTree(['/escritorios']));
    }),
  );
};
