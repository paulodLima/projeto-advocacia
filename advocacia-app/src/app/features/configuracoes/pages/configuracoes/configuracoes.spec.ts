import { DocumentosApiService } from '../../services/documentos-api.service';
import { WorkflowsApiService } from '../../services/workflows-api.service';
import { FinanceiroApiService } from '../../services/financeiro-api.service';
import { CofreApiService } from '../../services/cofre-api.service';
import { TemaService } from '../../../../core/theme/tema.service';
import { configuracoesIniciais } from '../../models/configuracoes.model';
import { signal } from '@angular/core';
import { of } from 'rxjs';
import { MeuPerfilApiService } from '../../services/meu-perfil-api.service';
import { EmpresaApiService } from '../../services/empresa-api.service';
import { EquipeApiService } from '../../services/equipe-api.service';
import { CadastrosApiService } from '../../services/cadastros-api.service';
import { AcessoService } from '../../../../core/auth/acesso.service';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { afterEach, describe, expect, it } from 'vitest';
import { AuthService } from '../../../../core/auth/auth.service';
import { ABAS_CONFIGURACAO } from '../../models/configuracoes.model';
import { Configuracoes } from './configuracoes';

describe('Tela de configurações', () => {
  afterEach(() => { localStorage.removeItem('configuracoes-front:teste-config-tela'); TestBed.resetTestingModule(); });
  it('abre todas as abas da referência, mantém Tema no final e permite voltar pelo endereço', async () => {
    TestBed.configureTestingModule({ providers: [provideRouter([{ path: 'config', component: Configuracoes }]), { provide: AuthService, useValue: { usuario: signal({ id: 'teste-config-tela', nome: 'Pessoa de teste', email: 'teste@example.test', status: 'ATIVO' }) } }] });
    TestBed.overrideProvider(MeuPerfilApiService, { useValue: { perfil: signal(null), carregar: () => of({ telefone: '', emailPessoal: '', endereco: '', foto: '', usuarioId: 'teste-config-tela' }) } });
    TestBed.overrideProvider(EmpresaApiService, { useValue: { empresa: signal({ id: null, papel: null, dados: {} }), carregar: () => of({ id: null, papel: null, dados: {} }) } });
    TestBed.overrideProvider(AcessoService, { useValue: { permiteConfiguracao: () => true } });
    TestBed.overrideProvider(TemaService, { useValue: { atual:signal('verde'), pronto:signal(true), carregando:signal(false), salvando:signal(false), erro:signal(false), aviso:signal('Tema salvo na sua conta.') } });
    TestBed.overrideProvider(EquipeApiService, { useValue: { carregar: () => of({ membros: [], convites: [] }) } });
    TestBed.overrideProvider(CadastrosApiService, { useValue: { carregar: () => of({ empresaId: null, papel: null, listas: {}, rotinas: [], sistemas: [] }) } });
    TestBed.overrideProvider(WorkflowsApiService, { useValue: { carregar: () => of({ empresaId: null, papel: null, workflows: [], tarefas: [], responsaveis: [] }) } });
    TestBed.overrideProvider(CofreApiService, { useValue: { carregar: () => of({ empresaId:null,podeEditar:false,configurado:true,credenciais:[] }) } });
    TestBed.overrideProvider(FinanceiroApiService, { useValue: { carregar: () => of({ empresaId:null, papel:null, versao:0, contas:[], categorias:[], centros:[], custos:[], urh:0, competencia:'', capacidade:configuracoesIniciais().financeiro }) } });
    TestBed.overrideProvider(DocumentosApiService, { useValue: { carregar: () => of({ empresaId: null, papel: null, documento: { logo: '', rodape: '', alinhamento: 'centro', repetir: false, documento: true, endereco: true, contato: true }, modelos: {} }) } });
    const harness = await RouterTestingHarness.create('/config');
    expect(harness.routeNativeElement?.querySelector('.cfg-tabs button:last-child')?.textContent?.trim()).toBe('Tema');
    for (const aba of ABAS_CONFIGURACAO) {
      await harness.navigateByUrl('/config?secao=' + aba.id, Configuracoes);
      expect(harness.routeNativeElement?.querySelector('.cfg-tabs button.ativo')?.textContent?.trim()).toBe(aba.label);
      expect(harness.routeNativeElement?.querySelector('h2')?.textContent?.trim()).toBe(aba.id === 'tema' ? 'Aparência' : aba.label);
    }
    await harness.navigateByUrl('/config?secao=inexistente', Configuracoes);
    expect(harness.routeNativeElement?.querySelector('h2')?.textContent).toBe('Meu perfil');
  });
  it('oculta configurações administrativas de membros e bloqueia o endereço direto de Equipe', async () => {
    TestBed.configureTestingModule({ providers: [provideRouter([{ path: 'config', component: Configuracoes }]), { provide: AuthService, useValue: { usuario: signal({ id: 'teste-config-tela', nome: 'Membro', email: 'membro@example.test', status: 'ATIVO' }) } }] });
    TestBed.overrideProvider(MeuPerfilApiService, { useValue: { perfil: signal(null), carregar: () => of({ telefone: '', emailPessoal: '', endereco: '', foto: '', usuarioId: 'teste-config-tela' }) } });
    TestBed.overrideProvider(AcessoService, { useValue: { permiteConfiguracao: (aba: string) => ['perfil', 'empresa', 'tema'].includes(aba) } });
    const harness = await RouterTestingHarness.create('/config?secao=equipe');
    expect(Array.from(harness.routeNativeElement!.querySelectorAll('.cfg-tabs button')).map(b => b.textContent?.trim())).toEqual(['Meu perfil', 'Empresa', 'Tema']);
    expect(harness.routeNativeElement?.querySelector('h2')?.textContent).toBe('Meu perfil');
  });
});

