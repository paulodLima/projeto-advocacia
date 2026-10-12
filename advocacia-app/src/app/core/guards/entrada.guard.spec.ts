import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { firstValueFrom, of } from 'rxjs';
import { EmpresaApiService } from '../../features/configuracoes/services/empresa-api.service';
import { entradaGuard } from './entrada.guard';

describe('Entrada do convidado', () => {
  const empresa = { carregar: vi.fn() };
  beforeEach(() => TestBed.configureTestingModule({ providers: [provideRouter([]), { provide: EmpresaApiService, useValue: empresa }] }));
  it('leva o responsável com convite confirmado ao cadastro do escritório', async () => {
    empresa.carregar.mockReturnValue(of({ id: null, podeCadastrar: true }));
    const resultado = await firstValueFrom(TestBed.runInInjectionContext(() => entradaGuard({} as any, {} as any)) as any);
    expect(TestBed.inject(Router).serializeUrl(resultado as any)).toBe('/config?secao=empresa');
  });
  it('mantém a entrada de quem já tem escritório', async () => {
    empresa.carregar.mockReturnValue(of({ id: 'empresa-a', podeCadastrar: false }));
    expect(await firstValueFrom(TestBed.runInInjectionContext(() => entradaGuard({} as any, {} as any)) as any)).toBe(true);
  });
});
