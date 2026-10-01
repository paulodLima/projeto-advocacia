import { HttpClient, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { switchMap } from 'rxjs';
import { API_URL } from '../config/api-url.token';

export const sessionInterceptor: HttpInterceptorFn = (request, next) => {
  const apiUrl = inject(API_URL).replace(/\/$/, '');
  if (!request.url.startsWith(apiUrl + '/')) {
    return next(request);
  }
  const authenticatedRequest = request.clone({ withCredentials: true });
  if (['GET', 'HEAD', 'OPTIONS'].includes(request.method)) {
    return next(authenticatedRequest);
  }
  // O HttpClient intercepta também o GET, que não passa por este ramo.
  return inject(HttpClient).get<{ token: string; headerName: string }>(
    apiUrl + '/api/auth/csrf', { withCredentials: true },
  ).pipe(switchMap((csrf) => next(authenticatedRequest.clone({
    setHeaders: { [csrf.headerName]: csrf.token },
  }))));
};
