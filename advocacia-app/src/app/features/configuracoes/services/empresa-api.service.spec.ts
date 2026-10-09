import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { afterEach, describe, expect, it } from 'vitest';
import { API_URL } from '../../../core/config/api-url.token';
import { AuthService } from '../../../core/auth/auth.service';
import { EmpresaApiService } from './empresa-api.service';

describe('Contexto da empresa', () => {
  afterEach(() => TestBed.resetTestingModule());
  it('limpa a empresa ao trocar de usuário e ignora respostas da conta anterior', () => {
    const usuario = signal<{ id: string } | null>({ id: 'a' });
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), { provide: API_URL, useValue: '' }, { provide: AuthService, useValue: { usuario } }] });
    const api = TestBed.inject(EmpresaApiService); const http = TestBed.inject(HttpTestingController);
    api.carregar().subscribe(); http.expectOne('/api/empresas/minha').flush({ id: 'a', papel: 'MASTER', dados: {} });
    expect(api.empresa()?.id).toBe('a');
    api.carregar().subscribe(); const anterior = http.expectOne('/api/empresas/minha');
    usuario.set({ id: 'b' }); TestBed.tick();
    expect(api.empresa()).toBeNull();
    anterior.flush({ id: 'a', papel: 'MASTER', dados: {} });
    expect(api.empresa()).toBeNull();
    api.carregar().subscribe(); http.expectOne('/api/empresas/minha').flush({ id: 'b', papel: 'MEMBRO', dados: {} });
    expect(api.empresa()?.id).toBe('b');
    usuario.set(null); TestBed.tick(); expect(api.empresa()).toBeNull();
    http.verify();
  });
});
