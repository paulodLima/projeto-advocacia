import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize, Observable } from 'rxjs';
import { FinanceiroApiService, FinanceiroDados, financeiroRecebido } from '../services/financeiro-api.service';
import { FormsModule } from '@angular/forms';
import { CurrencyPipe } from '@angular/common';
import { ConfiguracoesLocalService } from '../services/configuracoes-local.service';
import { ContaLocal, CategoriaLocal, RegistroLocal, configuracoesIniciais } from '../models/configuracoes.model';
import { Icone } from '../../../shared/components/icone/icone';
import { CapacidadeFinanceira } from './capacidade-financeira';

@Component({ selector: 'app-financeiro-config', imports: [FormsModule, CurrencyPipe, Icone, CapacidadeFinanceira], template: `
  <div class="cfg-titulo"><app-icone nome="wallet" /><div><h2>Financeiro</h2><p>a estrutura interna do financeiro, do jeito do escritório</p></div></div>
  <nav class="cfg-tabs" aria-label="Configurações financeiras">@for (item of abas; track item.id) { <button [class.ativo]="aba === item.id" (click)="aba = item.id">{{ item.nome }}</button> }</nav>
  @if (aviso()) { <p class="cfg-info" [class.cfg-erro]="erro()" [attr.role]="erro() ? 'alert' : 'status'">{{ aviso() }}</p> }
  @if (carregando()) { <p role="status">Carregando financeiro…</p> }
  @if (!pronto() && !carregando()) { <button (click)="carregar()">Tentar novamente</button> }
  @if (pronto()) { <button (click)="carregar()" [disabled]="ocupado()">Recarregar do banco</button> }
  @if (pronto() && !dados()?.empresaId) { <p class="cfg-info">Cadastre a empresa antes de configurar o financeiro.</p> }
  <fieldset class="financeiro-campos" [disabled]="!podeEditar || ocupado()">
  @switch (aba) {
    @case ('contas') { <div class="cfg-pilha"><div class="cfg-linha"><button class="cfg-primario" (click)="novaConta()">Nova conta ou cartão</button><button (click)="todas = !todas">{{ todas ? 'Mostrando todas' : 'Mostrando só as ativas' }}</button></div>
      @if (conta) { <form class="cfg-card cfg-pilha" (ngSubmit)="salvarConta()"><div class="cfg-grade"><label>Nome *<input name="nome" maxlength="150" [(ngModel)]="conta.nome" required /></label><label>Banco / instituição financeira<input name="banco" maxlength="150" [(ngModel)]="conta.banco" /></label><label>Tipo<select name="tipo" [(ngModel)]="conta.tipo"><option value="conta">Conta bancária</option><option value="cartao">Cartão de crédito</option><option value="caixa">Caixa / dinheiro</option><option value="investimento">Investimento</option></select></label><label>Saldo inicial desta conta<input name="saldo" type="number" step="0.01" [(ngModel)]="conta.saldo" /></label></div><label class="cfg-check"><input name="padrao" type="checkbox" [(ngModel)]="conta.padrao" />Usar como conta padrão nos novos lançamentos</label><div class="cfg-linha"><button type="submit" class="cfg-primario">Salvar</button><button type="button" (click)="conta = null">Cancelar</button></div></form> }
      <div class="cfg-tabela"><table><thead><tr><th>Nome</th><th>Banco / instituição</th><th>Tipo</th><th>Conta padrão</th><th>Ações</th></tr></thead><tbody>@for (item of contasVisiveis; track item.id) { <tr><td>{{ item.nome }}</td><td>{{ item.banco || 'não informado' }}</td><td>{{ item.tipo }}</td><td><input type="radio" name="conta-padrao" [disabled]="!item.ativo" [checked]="item.padrao" (click)="$event.preventDefault(); padrao(item.id)" aria-label="Usar como conta padrão" /></td><td><div class="cfg-linha"><button class="cfg-texto" (click)="editarConta(item)">editar</button><button class="cfg-texto" (click)="alternarConta(item)">{{ item.ativo ? 'desativar' : 'reativar' }}</button></div></td></tr> } @empty { <tr><td colspan="5" class="cfg-vazio">Nenhuma conta ou cartão cadastrado.</td></tr> }</tbody></table></div>
    </div> }
    @case ('categorias') { <div class="cfg-pilha"><div class="cfg-linha"><button class="cfg-primario" (click)="novaCategoria()">Nova categoria</button><input class="cresce" style="max-width:260px" [(ngModel)]="busca" placeholder="Buscar categoria" aria-label="Buscar categoria" /><button (click)="todas = !todas">{{ todas ? 'Mostrando todas' : 'Mostrando só as ativas' }}</button><span class="cfg-nota">{{ categoriasVisiveis.length }} categorias</span></div>
      @if (categoria) { <form class="cfg-card cfg-pilha" (ngSubmit)="salvarCategoria()"><div class="cfg-grade tres"><label>Categoria *<input name="nome" maxlength="150" [(ngModel)]="categoria.nome" required /></label><label>Grupo<select name="grupo" [(ngModel)]="categoria.grupo">@for (grupo of grupos; track grupo) { <option>{{ grupo }}</option> }</select></label><label>Tipo<select name="tipo" [(ngModel)]="categoria.direcao"><option value="entrada">Receita</option><option value="saida">Despesa</option><option value="ambas">Serve para as duas</option></select></label></div><div class="cfg-linha"><button class="cfg-primario" type="submit">Salvar</button><button type="button" (click)="categoria = null">Cancelar</button></div></form> }
      <div class="cfg-tabela"><table><thead><tr><th>Categoria</th><th>Grupo</th><th>Tipo</th><th>Ações</th></tr></thead><tbody>@for (item of categoriasVisiveis; track item.id) { <tr><td>{{ item.nome }}</td><td>{{ item.grupo }}</td><td>{{ item.direcao === 'ambas' ? 'Ambas' : item.direcao === 'entrada' ? 'Receita' : 'Despesa' }}</td><td><div class="cfg-linha"><button class="cfg-texto" (click)="editarCategoria(item)">editar</button><button class="cfg-texto" (click)="alternarCategoria(item)">{{ item.ativo ? 'desativar' : 'reativar' }}</button></div></td></tr> } @empty { <tr><td colspan="4" class="cfg-vazio">Nenhuma categoria cadastrada.</td></tr> }</tbody></table></div></div> }
    @case ('centros') { <div class="cfg-pilha"><form class="cfg-linha" (ngSubmit)="centro()"><input class="cresce" name="nome" maxlength="150" [(ngModel)]="novoCentro" placeholder="Nome do centro de custo" aria-label="Nome do centro de custo" required /><button class="cfg-primario" type="submit">Novo centro de custo</button></form><div class="cfg-tabela"><table><thead><tr><th>Centro de custo</th><th>Cadastro</th><th>Ações</th></tr></thead><tbody>@for (item of financeiro.centros; track item.id) { <tr><td><input maxlength="150" aria-label="Centro de custo" [(ngModel)]="item['nome']" (blur)="salvarCentro(item)" /></td><td>{{ item.ativo ? 'Ativo' : 'Inativo' }}</td><td><button class="cfg-texto" (click)="alternarCentro(item)">{{ item.ativo ? 'desativar' : 'reativar' }}</button></td></tr> } @empty { <tr><td colspan="3" class="cfg-vazio">Nenhum centro de custo cadastrado.</td></tr> }</tbody></table></div></div> }
    @case ('urh') { <div class="cfg-dupla"><div class="cfg-principal cfg-pilha"><p class="cfg-info">Toda a Tabela OAB/DF é expressa em URH. Sem este valor preenchido, a Calculadora de Honorários não calcula nada.</p><form class="cfg-card cfg-pilha" (ngSubmit)="salvarUrh()"><div class="cfg-grade tres"><label>Valor da URH<input name="urh" type="number" min="0.01" step="0.01" [(ngModel)]="financeiro.urh" required /></label><label>Competência<input name="competencia" type="month" [(ngModel)]="financeiro.competencia" required /></label><div style="align-self:end">1 URH = {{ financeiro.urh | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}</div></div><div class="cfg-linha cfg-separador"><p class="cresce">Consulte o valor vigente em oabdf.org.br.</p><button type="submit" class="cfg-primario">Salvar URH do mês</button></div></form>
      <section class="cfg-card cfg-pilha"><div><h3>Custos operacionais</h3><p>Liste os custos mensais do escritório (aluguel, salários, softwares, contador etc.) para calcular o custo por hora produtiva, usado como referência na Calculadora de Honorários.</p></div>
        @for (custo of financeiro.custos; track custo.id) { <div class="cfg-linha"><span class="cresce">{{ custo.nome }}</span><span>{{ custo.valor | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}</span><button class="cfg-texto" (click)="removerCusto(custo.id)" aria-label="Excluir custo">×</button></div> } @empty { <p>Nenhum custo cadastrado ainda.</p> }
        <form class="cfg-grade tres" (ngSubmit)="custo()"><input name="nome" maxlength="150" [(ngModel)]="novoCusto" placeholder="Ex.: Aluguel" aria-label="Nome do custo" required /><input name="valor" [(ngModel)]="valorCusto" type="number" min="0.01" step="0.01" placeholder="0,00" aria-label="Valor do custo" required /><button type="submit" class="cfg-primario">Adicionar</button></form><div class="cfg-linha cfg-separador"><p class="cresce">Se o seu pró-labore não estiver nesta lista, o “lucro” da margem é que vira o seu salário — e aí não sobra lucro nenhum.</p><strong>Custo fixo total: {{ custoTotal | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}</strong></div>
      </section><app-capacidade-financeira [financeiro]="financeiro" [total]="custoTotal" (salvar)="salvarCapacidade()" /></div><aside class="cfg-lateral prec cfg-pilha"><section class="cfg-card cfg-pilha"><h3>O que a calculadora vai usar</h3><div class="cfg-linha"><p class="cresce">URH vigente</p><strong>{{ financeiro.urh | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}</strong></div><div class="cfg-linha"><p class="cresce">Custo fixo / mês</p><strong>{{ custoTotal | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}</strong></div><div class="cfg-linha"><p class="cresce">Horas produtivas</p><strong>{{ horas }} h</strong></div><div class="cfg-linha"><p class="cresce">Hora técnica mínima</p><strong>{{ horaTecnica | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}</strong></div><div class="cfg-linha"><p class="cresce">Hora recomendada</p><strong>{{ horaRecomendada | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}</strong></div><div class="cfg-linha cfg-separador"><h3 class="cresce">Hora de mercado ({{ financeiro.fatorPosicionamento }}×)</h3><strong>{{ horaMercado | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}</strong></div><button disabled>Ir para a calculadora →</button><p>O cálculo completo estará disponível no módulo Calculadoras.</p></section><section class="cfg-card"><p>Uma hora técnica de {{ horaTecnica | currency:'BRL':'symbol':'1.2-2':'pt-BR' }} vira uma hora de mercado de {{ horaMercado | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}. É esse salto que separa cobrir o custo de construir escritório.</p></section></aside></div> }
  }
</fieldset>` , styles: [`.financeiro-campos { border:0; padding:0; margin:16px 0 0; min-width:0; }`] })
export class FinanceiroConfig {
  protected readonly store = inject(ConfiguracoesLocalService);
  private readonly api = inject(FinanceiroApiService);
  private readonly destroyRef = inject(DestroyRef);
  protected financeiro = configuracoesIniciais().financeiro;
  protected readonly dados = signal<FinanceiroDados | null>(null);
  protected readonly pronto = signal(false); protected readonly carregando = signal(false);
  protected readonly ocupado = signal(false); protected readonly aviso = signal(''); protected readonly erro = signal(false);
  private versao = 0;
  protected get podeEditar() { return this.pronto() && this.dados()?.papel === 'MASTER'; }
  protected aba = 'contas'; protected todas = false; protected busca = '';
  protected conta: ContaLocal | null = null; protected categoria: CategoriaLocal | null = null;
  protected novoCentro = ''; protected novoCusto = ''; protected valorCusto = 0;
  protected readonly abas = [{ id: 'contas', nome: 'Contas e cartões' }, { id: 'categorias', nome: 'Categorias' }, { id: 'centros', nome: 'Centros de custos' }, { id: 'urh', nome: 'URH e Custos' }];
  protected readonly grupos = ['0 - Deduções da receita bruta', '1 - Receitas', '2 - Folha de pagamento', '3 - Encargos sociais', '4 - Tributos', '5 - Despesas fixas', '6 - Despesas variáveis', '7 - Despesas bancárias', '8 - Marketing e comercial', '9 - Distribuição de lucros e participação'];
  constructor() { this.store.aviso.set(''); this.carregar(); }
  protected carregar() {
    if (this.carregando() || this.ocupado()) return;
    this.pronto.set(false); this.carregando.set(true); this.aviso.set('');
    this.api.carregar().pipe(takeUntilDestroyed(this.destroyRef),finalize(()=>this.carregando.set(false))).subscribe({
      next: d => { this.dados.set(d); this.versao=d.versao; this.financeiro=structuredClone(financeiroRecebido(d)); this.conta=null; this.categoria=null; this.pronto.set(true); this.erro.set(false); },
      error: e => this.falha(e),
    });
  }
  private falha(e: { error?: { detail?:string; erros?:string[] } }) { this.erro.set(true); this.aviso.set(e.error?.erros?.join(' · ') || e.error?.detail || 'Não foi possível salvar ou carregar o financeiro. Suas alterações permanecem na tela.'); }
  private executar(area:'contas'|'categorias'|'centros'|'custos'|'urh'|'capacidade', request:Observable<FinanceiroDados>, concluir=()=>{}) {
    if (!this.podeEditar || this.ocupado()) return;
    this.ocupado.set(true); this.aviso.set('');
    request.pipe(takeUntilDestroyed(this.destroyRef),finalize(()=>this.ocupado.set(false))).subscribe({ next: d => {
      this.versao=d.versao; this.dados.set(d);
      if (area==='capacidade') Object.assign(this.financeiro,structuredClone(d.capacidade));
      else if (area==='urh') { this.financeiro.urh=d.urh; this.financeiro.competencia=d.competencia; }
      else Object.assign(this.financeiro, { [area]: structuredClone(d[area]) });
      concluir(); this.erro.set(false); this.aviso.set('Configuração financeira salva no banco para toda a empresa.');
    }, error:e=>this.falha(e) });
  }
  protected get contasVisiveis() { return this.financeiro.contas.filter(c => this.todas || c.ativo); }
  protected get categoriasVisiveis() { return this.financeiro.categorias.filter(c => (this.todas || c.ativo) && c.nome.toLowerCase().includes(this.busca.toLowerCase())); }
  protected get custoTotal() { return this.financeiro.custos.reduce((s,c)=>s+Number(c.valor),0); }
  protected get horas() { const f=this.financeiro; return f.modoHoras==='direto' ? Number(f.horas || 0) : Math.round(f.advogados*f.horasSemanais*f.percentualProdutivo/100*f.semanasPorMes*100)/100; }
  protected get horaTecnica() { return this.horas ? this.custoTotal/this.horas : 0; }
  protected get horaRecomendada() { return this.horaTecnica*(1+this.financeiro.margemLucro/100); }
  protected get horaMercado() { return this.horaRecomendada*this.financeiro.fatorPosicionamento; }
  protected novaConta() { this.conta={id:this.store.id(),nome:'',banco:'',tipo:'conta',saldo:0,padrao:false,ativo:true}; }
  protected editarConta(c:ContaLocal) { this.conta={...c}; }
  protected salvarConta() {
    if (!this.conta?.nome.trim()) return;
    const c=structuredClone(this.conta);
    this.executar('contas',this.api.conta(c,this.versao,!this.financeiro.contas.some(i=>i.id===c.id)),()=>this.conta=null);
  }
  protected padrao(id:string) { const c=this.financeiro.contas.find(c=>c.id===id); if(c?.ativo) this.executar('contas',this.api.conta({...c,padrao:true},this.versao)); }
  protected alternarConta(c:ContaLocal) { this.executar('contas',this.api.conta({...c,ativo:!c.ativo,padrao:c.ativo ? false : c.padrao},this.versao)); }
  protected novaCategoria() { this.categoria={id:this.store.id(),nome:'',grupo:this.grupos[6],direcao:'saida',ativo:true}; }
  protected editarCategoria(c:CategoriaLocal) { this.categoria={...c}; }
  protected salvarCategoria() {
    if (!this.categoria?.nome.trim()) return;
    const c=structuredClone(this.categoria);
    this.executar('categorias',this.api.categoria(c,this.versao,!this.financeiro.categorias.some(i=>i.id===c.id)),()=>this.categoria=null);
  }
  protected alternarCategoria(c:CategoriaLocal) { this.executar('categorias',this.api.categoria({...c,ativo:!c.ativo},this.versao)); }
  protected centro() {
    if (!this.novoCentro.trim()) return;
    this.executar('centros',this.api.centro({id:'',nome:this.novoCentro.trim(),ativo:true},this.versao,true),()=>this.novoCentro='');
  }
  protected salvarCentro(c:RegistroLocal) { if (this.dados()?.centros.find(i=>i.id===c.id)?.['nome']===c['nome']) return; this.executar('centros',this.api.centro(structuredClone(c),this.versao)); }
  protected alternarCentro(c:RegistroLocal) { this.executar('centros',this.api.centro({...c,ativo:!c.ativo},this.versao)); }
  protected custo() {
    if (!this.novoCusto.trim() || !(this.valorCusto>0)) return;
    this.executar('custos',this.api.custo(this.novoCusto.trim(),Number(this.valorCusto),this.versao),()=>{this.novoCusto='';this.valorCusto=0;});
  }
  protected removerCusto(id:string) { this.executar('custos',this.api.excluirCusto(id,this.versao)); }
  protected salvarUrh() { if(this.financeiro.urh>0 && this.financeiro.competencia) this.executar('urh',this.api.urh(this.financeiro,this.versao)); }
  protected salvarCapacidade() { this.executar('capacidade',this.api.capacidade(this.financeiro,this.versao)); }
}


