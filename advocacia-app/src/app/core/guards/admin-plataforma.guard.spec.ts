import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, provideRouter } from '@angular/router';
import { Observable } from 'rxjs';
import { API_URL } from '../config/api-url.token';
import { PlataformaService } from '../plataforma/plataforma.service';
import { adminPlataformaGuard } from './admin-plataforma.guard';
import { escritorioGuard } from './escritorio.guard';

describe('Administração da plataforma', () => {
  beforeEach(() => TestBed.configureTestingModule({
    providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([]),
      { provide: API_URL, useValue: 'http://localhost:8080' }],
  }));
  afterEach(() => TestBed.inject(HttpTestingController).verify());
  function executar() {
    return TestBed.runInInjectionContext(() =>
      adminPlataformaGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot)) as Observable<unknown>;
  }
  it('permite administrador sem empresa e revalida o privilégio', () => {
    let resultado: unknown;
    executar().subscribe(v => resultado = v);
    TestBed.inject(HttpTestingController).expectOne('http://localhost:8080/api/plataforma/acesso')
      .flush({ podeAdministrar: true });
    expect(resultado).toBe(true);
    const entrada = TestBed.runInInjectionContext(() => escritorioGuard(
      { routeConfig: { path: 'inicio' } } as ActivatedRouteSnapshot, {} as RouterStateSnapshot));
    expect(TestBed.inject(Router).serializeUrl(entrada as ReturnType<Router['createUrlTree']>))
      .toBe('/administracao');
    TestBed.inject(HttpTestingController).expectNone('http://localhost:8080/api/escritorios/contexto');
  });
  it('remove autorização obsoleta e não libera administração para usuário comum', () => {
    TestBed.inject(PlataformaService).podeAdministrar.set(true);
    let resultado: unknown;
    executar().subscribe(v => resultado = v);
    TestBed.inject(HttpTestingController).expectOne('http://localhost:8080/api/plataforma/acesso')
      .flush({ podeAdministrar: false });
    expect(TestBed.inject(PlataformaService).podeAdministrar()).toBe(false);
    expect(TestBed.inject(Router).serializeUrl(resultado as ReturnType<Router['createUrlTree']>))
      .toBe('/escritorios');
  });
});
