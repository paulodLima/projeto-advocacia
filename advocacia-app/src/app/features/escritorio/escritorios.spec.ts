import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { EscritorioService } from '../../core/escritorio/escritorio.service';
import { Escritorios } from './escritorios';

describe('Acesso comum às empresas', () => {
  it('orienta usuário sem vínculo e não oferece criação de empresa', async () => {
    await TestBed.configureTestingModule({
      imports: [Escritorios],
      providers: [provideRouter([]), { provide: EscritorioService, useValue: { listar: () => of([]) } }],
    }).compileComponents();
    const fixture = TestBed.createComponent(Escritorios);
    fixture.detectChanges();
    const tela = fixture.nativeElement as HTMLElement;
    expect(tela.textContent).toContain('Solicite a liberação ao administrador');
    expect(tela.querySelector('form')).toBeNull();
    expect(tela.textContent).not.toContain('Criar escritório');
  });
});
