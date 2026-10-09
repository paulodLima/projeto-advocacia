import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of } from 'rxjs';
import { ParceirosApiService } from '../services/parceiros-api.service';
import { parceirosRoutes } from '../parceiros.routes';
import { parceiroVazio } from '../models/parceiro.model';

describe('Navegação de parceiros', () => {
  afterEach(() => TestBed.resetTestingModule());
  it('atualiza ficha quando muda o id e não consulta carteira sem permissão de Contatos', async () => {
    const api = {
      opcoes: vi.fn(() =>
        of({ podeEditar: false, podeVerContatos: false, estados: [], areas: [] }),
      ),
      buscar: vi.fn((id: string) =>
        of({
          ...parceiroVazio(),
          dados: { ...parceiroVazio().dados, nome: 'Parceiro ' + id },
          id,
          empresaId: 'empresa',
          versao: 0,
        }),
      ),
      carteira: vi.fn(),
      listar: vi.fn(),
    };
    TestBed.configureTestingModule({
      providers: [
        provideRouter([
          { path: 'parceiros', data: { modulo: 'parceiros' }, children: parceirosRoutes },
        ]),
        { provide: ParceirosApiService, useValue: api },
      ],
    });
    const h = await RouterTestingHarness.create('/parceiros/primeiro');
    expect(h.routeNativeElement?.textContent).toContain('Parceiro primeiro');
    await h.navigateByUrl('/parceiros/segundo');
    expect(h.routeNativeElement?.textContent).toContain('Parceiro segundo');
    expect(h.routeNativeElement?.textContent).not.toContain('Parceiro primeiro');
    expect(api.carteira).not.toHaveBeenCalled();
    expect(h.routeNativeElement?.textContent).not.toContain('Excluir parceiro');
  });
});
