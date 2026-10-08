import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, provideRouter, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { firstValueFrom, Observable } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { AuthService } from '../auth/auth.service';
import { AcessoService } from '../auth/acesso.service';
import { API_URL } from '../config/api-url.token';
import { moduloGuard } from './modulo.guard';

describe('Permissões de navegação', () => {
  let http: HttpTestingController;
  beforeEach(() => { TestBed.configureTestingModule({ providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting(), { provide: API_URL, useValue: '' }, { provide: AuthService, useValue: { usuario: signal({ id: 'membro' }) } }] }); http = TestBed.inject(HttpTestingController); });
  afterEach(() => { http.verify(); TestBed.resetTestingModule(); });
  const resposta = { empresaId: 'empresa', papel: 'MEMBRO', perfil: 'ASSISTENTE', modulos: ['inicio', 'config', 'agenda'], enviaDocumento: false };
  function verificar(path: string) { return firstValueFrom(TestBed.runInInjectionContext(() => moduloGuard({ routeConfig: { path } } as ActivatedRouteSnapshot, {} as RouterStateSnapshot)) as Observable<boolean | UrlTree>); }
  it('consulta permissões atuais e bloqueia URL de módulo não autorizado', async () => {
    const resultado = verificar('financeiro'); http.expectOne('/api/equipe/me').flush(resposta);
    expect(TestBed.inject(Router).serializeUrl(await resultado as UrlTree)).toBe('/inicio');
    const permitido = verificar('agenda'); http.expectOne('/api/equipe/me').flush(resposta); expect(await permitido).toBe(true);
    expect(TestBed.inject(AcessoService).permiteConfiguracao('equipe')).toBe(false);
    expect(TestBed.inject(AcessoService).permiteConfiguracao('perfil')).toBe(true);
  });
  it('não abre o módulo quando o servidor de permissões falha', async () => {
    const resultado = verificar('financeiro'); http.expectOne('/api/equipe/me').flush({}, { status: 503, statusText: 'Unavailable' });
    expect(TestBed.inject(Router).serializeUrl(await resultado as UrlTree)).toBe('/login');
  });
  it('verifica contatos também nas rotas internas de listagem, ficha e edição', async () => {
    for (const path of ['', ':id', ':id/editar']) {
      const route = { routeConfig: { path }, data: { modulo: 'contatos' } } as unknown as ActivatedRouteSnapshot;
      const permitido = firstValueFrom(TestBed.runInInjectionContext(() => moduloGuard(route, {} as RouterStateSnapshot)) as Observable<boolean | UrlTree>);
      http.expectOne('/api/equipe/me').flush({ ...resposta, modulos: [...resposta.modulos, 'contatos'] });
      expect(await permitido).toBe(true);
      const bloqueado = firstValueFrom(TestBed.runInInjectionContext(() => moduloGuard(route, {} as RouterStateSnapshot)) as Observable<boolean | UrlTree>);
      http.expectOne('/api/equipe/me').flush(resposta);
      expect(TestBed.inject(Router).serializeUrl(await bloqueado as UrlTree)).toBe('/inicio');
    }
  });
});
