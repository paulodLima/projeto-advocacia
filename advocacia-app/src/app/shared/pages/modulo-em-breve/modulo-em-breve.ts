import { Component, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Icone } from '../../components/icone/icone';

@Component({
  selector: 'app-modulo-em-breve',
  imports: [RouterLink, Icone],
  template: `<section><span class="ilustracao"><app-icone [nome]="icone" /></span><p class="etiqueta">EM BREVE</p><h2>{{ titulo }}</h2><p>Este espaço está sendo preparado para você.</p><a routerLink="/inicio">Voltar ao início <app-icone nome="arrow" /></a></section>`,
  styles: `:host { display: block; } section { min-height: 420px; display: flex; flex-direction: column; align-items: center; justify-content: center; text-align: center; border-radius: 16px; background: #ffffff9e; border: 1px solid #ffffffb3; padding: 40px 24px; } .ilustracao { width: 80px; height: 80px; border-radius: 24px; background: #afb69533; display: grid; place-items: center; color: #4f5a49; margin-bottom: 24px; } .ilustracao app-icone { width: 36px; height: 36px; } .etiqueta { letter-spacing: .2em; font-size: 10px; color: #7d6c5e; } h2 { margin: 8px 0 12px; font: 400 28px 'Book Antiqua', Palatino, Georgia, serif; color: #2a2723; } p { color: #7d6c5e; font-size: 13px; } a { display: flex; align-items: center; gap: 8px; margin-top: 28px; color: #4f5a49; font-size: 12px; text-decoration: none; } a:focus-visible { outline: 2px solid #b8935a; outline-offset: 4px; }`,
})
export class ModuloEmBreve {
  private readonly route = inject(ActivatedRoute);
  protected readonly titulo = this.route.snapshot.data['label'] as string;
  protected readonly icone = this.route.snapshot.data['icon'] as string;
}
