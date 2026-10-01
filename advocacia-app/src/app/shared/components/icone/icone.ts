import { Component, input } from '@angular/core';

@Component({
  selector: 'app-icone',
  template: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><use [attr.href]="'/icons/navegacao.svg#' + nome()" /></svg>`,
  styles: `:host { display: inline-flex; width: 18px; height: 18px; flex: 0 0 auto; } svg { width: 100%; height: 100%; }`,
})
export class Icone { readonly nome = input.required<string>(); }
