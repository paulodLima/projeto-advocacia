import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AuthService } from '../auth/auth.service';
import { API_URL } from '../config/api-url.token';
import { TemaService } from './tema.service';

describe('Tema na conta do usuário',()=>{
  const usuario=signal<{id:string;nome?:string}|null>({id:'a'});
  let http:HttpTestingController;
  beforeEach(()=>{
    usuario.set({id:'a'});
    TestBed.configureTestingModule({providers:[provideHttpClient(),provideHttpClientTesting(),{provide:API_URL,useValue:''},{provide:AuthService,useValue:{usuario}}]});
    http=TestBed.inject(HttpTestingController);
  });
  afterEach(()=>{http.verify();localStorage.removeItem('tema:a');TestBed.resetTestingModule();});
  function abrir(tema='verde') { const service=TestBed.inject(TemaService);TestBed.tick();http.expectOne('/api/usuarios/me/tema').flush({usuarioId:'a',tema});TestBed.tick();return service; }
  it('carrega do servidor, ignora preferências antigas locais e separa contas',()=>{
    localStorage.setItem('tema:a','vermelho');const service=abrir('roxo');expect(service.atual()).toBe('roxo');
    service.selecionar('amarelo');expect(service.atual()).toBe('amarelo');expect(service.salvando()).toBe(true);
    const req=http.expectOne('/api/usuarios/me/tema');expect(req.request.method).toBe('PUT');expect(req.request.body).toEqual({tema:'amarelo'});
    req.flush({usuarioId:'a',tema:'amarelo'});TestBed.tick();expect(service.aviso()).toContain('salvo na sua conta');expect(localStorage.getItem('tema:a')).toBe('vermelho');
    usuario.set({id:'b'});TestBed.tick();expect(service.atual()).toBe('verde');http.expectOne('/api/usuarios/me/tema').flush({usuarioId:'b',tema:'azul'});TestBed.tick();expect(service.atual()).toBe('azul');
    usuario.set(null);TestBed.tick();expect(service.atual()).toBe('verde');expect(service.pronto()).toBe(false);http.expectNone('/api/usuarios/me/tema');
  });
  it('salva mudanças rápidas na ordem, inclusive A-B-A, e só confirma a última',()=>{
    const service=abrir();service.selecionar('azul');const um=http.expectOne('/api/usuarios/me/tema');
    service.selecionar('roxo');service.selecionar('azul');expect(service.atual()).toBe('azul');http.expectNone('/api/usuarios/me/tema');
    um.flush({usuarioId:'a',tema:'azul'});expect(service.salvando()).toBe(true);const dois=http.expectOne('/api/usuarios/me/tema');expect(dois.request.body.tema).toBe('roxo');
    dois.flush({usuarioId:'a',tema:'roxo'});expect(service.atual()).toBe('azul');expect(service.salvando()).toBe(true);
    const tres=http.expectOne('/api/usuarios/me/tema');expect(tres.request.body.tema).toBe('azul');tres.flush({usuarioId:'a',tema:'azul'});expect(service.salvando()).toBe(false);
  });
  it('preserva a prévia sem confirmar gravação em falha e permite tentar novamente',()=>{
    const service=abrir();service.selecionar('preto');http.expectOne('/api/usuarios/me/tema').flush({},{status:503,statusText:'Unavailable'});
    expect(service.atual()).toBe('preto');expect(service.erro()).toBe(true);expect(service.aviso()).toContain('não foi salvo');
    service.tentarNovamente();http.expectOne('/api/usuarios/me/tema').flush({usuarioId:'a',tema:'preto'});expect(service.erro()).toBe(false);
  });
  it('cancela carga e fila da conta anterior ao trocar de usuário',()=>{
    const service=TestBed.inject(TemaService);TestBed.tick();const antiga=http.expectOne('/api/usuarios/me/tema');
    usuario.set({id:'b'});TestBed.tick();expect(antiga.cancelled).toBe(true);http.expectOne('/api/usuarios/me/tema').flush({usuarioId:'b',tema:'roxo'});
    service.selecionar('azul');const envio=http.expectOne('/api/usuarios/me/tema');service.selecionar('preto');usuario.set(null);TestBed.tick();expect(envio.cancelled).toBe(true);http.expectNone('/api/usuarios/me/tema');expect(service.atual()).toBe('verde');
  });
  it('permite repetir carga com falha, rejeita temas inválidos e não recarrega por mudança de nome',()=>{
    const service=TestBed.inject(TemaService);TestBed.tick();http.expectOne('/api/usuarios/me/tema').flush({},{status:503,statusText:'Unavailable'});
    expect(service.pronto()).toBe(false);service.selecionar('azul');http.expectNone('/api/usuarios/me/tema');
    service.tentarNovamente();http.expectOne('/api/usuarios/me/tema').flush({usuarioId:'a',tema:'verde'});TestBed.tick();
    service.selecionar('invalido');http.expectNone('/api/usuarios/me/tema');usuario.set({id:'a',nome:'Novo nome'});TestBed.tick();http.expectNone('/api/usuarios/me/tema');
  });
});
