import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { AuthService } from '../../../core/auth/auth.service';
import { ConfiguracoesLocalService } from './configuracoes-local.service';
import { configuracoesIniciais } from '../models/configuracoes.model';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

describe('Configurações locais', () => {
  const usuario = signal({ id: 'teste-config-a', nome: 'Usuário A', email: 'a@example.test', status: 'ATIVO' });
  beforeEach(() => { usuario.set({ id: 'teste-config-a', nome: 'Usuário A', email: 'a@example.test', status: 'ATIVO' }); TestBed.configureTestingModule({ providers: [{ provide: AuthService, useValue: { usuario } }] }); });
  afterEach(() => { vi.restoreAllMocks(); localStorage.removeItem('configuracoes-front:teste-config-a'); localStorage.removeItem('configuracoes-front:teste-config-b'); TestBed.resetTestingModule(); });
  it('restaura antes de abrir o formulário e mantém rascunhos separados por usuário', () => {
    const salvo = configuracoesIniciais(); salvo.empresa['razao_social'] = 'Sociedade A';
    localStorage.setItem('configuracoes-front:teste-config-a', JSON.stringify(salvo));
    const store = TestBed.inject(ConfiguracoesLocalService);
    expect(store.dados().empresa['razao_social']).toBe('Sociedade A');
    expect(store.salvar(d => { d.perfil.telefone = '11999999999'; })).toBe(true);
    usuario.set({ id: 'teste-config-b', nome: 'Usuário B', email: 'b@example.test', status: 'ATIVO' }); TestBed.tick();
    expect(store.dados().empresa).toEqual({});
    expect(store.dados().perfil.telefone).toBe('');
    usuario.set({ id: 'teste-config-a', nome: 'Usuário A', email: 'a@example.test', status: 'ATIVO' }); TestBed.tick();
    expect(store.dados().perfil.telefone).toBe('11999999999');
  });
  it('mantém o rascunho anterior quando o navegador não consegue gravar', () => {
    const store = TestBed.inject(ConfiguracoesLocalService);
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => { throw new Error('quota'); });
    expect(store.salvar(d => { d.empresa['razao_social'] = 'Não gravada'; })).toBe(false);
    expect(store.dados().empresa['razao_social']).toBeUndefined();
    expect(store.aviso()).toContain('Não foi possível salvar');
  });
  it('recupera configurações incompletas ou corrompidas sem quebrar os formulários', () => {
    localStorage.setItem('configuracoes-front:teste-config-a', JSON.stringify({ versao: 1, perfil: null, financeiro: { custos: null }, workflows: 'inválido' }));
    const store = TestBed.inject(ConfiguracoesLocalService);
    expect(store.dados().perfil.telefone).toBe('');
    expect(store.dados().financeiro.custos).toEqual([]);
    expect(store.dados().workflows).toEqual([]);
  });
});
