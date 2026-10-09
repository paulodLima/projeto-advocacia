import { TestBed } from '@angular/core/testing';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { EditorModelo } from './editor-modelo';
describe('Editor dos modelos de documentos', () => {
  afterEach(() => TestBed.resetTestingModule());
  it('substitui a seleção atual ao colar e mantém o cursor para inserir uma variável', () => {
    const fixture = TestBed.createComponent(EditorModelo); fixture.detectChanges();
    const mudou = vi.fn(); fixture.componentInstance.registerOnChange(mudou);
    fixture.componentInstance.writeValue('<p>Texto anterior</p>');
    const campo = fixture.nativeElement.querySelector('[contenteditable]') as HTMLElement;
    campo.focus(); const range = document.createRange(); range.selectNodeContents(campo);
    const selecao = document.getSelection(); selecao?.removeAllRanges(); selecao?.addRange(range);
    const evento = new Event('paste', { bubbles: true, cancelable: true });
    Object.defineProperty(evento, 'clipboardData', { value: { getData: () => 'Texto novo' } });
    campo.dispatchEvent(evento);
    expect(campo.textContent).toBe('Texto novo');
    fixture.componentInstance.inserir('{{cliente}}');
    expect(campo.textContent).toBe('Texto novo{{cliente}}');
    expect(mudou).toHaveBeenCalledWith('Texto novo{{cliente}}');
  });
});
