import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { afterEach, describe, expect, it } from 'vitest';
import { AuthService } from './auth.service';
import { AcessoService } from './acesso.service';
import { API_URL } from '../config/api-url.token';

describe('Contexto de permissões', () => {
  afterEach(() => TestBed.resetTestingModule());
  it('remove o acesso master ao trocar de conta e ignora resposta atrasada da conta anterior', () => {
    const usuario = signal<{ id: string } | null>({ id: 'master' });
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), { provide: API_URL, useValue: '' }, { provide: AuthService, useValue: { usuario } }] });
    const api = TestBed.inject(AcessoService); const http = TestBed.inject(HttpTestingController);
    const master = { empresaId: 'a', papel: 'MASTER', perfil: 'ADMINISTRADOR', modulos: ['inicio', 'config', 'financeiro'], enviaDocumento: true };
    api.carregar().subscribe(); http.expectOne('/api/equipe/me').flush(master);
    expect(api.permiteConfiguracao('equipe')).toBe(true);
    api.carregar().subscribe(); const anterior = http.expectOne('/api/equipe/me');
    usuario.set({ id: 'membro' }); TestBed.tick(); expect(api.permiteConfiguracao('equipe')).toBe(false);
    anterior.flush(master); expect(api.permite('financeiro')).toBe(false);
    http.verify();
  });
  it('uma resposta antiga não restaura permissões revogadas', () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), { provide: API_URL, useValue: '' }, { provide: AuthService, useValue: { usuario: signal({ id: 'membro' }) } }] });
    const api = TestBed.inject(AcessoService); const http = TestBed.inject(HttpTestingController);
    api.carregar().subscribe(); const antigo = http.expectOne('/api/equipe/me');
    api.carregar().subscribe(); const atual = http.expectOne('/api/equipe/me');
    atual.flush({ empresaId: 'a', papel: 'MEMBRO', perfil: 'ASSISTENTE', modulos: ['inicio', 'config'], enviaDocumento: false });
    antigo.flush({ empresaId: 'a', papel: 'MEMBRO', perfil: 'ADMINISTRADOR', modulos: ['inicio', 'config', 'financeiro'], enviaDocumento: true });
    expect(api.permite('financeiro')).toBe(false); http.verify();
  });
});
