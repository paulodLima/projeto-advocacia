import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_URL } from '../../core/config/api-url.token';
import { GestaoEmpresas } from './gestao-empresas';

describe('Gestão de contratos das empresas', () => {
  let http: HttpTestingController;
  const url = '/api/plataforma/admin/gestao-empresas';
  const empresa = { id: 'empresa', nome: 'Escritório A', cnpj: '11222333000181', responsavel: 'Responsável', emailResponsavel: 'dono@example.test',
    usuariosAtivos: 2, situacao: 'ATIVA', financeiro: 'NAO_INFORMADO', inicio: null, versao: 0,
    modulos: [{ codigo: 'inicio', nome: 'Início', situacao: 'DISPONIVEL', liberado: true }, { codigo: 'config', nome: 'Configurações', situacao: 'DISPONIVEL', liberado: true }, { codigo: 'contatos', nome: 'Contatos', situacao: 'DISPONIVEL', liberado: true }], historico: [] };
  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [GestaoEmpresas], providers: [provideHttpClient(), provideHttpClientTesting(), { provide: API_URL, useValue: '' }] });
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => { http.verify(); TestBed.resetTestingModule(); });
  async function abrir() {
    const fixture = TestBed.createComponent(GestaoEmpresas);
    http.expectOne(r => r.url === url).flush([empresa]); await fixture.whenStable();
    fixture.nativeElement.querySelector('[aria-label="Gerenciar Escritório A"]').click();
    http.expectOne(url + '/empresa').flush(empresa); await fixture.whenStable();
    return fixture;
  }
  it('exige motivo e confirmação e só muda situação após sucesso do servidor', async () => {
    const fixture = await abrir();
    const componente = fixture.componentInstance as any;
    const selecao = fixture.nativeElement.querySelector('[name=situacao]') as HTMLSelectElement;
    selecao.value = 'SUSPENSA'; selecao.dispatchEvent(new Event('change')); await fixture.whenStable();
    componente.salvar(); http.expectNone(url + '/empresa');
    const motivo = fixture.nativeElement.querySelector('[name=motivo]') as HTMLTextAreaElement;
    motivo.value = 'Inadimplência'; motivo.dispatchEvent(new Event('input'));
    const confirmado = fixture.nativeElement.querySelector('[name=confirmado]') as HTMLInputElement;
    confirmado.click(); await fixture.whenStable();
    fixture.nativeElement.querySelector('.ficha form').dispatchEvent(new Event('submit', { cancelable: true }));
    const req = http.expectOne(url + '/empresa');
    expect(req.request.body).toEqual({ versao: 0, situacao: 'SUSPENSA', financeiro: 'NAO_INFORMADO', inicio: null, modulos: ['inicio', 'config', 'contatos'], motivo: 'Inadimplência' });
    expect(componente.empresas()[0].situacao).toBe('ATIVA');
    req.flush({ ...empresa, situacao: 'SUSPENSA', versao: 1 }); await fixture.whenStable();
    expect(componente.empresas()[0].situacao).toBe('SUSPENSA');
    expect(componente.confirmado).toBe(false);
    expect(fixture.nativeElement.textContent).toContain('Contrato atualizado');
  });
  it('preserva o rascunho e o estado confirmado quando há conflito de versão', async () => {
    const fixture = await abrir(); const componente = fixture.componentInstance as any;
    componente.situacao = 'ENCERRADA'; componente.motivo = 'Cancelamento solicitado'; componente.confirmado = true;
    componente.modulos.contatos = false; componente.salvar();
    http.expectOne(url + '/empresa').flush({ detail: 'Recarregue a ficha antes de salvar.' }, { status: 409, statusText: 'Conflict' });
    await fixture.whenStable();
    expect(componente.motivo).toBe('Cancelamento solicitado'); expect(componente.modulos.contatos).toBe(false);
    expect(componente.ficha().situacao).toBe('ATIVA');
    expect(fixture.nativeElement.querySelector('[role=alert]').textContent).toContain('Recarregue');
  });
});
