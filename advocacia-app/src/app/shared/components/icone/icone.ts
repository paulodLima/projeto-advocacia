import { Component, computed, input } from '@angular/core';

@Component({
  selector: 'app-icone',
  template: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><use [attr.href]="arquivo() + '#' + nome()" /></svg>`,
  styles: `:host { display: inline-flex; width: 18px; height: 18px; flex: 0 0 auto; } svg { width: 100%; height: 100%; }`,
})
export class Icone {
  readonly nome = input.required<string>();
  protected readonly arquivo = computed(() => ['workflow', 'lock', 'link'].includes(this.nome()) ? '/icons/configuracoes.svg' : '/icons/navegacao.svg');
}
