import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize, Observable } from 'rxjs';
import { Workflow, WorkflowsApiService, WorkflowsDados } from '../services/workflows-api.service';
import { FormsModule } from '@angular/forms';

import { ConfiguracoesLocalService } from '../services/configuracoes-local.service';
import { Icone } from '../../../shared/components/icone/icone';
@Component({ selector: 'app-workflows-config', imports: [FormsModule, Icone], template: `
  <div class="cfg-pilha"><div class="cfg-titulo" style="margin:0"><app-icone nome="workflow" /><div><h2>Workflows</h2><p>cadeias de etapas que distribuem o prazo entre as pessoas</p></div></div>
    <p class="cfg-info">Cadastre as etapas, responsáveis, dias úteis e os tipos de tarefa que acionam cada workflow. A execução automática e o cálculo de datas serão integrados ao módulo de tarefas.</p>
    @if (aviso()) { <p class="cfg-info" [class.cfg-erro]="erro()" [attr.role]="erro() ? 'alert' : 'status'">{{ aviso() }}</p> }
    @if (carregando()) { <p role="status">Carregando workflows…</p> }
    @if (!pronto() && !carregando()) { <button (click)="carregar()">Tentar novamente</button> }
    @if (pronto()) { <button (click)="carregar()" [disabled]="ocupado() || carregando()">Recarregar do banco</button> }
    @if (pronto() && !dados()?.empresaId) { <p class="cfg-info">Cadastre sua empresa antes de criar workflows.</p> }
    <fieldset class="workflow-campos cfg-pilha" [disabled]="!podeEditar || ocupado()">
    <form class="cfg-linha" (ngSubmit)="criar()"><input name="nome" class="cresce" [(ngModel)]="nome" maxlength="150" placeholder="Nome do novo workflow…" aria-label="Nome do novo workflow" required /><button type="submit" class="cfg-primario">+ Criar</button></form>
    @for (workflow of workflows; track workflow.id) { <section class="cfg-card"><div class="cfg-linha"><button class="cfg-texto" [attr.aria-expanded]="aberto === workflow.id" (click)="aberto = aberto === workflow.id ? '' : workflow.id">{{ aberto === workflow.id ? '⌄' : '›' }}</button><input class="cresce" [(ngModel)]="workflow.nome" maxlength="150" aria-label="Nome do workflow" /><label class="cfg-check">D-<input type="number" [(ngModel)]="workflow.buffer" min="0" max="365" style="width:60px" /></label><button class="cfg-texto" (click)="remover(workflow.id)">Excluir ×</button></div>
      <p style="margin:8px 0">{{ workflow.etapas.length }} etapas · {{ dias(workflow) }} dias úteis</p>
      @if (aberto === workflow.id) { <div class="cfg-pilha" style="gap:12px;margin-left:24px">
        <div class="cfg-linha">@for (gatilho of workflow.gatilhos; track gatilho) { <span class="cfg-badge">{{ nomeGatilho(gatilho) }} <button class="cfg-texto" (click)="removerGatilho(workflow, gatilho)" [attr.aria-label]="'Remover gatilho ' + gatilho">×</button></span> }<select style="width:auto" aria-label="Adicionar gatilho" (change)="adicionarGatilho(workflow, $event)"><option value="">+ Adicionar gatilho…</option>@for (tarefa of tarefas; track tarefa.id) { <option [value]="tarefa.id">{{ tarefa.nome }}</option> }</select></div>
        @for (etapa of workflow.etapas; track etapa.id; let indice = $index) { <div class="cfg-linha"><span>{{ indice + 1 }}.</span><input class="cresce" [(ngModel)]="etapa.nome" placeholder="Etapa…" maxlength="150" aria-label="Nome da etapa" /><select class="cresce" [(ngModel)]="etapa.responsavel" aria-label="Responsável pela etapa"><option value="">— sem responsável —</option>@for (membro of dados()?.responsaveis || []; track membro.id) { <option [value]="membro.id" [disabled]="!membro.ativo">{{ membro.nome }}{{ membro.ativo ? '' : ' (inativo)' }}</option> }</select><label class="cfg-check"><input type="number" [(ngModel)]="etapa.dias" min="1" max="365" style="width:65px" />dias úteis</label><button class="cfg-texto" (click)="mover(workflow, indice, -1)" [disabled]="indice === 0" aria-label="Mover etapa para cima">↑</button><button class="cfg-texto" (click)="mover(workflow, indice, 1)" [disabled]="indice === workflow.etapas.length - 1" aria-label="Mover etapa para baixo">↓</button><button class="cfg-texto" (click)="removerEtapa(workflow, etapa.id)" aria-label="Excluir etapa">×</button></div> }
        <div class="cfg-linha"><button (click)="etapa(workflow)">+ Adicionar etapa</button><button class="cfg-primario" (click)="salvar(workflow)">Salvar workflow</button></div>
      </div> }
    </section> } @empty { <p class="cfg-vazio">Nenhum workflow cadastrado.</p> }
    </fieldset>
  </div>
`, styles: [`.workflow-campos { border: 0; padding: 0; margin: 0; min-width: 0; }`] })
export class WorkflowsConfig {
  protected readonly store = inject(ConfiguracoesLocalService);
  protected workflows: Workflow[] = [];
  protected nome = ''; protected aberto = '';
  private readonly api = inject(WorkflowsApiService);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly dados = signal<WorkflowsDados | null>(null);
  protected readonly pronto = signal(false);
  protected readonly carregando = signal(false);
  protected readonly ocupado = signal(false);
  protected readonly aviso = signal('');
  protected readonly erro = signal(false);
  protected get podeEditar() { return this.pronto() && this.dados()?.papel === 'MASTER'; }
  protected get tarefas() { return (this.dados()?.tarefas || []).filter(t => t.ativo); }
  protected nomeGatilho(id: string) { const t = this.dados()?.tarefas.find(t => t.id === id); return t ? t.nome + (t.ativo ? '' : ' (inativo)') : 'Tipo de tarefa indisponível'; }
  constructor() { this.store.aviso.set(''); this.carregar(); }
  protected carregar() {
    if (this.carregando() || this.ocupado()) return;
    this.pronto.set(false); this.carregando.set(true); this.aviso.set('');
    this.api.carregar().pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.carregando.set(false))).subscribe({
      next: dados => { this.dados.set(dados); this.workflows = structuredClone(dados.workflows); this.pronto.set(true); this.erro.set(false); }, error: erro => this.falha(erro),
    });
  }
  private falha(erro: { error?: { detail?: string; erros?: string[] } }) { this.erro.set(true); this.aviso.set(erro.error?.erros?.join(' · ') || erro.error?.detail || 'Não foi possível salvar ou carregar. Suas alterações permanecem na tela.'); }
  private executar<T>(request: Observable<T>, receber: (valor: T) => void) {
    if (!this.podeEditar || this.ocupado()) return;
    this.ocupado.set(true); this.aviso.set('');
    request.pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.ocupado.set(false))).subscribe({ next: valor => { receber(valor); this.erro.set(false); this.aviso.set('Workflow salvo no banco para toda a empresa.'); }, error: erro => this.falha(erro) });
  }
  protected dias(workflow: Workflow) { return workflow.etapas.reduce((s, e) => s + Number(e.dias || 0), 0); }
  protected criar() {
    if (!this.nome.trim()) return;
    this.executar(this.api.criar(this.nome.trim()), workflow => { this.workflows.push(workflow); this.nome = ''; this.aberto = workflow.id; });
  }
  protected remover(id: string) {
    const workflow = this.workflows.find(w => w.id === id); if (!workflow) return;
    this.executar(this.api.excluir(workflow), () => { this.workflows = this.workflows.filter(w => w.id !== id); if (this.aberto === id) this.aberto = ''; });
  }
  protected etapa(workflow: Workflow) { if (this.podeEditar && !this.ocupado()) workflow.etapas.push({ id: this.store.id(), nome: '', responsavel: '', dias: 1 }); }
  protected removerEtapa(workflow: Workflow, id: string) { if (this.podeEditar && !this.ocupado()) workflow.etapas = workflow.etapas.filter(e => e.id !== id); }
  protected mover(workflow: Workflow, indice: number, direcao: number) {
    if (!this.podeEditar || this.ocupado()) return;
    const alvo = indice + direcao; if (alvo >= 0 && alvo < workflow.etapas.length) [workflow.etapas[indice], workflow.etapas[alvo]] = [workflow.etapas[alvo], workflow.etapas[indice]];
  }
  protected adicionarGatilho(workflow: Workflow, event: Event) {
    if (!this.podeEditar || this.ocupado()) return;
    const select = event.target as HTMLSelectElement; if (select.value && !workflow.gatilhos.includes(select.value)) workflow.gatilhos.push(select.value); select.value = '';
  }
  protected removerGatilho(workflow: Workflow, gatilho: string) { if (this.podeEditar && !this.ocupado()) workflow.gatilhos = workflow.gatilhos.filter(g => g !== gatilho); }
  protected salvar(workflow: Workflow) {
    if (!workflow.nome.trim() || !Number.isInteger(Number(workflow.buffer)) || workflow.buffer < 0 || workflow.buffer > 365 || workflow.etapas.some(e => !e.nome.trim() || !Number.isInteger(Number(e.dias)) || e.dias < 1 || e.dias > 365)) {
      this.erro.set(true); this.aviso.set('Informe nomes, buffer de 0 a 365 e prazos inteiros de 1 a 365 dias úteis.'); return;
    }
    this.executar(this.api.salvar(structuredClone(workflow)), salvo => { this.workflows = this.workflows.map(w => w.id === salvo.id ? salvo : w); });
  }
}

