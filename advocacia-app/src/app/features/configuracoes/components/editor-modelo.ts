import { DOCUMENT } from '@angular/common';
import { Component, ElementRef, forwardRef, inject, input, output, SecurityContext, viewChild } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { DomSanitizer } from '@angular/platform-browser';

@Component({ selector: 'app-editor-modelo', template: `
  <div class="editor"><div class="ferramentas" role="toolbar" aria-label="Formatação do texto">
    @for (ferramenta of ferramentas; track ferramenta.comando) { <button type="button" [disabled]="disabled()" [attr.aria-label]="ferramenta.nome" [title]="ferramenta.nome" (mousedown)="$event.preventDefault()" (click)="formatar(ferramenta.comando)">{{ ferramenta.simbolo }}</button> }
  </div><div #campo class="campo" [attr.contenteditable]="!disabled()" [attr.aria-disabled]="disabled()" role="textbox" aria-multiline="true" [attr.aria-label]="rotulo()" (input)="alterou()" (focus)="foco.emit(); guardarSelecao()" (blur)="tocou()" (keyup)="guardarSelecao()" (mouseup)="guardarSelecao()" (paste)="colar($event)"></div></div>
`, styles: `.editor { border:1px solid #6b635b24;border-radius:10px;overflow:hidden;background:#ffffff8c; } .ferramentas { display:flex;gap:4px;border-bottom:1px solid #6b635b24;padding:6px;background:#ffffff66;flex-wrap:wrap; } .ferramentas button { border:0;background:transparent;color:#6b635b;padding:4px 8px;font-size:12px;cursor:pointer;border-radius:4px; } .ferramentas button:hover { background:#e4dbd2; } .campo { min-height:120px;padding:12px;font-size:13px;outline:none;line-height:1.6; } .campo:focus { background:#ffffffcc; }`,
  providers: [{ provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => EditorModelo), multi: true }],
})
export class EditorModelo implements ControlValueAccessor {
  readonly disabled = input(false);
  readonly rotulo = input('Texto do documento'); readonly chave = input(''); readonly foco = output();
  private readonly campo = viewChild.required<ElementRef<HTMLElement>>('campo');
  private readonly sanitizer = inject(DomSanitizer); private readonly document = inject(DOCUMENT);
  protected readonly ferramentas = [
    { comando: 'bold', nome: 'Negrito', simbolo: 'B' }, { comando: 'italic', nome: 'Itálico', simbolo: 'I' },
    { comando: 'underline', nome: 'Sublinhado', simbolo: 'U' }, { comando: 'insertUnorderedList', nome: 'Lista', simbolo: '• Lista' },
    { comando: 'insertOrderedList', nome: 'Lista numerada', simbolo: '1. Lista' }, { comando: 'justifyLeft', nome: 'Alinhar à esquerda', simbolo: '≡' },
    { comando: 'justifyCenter', nome: 'Centralizar', simbolo: '☷' }, { comando: 'justifyFull', nome: 'Justificar', simbolo: '☰' },
    { comando: 'removeFormat', nome: 'Limpar formatação', simbolo: 'Tx' },
  ];
  private selecao: Range | null = null;
  private aoMudar: (texto: string) => void = () => {};
  protected tocou: () => void = () => {};
  writeValue(valor: string) { this.campo().nativeElement.innerHTML = this.sanitizer.sanitize(SecurityContext.HTML, valor ?? '') ?? ''; }
  registerOnChange(fn: (texto: string) => void) { this.aoMudar = fn; }
  registerOnTouched(fn: () => void) { this.tocou = fn; }
  protected alterou() { if (this.disabled()) return; this.aoMudar(this.sanitizer.sanitize(SecurityContext.HTML, this.campo().nativeElement.innerHTML) ?? ''); this.guardarSelecao(); }
  protected formatar(comando: string) { if (this.disabled()) return; this.campo().nativeElement.focus(); this.document.execCommand(comando); this.alterou(); }
  protected guardarSelecao() { const selecao = this.document.getSelection(); if (selecao?.rangeCount && this.campo().nativeElement.contains(selecao.anchorNode)) this.selecao = selecao.getRangeAt(0).cloneRange(); }
  inserir(texto: string) { if (this.disabled()) return;
    const campo = this.campo().nativeElement; campo.focus(); const selecao = this.document.getSelection();
    const range = this.selecao && campo.contains(this.selecao.commonAncestorContainer) ? this.selecao : this.document.createRange();
    if (!this.selecao || !campo.contains(range.commonAncestorContainer)) { range.selectNodeContents(campo); range.collapse(false); }
    range.deleteContents(); const node = this.document.createTextNode(texto); range.insertNode(node); range.setStartAfter(node); range.collapse(true);
    selecao?.removeAllRanges(); selecao?.addRange(range); this.alterou();
  }
  protected colar(event: ClipboardEvent) { event.preventDefault(); this.guardarSelecao(); this.inserir(event.clipboardData?.getData('text/plain') || ''); }
}

