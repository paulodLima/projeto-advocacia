import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize, Observable } from 'rxjs';
import { Cadastros, CadastrosApiService } from '../services/cadastros-api.service';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ConfiguracoesLocalService } from '../services/configuracoes-local.service';
import { LISTAS_CADASTRO, ListaCadastro, RegistroLocal, SistemaLocal } from '../models/configuracoes.model';
import { Icone } from '../../../shared/components/icone/icone';
import { GRUPOS_MENU } from '../../../core/navigation/menu.model';

@Component({ selector: 'app-cadastros-config', imports: [FormsModule, Icone], template: `
  <div class="cfg-titulo"><app-icone nome="settings" /><div><h2>Cadastros e sistemas</h2><p>as listas reutilizadas pela intranet, as pontes com o mundo lá fora e os atalhos para os sistemas</p></div></div>
  @if (aviso()) { <p class="cfg-info" [class.cfg-erro]="erro()" [attr.role]="erro() ? 'alert' : 'status'">{{ aviso() }}</p> }
  @if (carregando()) { <p role="status">Carregando cadastros…</p> }
  @if (!pronto() && !carregando()) { <button type="button" (click)="carregar()">Tentar novamente</button> }
  @if (pronto() && !dados()?.empresaId) { <p class="cfg-info">Cadastre sua empresa na aba Empresa antes de adicionar listas, rotinas ou sistemas.</p> }
  <fieldset class="cadastros-campos cfg-pilha" [disabled]="!podeEditar || ocupado()">
    <details class="cfg-card" open><summary><div><h3>Listas do escritório</h3><p>grupos de ação, fases, tipos de tarefa, etiquetas</p></div></summary><div class="cfg-detalhe cfg-pilha">
      <p class="cfg-info">Cada tipo de ação aponta para um grupo, cada etapa para uma fase, e cada tipo de tarefa para uma fase com pontuação. Grupos e fases vinculados a outros cadastros não podem ser excluídos. As relações são preservadas ao renomear um item.</p>
      @if (!lista) { <div class="cfg-grade tres cartoes">@for (item of listas; track item.id) { <button class="cfg-card cadastro-cartao" (click)="abrirLista(item)"><div class="cfg-linha"><span class="cadastro-icone"><app-icone nome="folder" /></span><h3 class="cresce">{{ item.nome }}</h3><strong>{{ (listasSalvas()[item.id] || []).length }}</strong></div><p>{{ item.dica }}</p></button> }</div> }
      @else { <div><button class="cfg-texto" (click)="lista = null">← Todos os cadastros</button><h3 style="margin-top:12px">{{ lista.nome }}</h3><p>{{ lista.dica }}</p></div>
        <form class="cfg-linha" (ngSubmit)="adicionarRegistro()">@for (campo of lista.campos; track campo.id) { <label class="cresce">{{ campo.label }}
          @if (campo.tipo === 'grupo' || campo.tipo === 'fase') { <select [name]="campo.id" [(ngModel)]="registro[campo.id]"><option value="">Selecione</option>@for (opcao of opcoes(campo.tipo); track opcao.id) { <option [value]="opcao.id">{{ opcao['nome'] }}</option> }</select> }
          @else { <input [name]="campo.id" [type]="campo.tipo || 'text'" [(ngModel)]="registro[campo.id]" [required]="$first" /> }
        </label> }<button class="cfg-primario" type="submit">Adicionar</button></form>
        <div class="cfg-tabela"><table><thead><tr>@for (campo of lista.campos; track campo.id) { <th>{{ campo.label }}</th> }<th>Ações</th></tr></thead><tbody>@for (linha of listasSalvas()[lista.id] || []; track linha.id) {
          <tr>@for (campo of lista.campos; track campo.id) { <td>@if (campo.tipo === 'grupo' || campo.tipo === 'fase') { <select [attr.aria-label]="campo.label" [(ngModel)]="rascunhos[linha.id][campo.id]"><option value="">Selecione</option>@for (opcao of opcoes(campo.tipo); track opcao.id) { <option [value]="opcao.id">{{ opcao['nome'] }}</option> }</select> } @else { <input [type]="campo.tipo === 'number' || campo.tipo === 'color' ? campo.tipo : 'text'" [attr.aria-label]="campo.label" maxlength="150" [(ngModel)]="rascunhos[linha.id][campo.id]" /> }</td> }<td><button class="cfg-primario" (click)="atualizarRegistro(linha.id)">Salvar</button><button class="cfg-texto" (click)="excluirRegistro(linha.id)">remover</button></td></tr>
        } @empty { <tr><td [attr.colspan]="lista.campos.length + 1" class="cfg-vazio">Nenhum item cadastrado.</td></tr> }</tbody></table></div>
      }
    </div></details>
    <details class="cfg-card"><summary><div><h3>Integrações</h3><p>Google, ASAAS, ADVBOX, Publicações Online</p></div></summary><div class="cfg-detalhe cfg-pilha">
      <p class="cfg-info">Listas, rotinas e sistemas são salvos no servidor. As conexões externas e suas sincronizações serão implementadas em uma etapa própria.</p>
      <div class="integracoes">@for (integracao of integracoes; track integracao.nome) { <section class="cfg-card"><div class="cfg-linha"><app-icone [nome]="integracao.icone" /><h3 class="cresce">{{ integracao.nome }}</h3><span class="cfg-badge">Não configurada</span></div><p style="margin-top:12px">{{ integracao.descricao }}</p><button style="margin-top:12px" (click)="mostrarIntegracao = integracao.nome">{{ integracao.nome === 'Google Agenda' ? 'conectar' : 'conferir configuração' }}</button></section> }</div>
      @if (mostrarIntegracao) { <p class="cfg-info" role="status">{{ mostrarIntegracao }}: integração pendente de backend. Nenhuma conexão foi realizada.</p> }
      <section class="cfg-card"><h3>Agendas do Google</h3><p>Escolha quais agendas entram na intranet e em qual delas a intranet cria audiências, reuniões e prazos. Agenda privada traz os compromissos só para você; o resto da equipe não os vê.</p><div class="cfg-linha"><button disabled>sincronizar agora</button><button disabled>atualizar lista</button></div><p class="cfg-vazio">Conecte uma conta para trazer as agendas.</p></section>
    </div></details>
    <details class="cfg-card"><summary><div><h3>Notícias</h3><p>buscar novidades, aprovar o que vai para o Início, fontes</p></div></summary><div class="cfg-detalhe"><div class="cfg-linha"><button disabled>Buscar novidades</button><span class="cfg-nota">Busca e publicação disponíveis após integrar o backend.</span></div><p class="cfg-vazio">Nenhuma notícia disponível para curadoria.</p></div></details>
    <details class="cfg-card"><summary><div><h3>Rotinas do escritório</h3><p>o que aparece para marcar na tela de Início</p></div></summary><div class="cfg-detalhe"><div class="cfg-grade">
      @for (periodo of periodos; track periodo.id) { <section><h4>{{ periodo.nome }}</h4><div class="cfg-pilha" style="gap:8px;margin-top:8px">@for (rotina of rotinas(periodo.id); track rotina.id) { <div class="cfg-linha"><span class="cresce">{{ rotina.nome }}</span><button class="cfg-texto" (click)="removerRotina(rotina.id)">remover</button></div> } @empty { <p>Nenhuma ainda.</p> }</div><form class="cfg-linha" style="margin-top:8px" (ngSubmit)="adicionarRotina(periodo.id)"><input class="cresce" name="rotina" [(ngModel)]="novaRotina[periodo.id]" placeholder="Nova rotina" aria-label="Nova rotina" required /><button type="submit" [attr.aria-label]="'Adicionar rotina ' + periodo.nome">+</button></form></section> }
    </div></div></details>
    <details class="cfg-card"><summary><div><h3>Sistemas</h3><p>os atalhos que aparecem na tela de Início</p></div></summary><div class="cfg-detalhe cfg-pilha">
      <section class="cfg-pilha"><h4>Páginas no Acesso rápido</h4><p>Selecione as páginas que aparecerão no Início da equipe. Cada pessoa verá apenas aquelas para as quais tem permissão.</p>
        <div class="cfg-grade">@for (grupo of gruposAtalhos; track grupo.titulo) { <section><h4>{{ grupo.titulo }}</h4>@for (pagina of grupo.itens; track pagina.id) { <label class="cfg-check"><input type="checkbox" [checked]="atalhos.includes(pagina.id)" (change)="alternarAtalho(pagina.id)" /><app-icone [nome]="pagina.icon" />{{ pagina.label }}</label> }</section> }</div>
        <div><button type="button" class="cfg-primario" (click)="salvarAtalhos()">Salvar atalhos</button></div>
      </section>
      <div class="cfg-linha"><p class="cresce">Os sistemas habilitados aparecem como atalho na tela de Início. Envie uma logo ou escolha um dos ícones disponíveis.</p><button class="cfg-primario" (click)="novoSistema()">+ Novo acesso rápido</button></div>
      <div class="integracoes">@for (sistema of sistemas; track sistema.id) { <section class="cfg-card"><div class="cfg-linha"><span class="cadastro-icone" [style.background]="sistema.cor">@if (sistema.logo) { <img [src]="sistema.logo" alt="Logo do sistema" /> } @else { <app-icone [nome]="sistema.icone" /> }</span><label class="cfg-check cresce"><input type="checkbox" [(ngModel)]="sistema.ativo" />aparece no Início da equipe</label><button class="cfg-texto" (click)="removerSistema(sistema.id)">remover</button></div>
        <label>Título<input [(ngModel)]="sistema.nome" placeholder="Título (ex.: ADVBOX)" /></label><label>Endereço<input [(ngModel)]="sistema.url" type="url" placeholder="https://…" /></label>
        <div class="cfg-linha"><label class="cresce">Ícone<select [(ngModel)]="sistema.icone"><option value="link">Link</option><option value="scale">Justiça</option><option value="calendar">Agenda</option><option value="file">Documento</option><option value="wallet">Financeiro</option></select></label><label>Cor<input type="color" [(ngModel)]="sistema.cor" /></label><label class="cfg-upload">Enviar logo<input type="file" accept="image/png,image/jpeg,image/webp,image/svg+xml" (change)="logoSistema($event, sistema)" /></label></div>
      </section> } @empty { <p class="cfg-vazio">Nenhum acesso rápido cadastrado.</p> }</div><div><button class="cfg-primario" (click)="salvarSistemas()">Salvar sistemas</button></div>
    </div></details>
  </fieldset>
`, styles: `.cadastros-campos { border:0;padding:0;margin:0;min-width:0; } .cadastro-cartao { text-align:left; } .cadastro-cartao p { margin-top:8px; } .cadastro-icone { width:44px;height:44px;border-radius:16px;background:var(--tema-clara-suave);display:grid;place-items:center;color:var(--tema-primaria);flex-shrink:0;overflow:hidden; } .cadastro-icone img { width:100%;height:100%;object-fit:contain; } .integracoes { display:grid;grid-template-columns:repeat(auto-fill,minmax(240px,1fr));gap:16px; }` })
export class CadastrosConfig {
  protected readonly store = inject(ConfiguracoesLocalService);
  protected readonly listas = LISTAS_CADASTRO;
  protected lista: ListaCadastro | null = null;
  protected registro: Record<string, string | number> = {};
  protected novaRotina: Record<string, string> = {};
  protected sistemas: SistemaLocal[] = [];
  protected atalhos: string[] = [];
  protected readonly gruposAtalhos = GRUPOS_MENU;
  protected alternarAtalho(id: string) {
    if (!this.podeEditar || this.ocupado()) return;
    this.atalhos = this.atalhos.includes(id) ? this.atalhos.filter(a => a !== id) : [...this.atalhos, id];
  }
  protected salvarAtalhos() {
    this.executar(this.api.salvarAtalhos([...this.atalhos]), atalhos => {
      this.atalhos = [...atalhos]; this.dados.update(d => d && ({ ...d, atalhos }));
    });
  }
  protected readonly api = inject(CadastrosApiService);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly dados = signal<Cadastros | null>(null);
  protected readonly listasSalvas = signal<Record<string, RegistroLocal[]>>({});
  protected rascunhos: Record<string, RegistroLocal> = {};
  protected readonly aviso = signal('');
  protected readonly erro = signal(false);
  protected readonly pronto = signal(false);
  protected readonly carregando = signal(false);
  protected readonly ocupado = signal(false);
  protected get podeEditar() { return this.pronto() && this.dados()?.papel === 'MASTER'; }
  constructor() { this.carregar(); }
  protected mostrarIntegracao = '';
  protected readonly periodos = [{ id: 'diaria', nome: 'Diárias' }, { id: 'semanal', nome: 'Semanais' }, { id: 'mensal', nome: 'Mensais' }, { id: 'anual', nome: 'Anuais' }];
  protected readonly integracoes = [
    { nome: 'Publicações Online', icone: 'scale', descricao: 'Intimações, publicações e distribuições. Alimenta a triagem em Gestão Processual.' },
    { nome: 'ADVBOX', icone: 'folder', descricao: 'Sincronização de processos, clientes e movimentações.' },
    { nome: 'ASAAS', icone: 'wallet', descricao: 'Cobrança de honorários por boleto, Pix e cartão, com baixa automática.' },
    { nome: 'Google Agenda', icone: 'calendar', descricao: 'Audiências, reuniões e prazos espelhados na agenda do escritório.' },
    { nome: 'Índices econômicos', icone: 'chart', descricao: 'IPCA, INPC, IGP-M, Selic e a tabela do TJDFT, para a correção monetária.' },
    { nome: 'ZapSign', icone: 'file', descricao: 'Assinatura eletrônica dos documentos do escritório.' },
    { nome: 'Claude (Olívia)', icone: 'sparkles', descricao: 'Assistente da intranet, responde com base nos POPs.' },
  ];
  protected carregar() {
    if (this.carregando()) return;
    this.carregando.set(true); this.aviso.set(''); this.store.aviso.set('');
    this.api.carregar().pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.carregando.set(false))).subscribe({
      next: dados => {
        this.dados.set(dados); this.listasSalvas.set(dados.listas); this.sistemas = structuredClone(dados.sistemas);
        this.atalhos = [...(dados.atalhos ?? ['agenda', 'casos', 'contatos', 'documentos'])];
        this.rascunhos = Object.fromEntries(Object.values(dados.listas).flat().map(item => [item.id, structuredClone(item)]));
        this.pronto.set(true); this.erro.set(false);
      }, error: erro => this.falha(erro),
    });
  }
  private falha(erro: { error?: { detail?: string; erros?: string[] } }) { this.erro.set(true); this.aviso.set(erro.error?.erros?.join(' · ') || erro.error?.detail || 'Não foi possível salvar ou carregar os cadastros. Tente novamente.'); }
  private executar<T>(request: Observable<T>, receber: (valor: T) => void) {
    if (!this.podeEditar || this.ocupado()) return;
    this.ocupado.set(true); this.aviso.set('');
    request.pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.ocupado.set(false))).subscribe({
      next: valor => { receber(valor); this.erro.set(false); this.aviso.set('Salvo no banco de dados para toda a empresa.'); }, error: erro => this.falha(erro),
    });
  }
  protected abrirLista(lista: ListaCadastro) { this.lista = lista; this.registro = {}; }
  protected opcoes(tipo: string) { return (this.listasSalvas()[tipo === 'grupo' ? 'grupos' : 'fases'] || []).filter(r => r.ativo); }
  private campos(lista: ListaCadastro, registro: Record<string, string | number | boolean>): Record<string, string> { return Object.fromEntries(lista.campos.map(c => [c.id, String(registro[c.id] ?? '').trim()])); }
  protected adicionarRegistro() {
    const lista = this.lista;
    if (!lista || !String(this.registro[lista.campos[0].id] || '').trim()) return;
    this.executar(this.api.criar(lista.id, this.campos(lista, this.registro)), item => {
      this.listasSalvas.update(atual => ({ ...atual, [lista.id]: [...(atual[lista.id] || []), item] }));
      this.rascunhos[item.id] = structuredClone(item); this.registro = {};
    });
  }
  protected atualizarRegistro(id: string) {
    const lista = this.lista; const item = this.rascunhos[id]; if (!lista || !item) return;
    this.executar(this.api.salvar(lista.id, item, this.campos(lista, item)), salvo => {
      this.listasSalvas.update(atual => ({ ...atual, [lista.id]: atual[lista.id].map(r => r.id === id ? salvo : r) }));
      this.rascunhos[id] = structuredClone(salvo);
    });
  }
  protected excluirRegistro(id: string) {
    const lista = this.lista; if (!lista) return;
    this.executar(this.api.excluir(lista.id, id), () => {
      this.listasSalvas.update(atual => ({ ...atual, [lista.id]: atual[lista.id].filter(r => r.id !== id) })); delete this.rascunhos[id];
    });
  }
  protected rotinas(periodo: string) { return this.dados()?.rotinas.filter(r => r.periodo === periodo) || []; }
  protected adicionarRotina(periodo: string) {
    const nome = this.novaRotina[periodo]?.trim(); if (!nome) return;
    this.executar(this.api.criarRotina(nome, periodo), rotina => { this.dados.update(d => d && ({ ...d, rotinas: [...d.rotinas, rotina] })); this.novaRotina[periodo] = ''; });
  }
  protected removerRotina(id: string) { this.executar(this.api.excluirRotina(id), () => this.dados.update(d => d && ({ ...d, rotinas: d.rotinas.filter(r => r.id !== id) }))); }
  protected novoSistema() { if (this.podeEditar && !this.ocupado() && this.sistemas.length < 30) this.sistemas.push({ id: this.store.id(), nome: '', url: '', ativo: true, icone: 'link', cor: '#e4dbd2', logo: '' }); }
  protected removerSistema(id: string) { if (this.podeEditar && !this.ocupado()) this.sistemas = this.sistemas.filter(s => s.id !== id); }
  protected async logoSistema(event: Event, sistema: SistemaLocal) {
    if (!this.podeEditar || this.ocupado()) return;
    const imagem = await this.store.imagem(event, 512, 'image/png');
    if (imagem && (imagem.length - imagem.indexOf(',') - 1) * 0.75 <= 256 * 1024) { sistema.logo = imagem; this.erro.set(false); this.aviso.set('Logo preparada. Clique em Salvar sistemas para publicar.'); }
    else { this.erro.set(true); this.aviso.set(imagem ? 'A logo preparada deve ter até 256 KB.' : this.store.aviso()); }
  }
  protected salvarSistemas() {
    if (this.sistemas.some(s => !s.nome.trim() || !/^https?:\/\//i.test(s.url))) { this.erro.set(true); this.aviso.set('Preencha o título e um endereço http:// ou https:// em cada sistema.'); return; }
    this.executar(this.api.salvarSistemas(structuredClone(this.sistemas)), sistemas => { this.sistemas = structuredClone(sistemas); this.dados.update(d => d && ({ ...d, sistemas })); });
  }
}

