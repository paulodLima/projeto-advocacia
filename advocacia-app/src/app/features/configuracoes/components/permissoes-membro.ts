import { Component, input } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ITENS_MENU } from '../../../core/navigation/menu.model';
import { ConfiguracaoMembro } from '../services/equipe-api.service';

@Component({ selector: 'app-permissoes-membro', imports: [FormsModule], template: `
  <div class="cfg-pilha">
    <div class="cfg-grade"><label>Nome de exibição<input [(ngModel)]="dados().nomeExibicao" [ngModelOptions]="standalone" maxlength="150" [disabled]="disabled()" /></label>
      <label>Perfil<select [(ngModel)]="dados().perfil" [ngModelOptions]="standalone" [disabled]="disabled()">@for (perfil of perfis; track perfil.id) { <option [value]="perfil.id">{{ perfil.nome }}</option> }</select></label>
      @if (!convite()) { <label>Telefone<input type="tel" maxlength="30" [(ngModel)]="dados().telefone" [ngModelOptions]="standalone" [disabled]="disabled()" /></label><label>Admissão<input type="date" [(ngModel)]="dados().admissao" [ngModelOptions]="standalone" [disabled]="disabled()" /></label><label>Nascimento<input type="date" [(ngModel)]="dados().nascimento" [ngModelOptions]="standalone" [disabled]="disabled()" /></label><label>Situação<select [(ngModel)]="dados().situacao" [ngModelOptions]="standalone" [disabled]="disabled()"><option value="ATIVO">Ativo</option><option value="INATIVO">Inativo</option><option value="SUSPENSO">Suspenso</option></select></label> }
    </div>
    <label class="cfg-check"><input type="checkbox" [(ngModel)]="dados().restrito" [ngModelOptions]="standalone" [disabled]="disabled()" />Restringir às abas escolhidas</label>
    @if (dados().restrito) { <div class="cfg-grade tres">@for (item of menu; track item.id) { <label class="cfg-check"><input type="checkbox" [checked]="dados().abas.includes(item.id)" [disabled]="disabled()" (change)="alternar(item.id)" />{{ item.label }}</label> }</div> }
    <p>Início e configurações pessoais ficam disponíveis. Sem restrição, os módulos seguem o perfil escolhido; apenas o master administra a empresa e a equipe.</p>
    <label class="cfg-check"><input type="checkbox" [(ngModel)]="dados().enviaDocumento" [ngModelOptions]="standalone" [disabled]="disabled()" />Pode enviar documentos para assinatura</label>
  </div>
` })
export class PermissoesMembro {
  readonly dados = input.required<ConfiguracaoMembro>();
  readonly disabled = input(false);
  readonly convite = input(false);
  protected readonly standalone = { standalone: true };
  protected readonly menu = ITENS_MENU.filter(item => !['inicio', 'config'].includes(item.id));
  protected readonly perfis = [{ id: 'ADMINISTRADOR', nome: 'Administrador' }, { id: 'ADVOGADO', nome: 'Advogado' }, { id: 'ASSISTENTE', nome: 'Assistente' }, { id: 'FINANCEIRO', nome: 'Financeiro' }];
  protected alternar(id: string) { this.dados().abas = this.dados().abas.includes(id) ? this.dados().abas.filter(aba => aba !== id) : [...this.dados().abas, id]; }
}
