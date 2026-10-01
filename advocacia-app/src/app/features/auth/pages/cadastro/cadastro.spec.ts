import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';
import { AuthService } from '../../../../core/auth/auth.service';
import { Cadastro } from './cadastro';

describe('Cadastro', () => {
  const auth = {
    cadastroPendente: vi.fn(() => of({ email: 'validado@gmail.com' })),
    concluirCadastro: vi.fn(() => of({ id: 'id', nome: 'Novo usuário', email: 'validado@gmail.com', status: 'ATIVO' })),
    sair: vi.fn(() => of(undefined)),
  };
  beforeEach(async () => {
    auth.concluirCadastro.mockClear();
    await TestBed.configureTestingModule({
      imports: [Cadastro],
      providers: [provideRouter([]), { provide: AuthService, useValue: auth }],
    }).compileComponents();
  });
  it('keeps verified email read only and sends only the name before opening inicio', async () => {
    const router = TestBed.inject(Router);
    const navigate = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    const fixture = TestBed.createComponent(Cadastro);
    fixture.detectChanges();
    await fixture.whenStable();
    const element = fixture.nativeElement as HTMLElement;
    const email = element.querySelector<HTMLInputElement>('#email')!;
    expect(email.value).toBe('validado@gmail.com');
    expect(email.readOnly).toBe(true);
    const nome = element.querySelector<HTMLInputElement>('#nome')!;
    nome.value = '  Novo usuário  ';
    nome.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    await fixture.whenStable();
    element.querySelector('form')!.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    expect(auth.concluirCadastro).toHaveBeenCalledWith('Novo usuário');
    expect(navigate).toHaveBeenCalledWith('/inicio');
  });
});
