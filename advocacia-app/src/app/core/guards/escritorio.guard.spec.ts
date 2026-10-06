import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, provideRouter } from '@angular/router';
import { Observable } from 'rxjs';
import { API_URL } from '../config/api-url.token';
import { EscritorioService } from '../escritorio/escritorio.service';
import { escritorioGuard } from './escritorio.guard';

describe('Contexto do escritório nas rotas', () => {
  beforeEach(() => TestBed.configureTestingModule({
    providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([]),
      { provide: API_URL, useValue: 'http://localhost:8080' }],
  }));

  afterEach(() => TestBed.inject(HttpTestingController).verify());

  function executar(path: string) {
    const route = { routeConfig: { path } } as ActivatedRouteSnapshot;
    return TestBed.runInInjectionContext(() => escritorioGuard(route, {} as RouterStateSnapshot));
  }

  it('permite abrir a seleção sem escritório ativo', () => {
    expect(executar('escritorios')).toBe(true);
    TestBed.inject(HttpTestingController).expectNone('http://localhost:8080/api/escritorios/contexto');
  });

  it('revalida o contexto antes de liberar uma rota', () => {
    let resultado: unknown;
    (executar('inicio') as Observable<unknown>).subscribe((value) => resultado = value);
    const acesso = { id: 'escritorio-a', nome: 'Escritório A', perfil: 'PROPRIETARIO' };
    TestBed.inject(HttpTestingController).expectOne('http://localhost:8080/api/escritorios/contexto').flush(acesso);
    expect(resultado).toBe(true);
    expect(TestBed.inject(EscritorioService).ativo()?.id).toBe('escritorio-a');
  });

  it('remove contexto obsoleto e direciona para seleção quando o vínculo foi revogado', () => {
    TestBed.inject(EscritorioService).ativo.set({ id: 'a', nome: 'A', perfil: 'MEMBRO' });
    let resultado: unknown;
    (executar('inicio') as Observable<unknown>).subscribe((value) => resultado = value);
    TestBed.inject(HttpTestingController).expectOne('http://localhost:8080/api/escritorios/contexto')
      .flush({}, { status: 403, statusText: 'Forbidden' });
    expect(TestBed.inject(EscritorioService).ativo()).toBeNull();
    expect(TestBed.inject(Router).serializeUrl(resultado as ReturnType<Router['createUrlTree']>))
      .toBe('/escritorios');
  });
});
