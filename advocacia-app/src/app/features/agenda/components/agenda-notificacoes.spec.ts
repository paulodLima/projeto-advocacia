import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { API_URL } from '../../../core/config/api-url.token';
import { AgendaNotificacoes } from './agenda-notificacoes';
import { LembreteAgenda, PainelLembretes } from '../services/lembretes-agenda-api.service';

describe('Lembretes pessoais da Agenda',()=>{
  let http:HttpTestingController;
  const url='/api/agenda/lembretes';
  const lembrete:LembreteAgenda={atividadeId:'00000000-0000-0000-0000-000000000099',versao:3,titulo:'Conferir contrato',tipo:'TAREFA',marco:'ATIVIDADE',data:'2026-10-16',hora:'09:00:00'};
  const painel:PainelLembretes={preferencias:{ativo:true,antecedenciaMinutos:1440},itens:[lembrete],total:1,pagina:0,tamanho:20,consultadoEm:'2026-10-16T12:00:00Z',fusoHorario:'America/Sao_Paulo'};
  beforeEach(()=>{
    TestBed.configureTestingModule({imports:[AgendaNotificacoes],providers:[provideRouter([]),provideHttpClient(),provideHttpClientTesting(),{provide:API_URL,useValue:''}]});
    http=TestBed.inject(HttpTestingController);
  });
  afterEach(()=>{http.verify();TestBed.resetTestingModule();vi.useRealTimers();});
  function criar(){
    const f=TestBed.createComponent(AgendaNotificacoes);
    const r=http.expectOne(r=>r.url===url);expect(r.request.params.get('tamanho')).toBe('20');r.flush(painel);return f;
  }
  it('só remove aviso depois da confirmação e mantém a lista em falha',async()=>{
    const f=criar();await f.whenStable();const c=f.componentInstance as any;
    c.ler(lembrete);expect(c.painel().itens.length).toBe(1);
    const r=http.expectOne(url+'/'+lembrete.atividadeId+'/lido');expect(r.request.body).toEqual({versao:3,marco:'ATIVIDADE'});
    r.flush({detail:'Aviso atualizado'},{status:404,statusText:'Not Found'});await f.whenStable();
    expect(c.painel().itens.length).toBe(1);expect(f.nativeElement.textContent).toContain('Aviso atualizado');
    c.ler(lembrete);http.expectOne(url+'/'+lembrete.atividadeId+'/lido').flush(null);
    http.expectOne(r=>r.url===url).flush({...painel,itens:[],total:0});await f.whenStable();
    expect(c.painel().total).toBe(0);expect(f.nativeElement.textContent).toContain('Nenhum lembrete pendente');
  });
  it('abre a atividade somente após a leitura confirmada pelo servidor',async()=>{
    const f=criar();const router=TestBed.inject(Router);const navegar=vi.spyOn(router,'navigate').mockResolvedValue(true);
    let abriu=false;f.componentInstance.abriu.subscribe(()=>abriu=true);
    const c=f.componentInstance as any;c.ler(lembrete,true);expect(navegar).not.toHaveBeenCalled();
    http.expectOne(url+'/'+lembrete.atividadeId+'/lido').flush(null);await f.whenStable();
    expect(navegar).toHaveBeenCalledWith(['/agenda'],{queryParams:{atividade:lembrete.atividadeId}});expect(abriu).toBe(true);
  });
  it('salva preferências pessoais, preserva rascunho na falha e recarrega após confirmação',async()=>{
    const f=criar();await f.whenStable();const c=f.componentInstance as any;
    c.configurar();c.preferencias={ativo:false,antecedenciaMinutos:60};c.salvarPreferencias();
    const r=http.expectOne(url+'/preferencias');expect(r.request.body).toEqual({ativo:false,antecedenciaMinutos:60});
    r.flush({},{status:500,statusText:'Failure'});await f.whenStable();
    expect(c.configurando()).toBe(true);expect(c.preferencias.ativo).toBe(false);
    c.salvarPreferencias();http.expectOne(url+'/preferencias').flush({ativo:false,antecedenciaMinutos:60});
    http.expectOne(r=>r.url===url).flush({...painel,preferencias:{ativo:false,antecedenciaMinutos:60},itens:[],total:0});
    await f.whenStable();expect(f.nativeElement.textContent).toContain('Seus lembretes estão desativados');
  });
  it('permissão revogada limpa os dados exibidos e permite tentar novamente',async()=>{
    const f=criar();await f.whenStable();const c=f.componentInstance as any;c.carregar();
    http.expectOne(r=>r.url===url).flush({},{status:403,statusText:'Forbidden'});await f.whenStable();
    expect(c.painel()).toBeNull();expect(f.nativeElement.textContent).not.toContain('Conferir contrato');
    c.carregar();http.expectOne(r=>r.url===url).flush(painel);await f.whenStable();expect(c.painel().total).toBe(1);
  });
  it('atualização periódica não sobrescreve preferências em edição e encerra ao fechar',async()=>{
    vi.useFakeTimers();const f=criar();const c=f.componentInstance as any;c.configurar();c.preferencias.antecedenciaMinutos=15;
    await vi.advanceTimersByTimeAsync(45000);http.expectNone(r=>r.url===url);expect(c.preferencias.antecedenciaMinutos).toBe(15);
    c.configurar();await vi.advanceTimersByTimeAsync(45000);http.expectOne(r=>r.url===url).flush(painel);
    f.destroy();await vi.advanceTimersByTimeAsync(45000);http.expectNone(r=>r.url===url);
  });
});
