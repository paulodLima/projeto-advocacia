import { Component, input, model } from '@angular/core';
import { Icone } from '../icone/icone';

@Component({
  selector: 'app-painel-inicio',
  imports: [Icone],
  template: `<section><header><button type="button" (click)="aberto.set(!aberto())" [attr.aria-expanded]="aberto()"><app-icone nome="chevron" [class.aberto]="aberto()" /><span>{{ titulo() }}</span>@if (!aberto() && resumo()) { <small>{{ resumo() }}</small> }</button><ng-content select="[acoes]" /></header>@if (aberto()) { <div class="corpo"><ng-content /></div> }</section>`,
  styles: `:host { display: block; } section { border-radius: 16px; background: rgba(255,255,255,.62); backdrop-filter: blur(22px) saturate(150%); border: 1px solid rgba(255,255,255,.7); box-shadow: 0 1px 2px #3a342d08, 0 8px 28px #3a342d0f; overflow: hidden; } header { display: flex; align-items: center; gap: 8px; padding: 16px 20px; } button { display: flex; align-items: center; flex: 1; flex-wrap: wrap; gap: 8px; border: 0; padding: 0; background: transparent; text-align: left; color: #2a2723; font: inherit; font-size: 13px; font-weight: 600; cursor: pointer; } button:focus-visible { outline: 2px solid #b8935a; outline-offset: 4px; } app-icone { width: 13px; height: 13px; color: #7d6c5e; } app-icone.aberto { transform: rotate(90deg); } small { color: #7d6c5e; font-size: 11px; font-weight: 400; } .corpo { padding: 0 20px 20px; }`,
})
export class PainelInicio {
  readonly titulo = input.required<string>();
  readonly resumo = input('');
  readonly aberto = model(true);
}
