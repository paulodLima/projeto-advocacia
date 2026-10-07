import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { PERSONALIDADES, TAMANHOS_RESPOSTA } from '../models/referencia';
import { ConfiguracoesLocalService } from '../services/configuracoes-local.service';
import { Icone } from '../../../shared/components/icone/icone';
@Component({ selector: 'app-olivia-config', imports: [FormsModule, Icone], template: `
  <div class="cfg-pilha"><div class="cfg-titulo" style="margin:0"><app-icone nome="sparkles" /><div><h2>Olívia</h2><p>como ela responde à equipe</p></div></div>
    <p class="cfg-info">O que se escolhe aqui é o JEITO de responder. O que ela pode afirmar não muda: ela continua proibida de inventar informação, obrigada a consultar os dados de verdade antes de dar número, e a registrar o que não soube responder.</p>
    <section class="cfg-card cfg-pilha"><h3>Personalidade</h3>@for (personalidade of personalidades; track personalidade.id) { <button class="personalidade" [class.selecionada]="olivia.persona === personalidade.id" [attr.aria-pressed]="olivia.persona === personalidade.id" (click)="olivia.persona = personalidade.id"><div class="cfg-linha"><strong>{{ personalidade.nome }}</strong><span>{{ personalidade.resumo }}</span></div><p style="margin-top:6px;font-style:italic">“{{ personalidade.exemplo }}”</p></button> }</section>
    <section class="cfg-card cfg-pilha"><h3>Tamanho da resposta</h3><div class="cfg-linha">@for (tamanho of tamanhos; track tamanho.id) { <button class="personalidade cresce" [class.selecionada]="olivia.tamanho === tamanho.id" (click)="olivia.tamanho = tamanho.id"><h4>{{ tamanho.nome }}</h4><p>{{ tamanho.resumo }}</p></button> }</div></section>
    <section class="cfg-card cfg-pilha"><h3>Como ela chama a equipe</h3><input [(ngModel)]="olivia.tratamento" maxlength="60" aria-label="Como ela chama a equipe" placeholder="ex.: pelo primeiro nome · doutora · você" /><p>Vazio: ela usa o primeiro nome de quem perguntou.</p></section>
    <section class="cfg-card cfg-pilha"><h3>Instruções próprias do escritório</h3><textarea [(ngModel)]="olivia.instrucoes" rows="6" maxlength="1500" aria-label="Instruções próprias do escritório" placeholder="ex.: Nunca sugerir prazo processual sem mandar conferir no processo. Ao falar de honorários, lembrar da tabela da OAB/DF."></textarea><p>{{ olivia.instrucoes.length }} de 1500 caracteres. Entram depois das regras fixas, nunca por cima delas.</p></section>
    <div><button class="cfg-primario" (click)="salvar()">Salvar</button></div>
  </div>
`, styles: `.personalidade { width:100%;text-align:left;padding:12px;border-radius:12px; } .personalidade.selecionada { border-color:var(--tema-primaria);background:var(--tema-suave); } .personalidade strong { font-size:14px; } .personalidade span { font-size:11px;color:#7d6c5e; } .personalidade.selecionada strong { color:var(--tema-primaria); }` })
export class OliviaConfig {
  protected readonly store = inject(ConfiguracoesLocalService);
  protected readonly personalidades = PERSONALIDADES;
  protected readonly tamanhos = TAMANHOS_RESPOSTA;
  protected olivia = { ...this.store.dados().olivia };
  protected salvar() { this.store.salvar(d => { d.olivia = { ...this.olivia }; }); }
}
