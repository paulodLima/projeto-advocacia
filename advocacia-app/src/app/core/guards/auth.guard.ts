import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of, switchMap } from 'rxjs';
import { PlataformaService } from '../plataforma/plataforma.service';
import { AuthService } from '../auth/auth.service';

export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const plataforma = inject(PlataformaService);
  return auth.carregarSessao().pipe(
    switchMap(() => plataforma.carregarAcesso()),
    map(() => plataforma.podeAdministrar() && state.url === '/inicio' ? router.createUrlTree(['/administracao']) : true),
    catchError(() => {
      auth.usuario.set(null);
      plataforma.podeAdministrar.set(false);
      return of(router.createUrlTree(['/login']));
    }),
  );
};
