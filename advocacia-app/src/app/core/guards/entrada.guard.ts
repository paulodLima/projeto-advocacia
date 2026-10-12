import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';
import { EmpresaApiService } from '../../features/configuracoes/services/empresa-api.service';

export const entradaGuard: CanActivateFn = () => {
  const router = inject(Router);
  return inject(EmpresaApiService).carregar().pipe(map(empresa => empresa.id ? true :
    router.createUrlTree(['/config'], { queryParams: { secao: empresa.podeCadastrar ? 'empresa' : 'perfil' } })));
};
