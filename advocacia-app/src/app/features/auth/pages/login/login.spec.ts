import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { API_URL } from '../../../../core/config/api-url.token';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';
import { AuthService } from '../../../../core/auth/auth.service';
import { Login } from './login';
import { IdentidadeEmpresaService } from '../../../../core/branding/identidade-empresa.service';

describe('Login', () => {
  const auth = {
    solicitarCodigo: vi.fn(() => of({ desafioId: 'desafio', mensagem: 'Verifique seu email.' })),
    validarCodigo: vi.fn(),
    provedores: vi.fn(() => of({ google: false })),
  };

  beforeEach(async () => {
    auth.solicitarCodigo.mockClear();
    await TestBed.configureTestingModule({
      imports: [Login],
      providers: [provideHttpClient(), provideHttpClientTesting(), { provide: API_URL, useValue: '' }, provideRouter([]), { provide: AuthService, useValue: auth }],
    }).compileComponents();
  });

  it('carrega a marca pública pelo link do escritório sem alterar o fluxo de autenticação', async () => {
    const id = '00000000-0000-0000-0000-000000000001';
    const params = convertToParamMap({ empresa: id });
    TestBed.overrideProvider(ActivatedRoute, { useValue: { snapshot: { queryParamMap: params }, queryParamMap: of(params) } });
    const fixture = TestBed.createComponent(Login);
    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/api/public/empresas/' + id + '/identidade').flush({ empresaId: id, nome: 'Escritório A',
      identidade: { imagens: { foto_login: '/foto.jpg' }, usos: {}, zoom: 1.5, posX: 30, posY: 70 } });
    fixture.detectChanges(); await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('.marca-nome').textContent).toBe('Escritório A');
    expect(fixture.nativeElement.querySelector('.tela-login-foto img').style.objectPosition).toBe('30% 70%');
    expect(fixture.nativeElement.querySelector('.tela-login-foto img').style.transform).toBe('scale(1.5)');
    expect(fixture.nativeElement.querySelector('input[type=email]')).not.toBeNull();
    http.verify();
  });

  it('shows email as the main option without a fixed domain', async () => {
    const fixture = TestBed.createComponent(Login);
    fixture.detectChanges();
    await fixture.whenStable();
    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelector('input[type="email"]')).not.toBeNull();
    expect(element.textContent).toContain('Gestão Advocacia');
    expect(element.textContent).not.toContain('@andressaborges.adv.br');
    const google = element.querySelector<HTMLButtonElement>('[aria-label="Entrar com Google"]')!;
    expect(google.querySelector('svg')).not.toBeNull();
    expect(google.textContent?.trim()).toBe('');
    google.click();
    fixture.detectChanges();
    expect(element.querySelector('[role="status"]')?.textContent).toContain('Use seu email');
  });

  it('updates name, logo and login image using the company identity', async () => {
    const fixture = TestBed.createComponent(Login);
    fixture.detectChanges();
    const marca = TestBed.inject(IdentidadeEmpresaService);
    marca.aplicar({
      nome: 'Empresa de teste',
      logoUrl: '/images/empresa-teste.svg',
      imagemLoginUrl: '/images/empresa-teste.jpeg',
    });
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelector('.marca-nome')?.textContent).toBe('Empresa de teste');
    expect(element.querySelector('.marca-simbolo img')?.getAttribute('src')).toBe('/images/empresa-teste.svg');
    expect(element.querySelector('.tela-login-foto img')?.getAttribute('src')).toBe('/images/empresa-teste.jpeg');
    marca.aplicar({ nome: 'Outra empresa' });
    fixture.detectChanges();
    expect(element.querySelector('.marca-simbolo img')?.getAttribute('src')).toBe('/images/logo-ga.svg');
  });

  it('requests a code and moves to the verification form', async () => {
    const fixture = TestBed.createComponent(Login);
    fixture.detectChanges();
    await fixture.whenStable();
    const element = fixture.nativeElement as HTMLElement;
    const input = element.querySelector<HTMLInputElement>('input[type="email"]')!;
    input.value = 'teste@gmail.com';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    await fixture.whenStable();
    element.querySelector('form')!.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    fixture.detectChanges();
    await fixture.whenStable();
    expect(auth.solicitarCodigo).toHaveBeenCalledWith('teste@gmail.com');
    expect(element.querySelector('input[autocomplete="one-time-code"]')).not.toBeNull();
    expect(element.textContent).toContain('Validar e entrar');
  });

  it('opens cadastro when a new email is validated', async () => {
    auth.validarCodigo.mockReturnValue(of({ cadastroPendente: true }));
    const router = TestBed.inject(Router);
    const navigate = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    const fixture = TestBed.createComponent(Login);
    fixture.detectChanges();
    await fixture.whenStable();
    const element = fixture.nativeElement as HTMLElement;
    const email = element.querySelector<HTMLInputElement>('input[type="email"]')!;
    email.value = 'novo@gmail.com';
    email.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    await fixture.whenStable();
    element.querySelector('form')!.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    fixture.detectChanges();
    await fixture.whenStable();
    const codigo = element.querySelector<HTMLInputElement>('input[autocomplete="one-time-code"]')!;
    codigo.value = '123456';
    codigo.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    await fixture.whenStable();
    element.querySelector('form')!.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    expect(navigate).toHaveBeenCalledWith('/cadastro');
  });
});
