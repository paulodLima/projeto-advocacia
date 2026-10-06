import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { PlataformaService } from '../plataforma/plataforma.service';

export const adminPlataformaGuard: CanActivateFn = () => {
  const plataforma = inject(PlataformaService);
  const router = inject(Router);
  return plataforma.carregarAcesso().pipe(
    map((acesso) => acesso.podeAdministrar ? true : router.createUrlTree(['/escritorios'])),
    catchError(() => {
      plataforma.podeAdministrar.set(false);
      return of(router.createUrlTree(['/login']));
    }),
  );
};
