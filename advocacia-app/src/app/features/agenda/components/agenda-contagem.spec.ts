import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_URL } from '../../../core/config/api-url.token';
import { AgendaContagem } from './agenda-contagem';
import { LembretesAgendaApiService } from '../services/lembretes-agenda-api.service';

describe('Contagem de lembretes no sino',()=>{
  it('atualiza sem abrir o painel, reage à leitura e oculta contagem indisponível',async()=>{
    vi.useFakeTimers();
    TestBed.configureTestingModule({imports:[AgendaContagem],providers:[provideHttpClient(),provideHttpClientTesting(),{provide:API_URL,useValue:''}]});
    const http=TestBed.inject(HttpTestingController),f=TestBed.createComponent(AgendaContagem);
    const contagens:(number|null)[]=[];f.componentInstance.contagem.subscribe(n=>contagens.push(n));
    await vi.advanceTimersByTimeAsync(1);
    const r=http.expectOne(r=>r.url==='/api/agenda/lembretes');expect(r.request.params.get('tamanho')).toBe('1');r.flush({total:3});
    f.detectChanges();expect(f.nativeElement.textContent).toContain('3');expect(contagens).toEqual([3]);
    TestBed.inject(LembretesAgendaApiService).notificarAlteracao();
    http.expectOne(r=>r.url==='/api/agenda/lembretes').flush({total:2});expect(contagens).toEqual([3,2]);
    await vi.advanceTimersByTimeAsync(45000);
    http.expectOne(r=>r.url==='/api/agenda/lembretes').flush({},{status:403,statusText:'Forbidden'});
    f.detectChanges();expect(f.nativeElement.querySelector('span')).toBeNull();expect(contagens.at(-1)).toBeNull();
    f.destroy();await vi.advanceTimersByTimeAsync(45000);http.expectNone(r=>r.url==='/api/agenda/lembretes');
    http.verify();TestBed.resetTestingModule();vi.useRealTimers();
  });
});
