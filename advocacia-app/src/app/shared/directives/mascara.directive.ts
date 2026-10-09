import { Directive, ElementRef, HostListener, Input, OnChanges, forwardRef, inject } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

export type TipoMascara = '' | 'telefone' | 'cpf' | 'cnpj';
export function formatarNumero(valor: string, tipo: TipoMascara): string {
  if (!tipo) return valor;
  const n = valor.replace(/\D/g, '').slice(0, tipo === 'cnpj' ? 14 : 11);
  const grupos = tipo === 'cpf' ? [3, 3, 3, 2] : tipo === 'cnpj' ? [2, 3, 3, 4, 2] : [2, n.length > 10 ? 5 : 4, 4];
  const separadores = tipo === 'cpf' ? ['.', '.', '-'] : tipo === 'cnpj' ? ['.', '.', '/', '-'] : [') ', '-'];
  let saida = tipo === 'telefone' && n ? '(' : '';
  let pos = 0;
  grupos.forEach((tamanho, i) => {
    if (n.length > pos) saida += (i ? separadores[i - 1] : '') + n.slice(pos, pos + tamanho);
    pos += tamanho;
  });
  return saida;
}

@Directive({ selector: 'input[appMascara]', providers: [{ provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => MascaraDirective), multi: true }] })
export class MascaraDirective implements ControlValueAccessor, OnChanges {
  @Input() appMascara: TipoMascara = '';
  private readonly campo = inject<ElementRef<HTMLInputElement>>(ElementRef);
  private valor = '';
  private mudar: (valor: string) => void = () => {};
  private tocar: () => void = () => {};
  writeValue(valor: unknown) { this.valor = String(valor ?? ''); this.exibir(); }
  ngOnChanges() { this.exibir(); }
  private exibir() {
    this.campo.nativeElement.value = formatarNumero(this.valor, this.appMascara);
    this.campo.nativeElement.inputMode = this.appMascara === 'telefone' ? 'tel' : this.appMascara ? 'numeric' : '';
  }
  registerOnChange(fn: (valor: string) => void) { this.mudar = fn; }
  registerOnTouched(fn: () => void) { this.tocar = fn; }
  setDisabledState(disabled: boolean) { this.campo.nativeElement.disabled = disabled; }
  @HostListener('blur') blur() { this.tocar(); }
  @HostListener('input') input() {
    const campo = this.campo.nativeElement;
    const digitosAntes = campo.value.slice(0, campo.selectionStart ?? campo.value.length).replace(/\D/g, '').length;
    this.valor = this.appMascara ? campo.value.replace(/\D/g, '').slice(0, this.appMascara === 'cnpj' ? 14 : 11) : campo.value;
    this.exibir();
    if (this.appMascara && campo.type !== 'date') {
      let pos = 0, total = 0;
      while (pos < campo.value.length && total < digitosAntes) { if (/\d/.test(campo.value[pos])) total++; pos++; }
      campo.setSelectionRange(pos, pos);
    }
    this.mudar(this.valor);
  }
}
