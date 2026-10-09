import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { MascaraDirective, TipoMascara, formatarNumero } from './mascara.directive';

@Component({ imports: [FormsModule, MascaraDirective], template: '<input [appMascara]="tipo()" [(ngModel)]="valor" />' })
class FormTeste { tipo = signal<TipoMascara>('telefone'); valor = signal('61999991234'); }

describe('Máscaras de cadastro', () => {
  it('formata telefone fixo, celular, CPF e CNPJ, inclusive valores parciais', () => {
    expect(formatarNumero('6133331234', 'telefone')).toBe('(61) 3333-1234');
    expect(formatarNumero('61999991234', 'telefone')).toBe('(61) 99999-1234');
    expect(formatarNumero('12345678901', 'cpf')).toBe('123.456.789-01');
    expect(formatarNumero('12345678000190', 'cnpj')).toBe('12.345.678/0001-90');
    expect(formatarNumero('1234', 'cpf')).toBe('123.4');
    expect(formatarNumero('', 'telefone')).toBe('');
  });
  it('exibe valores do banco com máscara, preserva cursor e entrega modelo sem pontuação', async () => {
    const fixture = TestBed.createComponent(FormTeste); await fixture.whenStable();
    const campo = fixture.nativeElement.querySelector('input') as HTMLInputElement;
    expect(campo.value).toBe('(61) 99999-1234');
    campo.value = '(11) 98888-1234'; campo.setSelectionRange(8, 8);
    campo.dispatchEvent(new Event('input')); await fixture.whenStable();
    expect(fixture.componentInstance.valor()).toBe('11988881234');
    expect(campo.selectionStart).toBe(8);
    campo.value = ''; campo.dispatchEvent(new Event('input')); await fixture.whenStable();
    expect(fixture.componentInstance.valor()).toBe('');
    fixture.componentInstance.tipo.set('cnpj'); fixture.componentInstance.valor.set('12345678000190');
    fixture.changeDetectorRef.markForCheck();
    fixture.detectChanges();
    await fixture.whenStable(); expect(campo.value).toBe('12.345.678/0001-90');
    TestBed.resetTestingModule();
  });
});
