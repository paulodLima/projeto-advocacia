import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { AuthService } from '../auth/auth.service';

export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.carregarSessao().pipe(
    map(() => true),
    catchError(() => {
      auth.usuario.set(null);
      return of(router.createUrlTree(['/login']));
    }),
  );
};
