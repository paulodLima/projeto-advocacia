import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { Empresa, ListaEmpresas, PlataformaService } from '../../core/plataforma/plataforma.service';

@Component({
  selector: 'app-admin-empresas', imports: [FormsModule, RouterLink],
  styleUrls: ['./admin-painel.scss', './admin-empresas.scss'],
  template: `
    <div class="pagina-admin">
      <header><a class="marca" routerLink="/administracao">Gestão Advocacia <span>Administração da plataforma</span></a>
        <a class="voltar" routerLink="/administracao">Voltar ao painel</a></header>
      <main>
        <p class="contexto">ADMINISTRAÇÃO</p><h1>Empresas</h1>
        <p class="introducao">Cadastre empresas e mantenha seus dados atualizados.</p>
        @if (erro()) { <p role="alert">{{ erro() }}</p> }
        @if (mensagem()) { <p class="sucesso" role="status">{{ mensagem() }}</p> }
        @if (aberto()) {
          <article class="ficha">
            <h2>{{ selecionada ? 'Ficha da empresa' : 'Nova empresa' }}</h2>
            <form #form="ngForm" (ngSubmit)="salvar()">
              <fieldset [disabled]="salvando()"><legend>Dados da empresa</legend>
              <label>Razão social <input name="razaoSocial" [(ngModel)]="razaoSocial" required maxlength="150"></label>
              <label>Nome fantasia / nome de exibição <input name="nome" [(ngModel)]="nome" required maxlength="150"></label>
              <div class="campos"><label>CNPJ <input name="cnpj" [(ngModel)]="cnpj" required maxlength="18" placeholder="00.000.000/0000-00" autocapitalize="characters"></label>
              <label>Quantidade de funcionários <input name="quantidadeFuncionarios" [(ngModel)]="quantidadeFuncionarios" type="number" min="0" step="1" required></label></div>
              <p class="ajuda">Quantidade cadastral de funcionários; não limita os usuários da plataforma.</p>
              <label>Código da empresa <input name="identificacao" [(ngModel)]="identificacao" required maxlength="80" pattern="[a-z0-9][a-z0-9-]{1,79}" [disabled]="salvando()" aria-describedby="ajuda-codigo"></label>
              <p id="ajuda-codigo" class="ajuda">Código único com 2 a 80 letras minúsculas, números ou hífens. Exemplo: empresa-a.</p>
              <label>Situação <select name="ativo" [(ngModel)]="ativo" [disabled]="salvando()"><option [ngValue]="true">Ativa</option><option [ngValue]="false">Inativa</option></select></label>
              <p class="ajuda">Uma empresa inativa não pode ser acessada pelos seus usuários.</p>
              </fieldset>
              <fieldset [disabled]="salvando()"><legend>Contato</legend><div class="campos">
              <label>Email da empresa <input name="email" [(ngModel)]="email" type="email" email maxlength="254"></label>
              <label>Telefone <input name="telefone" [(ngModel)]="telefone" type="tel" maxlength="30"></label></div></fieldset>
              <fieldset [disabled]="salvando()"><legend>Endereço</legend>
              <label>Logradouro, número e complemento <input name="endereco" [(ngModel)]="endereco" maxlength="300" placeholder="Rua, número, bairro e complemento"></label>
              <div class="campos"><label>Cidade <input name="cidade" [(ngModel)]="cidade" maxlength="100"></label>
              <label>UF <select name="uf" [(ngModel)]="uf"><option value="">Selecione</option>@for (estado of estados; track estado) { <option [value]="estado">{{ estado }}</option> }</select></label>
              <label>CEP <input name="cep" [(ngModel)]="cep" maxlength="9" pattern="[0-9]{5}-?[0-9]{3}" placeholder="00000-000"></label></div></fieldset>
              <div class="botoes"><button class="primario" type="submit" [disabled]="form.invalid || !nome.trim() || !razaoSocial.trim() || salvando()">{{ salvando() ? 'Salvando…' : 'Salvar empresa' }}</button>
                <button type="button" (click)="aberto.set(false)" [disabled]="salvando()">Cancelar</button></div>
            </form>
          </article>
        }
        <section aria-label="Empresas cadastradas">
          <div class="barra"><h2>Empresas cadastradas <small>({{ lista().total }})</small></h2>
            <button class="primario" type="button" (click)="nova()" [disabled]="salvando() || carregando()">Nova empresa</button></div>
          @if (carregando()) { <p role="status">Carregando empresas…</p> }
          @else {
            @for (empresa of lista().itens; track empresa.id) {
              <article class="linha"><div><h2>{{ empresa.nome }}</h2><p>{{ empresa.razaoSocial || 'Dados cadastrais pendentes' }}</p><p>CNPJ: {{ empresa.cnpj || 'Não informado' }} · Funcionários: {{ empresa.quantidadeFuncionarios ?? 'Não informado' }}</p></div>
                <span>{{ empresa.ativo ? 'Ativa' : 'Inativa' }}</span>
                <button type="button" (click)="editar(empresa.id)" [disabled]="salvando()">Abrir ficha<span class="sr-only"> de {{ empresa.nome }}</span></button></article>
            } @empty { <article><p>Nenhuma empresa cadastrada nesta página.</p></article> }
            <div class="paginacao"><button type="button" (click)="carregar(lista().pagina - 1)" [disabled]="lista().pagina === 0 || salvando()">Anterior</button>
              <span>Página {{ lista().pagina + 1 }} de {{ lista().paginas || 1 }}</span>
              <button type="button" (click)="carregar(lista().pagina + 1)" [disabled]="lista().pagina + 1 >= lista().paginas || salvando()">Próxima</button></div>
          }
        </section>
      </main>
    </div>
  `,
})
export class AdminEmpresas {
  private readonly plataforma = inject(PlataformaService);
  protected readonly lista = signal<ListaEmpresas>({ itens: [], total: 0, pagina: 0, paginas: 0 });
  protected readonly aberto = signal(false);
  protected readonly carregando = signal(false);
  protected readonly salvando = signal(false);
  protected readonly erro = signal('');
  protected readonly mensagem = signal('');
  protected selecionada?: string;
  protected nome = ''; protected identificacao = ''; protected ativo = true;
  protected razaoSocial = ''; protected cnpj = ''; protected quantidadeFuncionarios: number | null = null;
  protected email = ''; protected telefone = ''; protected endereco = ''; protected cidade = ''; protected uf = ''; protected cep = '';
  protected readonly estados = ['AC','AL','AP','AM','BA','CE','DF','ES','GO','MA','MT','MS','MG','PA','PB','PR','PE','PI','RJ','RN','RS','RO','RR','SC','SP','SE','TO'];
  constructor() { this.carregar(); }
  protected carregar(pagina = 0) {
    this.carregando.set(true); this.erro.set('');
    this.plataforma.listarEmpresas(pagina).pipe(finalize(() => this.carregando.set(false))).subscribe({
      next: lista => this.lista.set(lista), error: () => this.erro.set('Não foi possível carregar as empresas. Confira seu acesso e tente novamente.'),
    });
  }
  protected nova() {
    this.selecionada = undefined; this.nome = ''; this.identificacao = ''; this.ativo = true;
    this.razaoSocial = ''; this.cnpj = ''; this.quantidadeFuncionarios = null;
    this.email = ''; this.telefone = ''; this.endereco = ''; this.cidade = ''; this.uf = ''; this.cep = '';
    this.erro.set(''); this.mensagem.set(''); this.aberto.set(true);
  }
  protected editar(id: string) {
    this.erro.set(''); this.mensagem.set(''); this.salvando.set(true);
    this.plataforma.carregarEmpresa(id).pipe(finalize(() => this.salvando.set(false))).subscribe({
      next: e => { this.selecionada = e.id; this.nome = e.nome; this.identificacao = e.identificacao; this.ativo = e.ativo;
        this.razaoSocial = e.razaoSocial || ''; this.cnpj = e.cnpj || ''; this.quantidadeFuncionarios = e.quantidadeFuncionarios;
        this.email = e.email || ''; this.telefone = e.telefone || ''; this.endereco = e.endereco || '';
        this.cidade = e.cidade || ''; this.uf = e.uf || ''; this.cep = e.cep || ''; this.aberto.set(true); },
      error: () => this.erro.set('Não foi possível abrir a ficha da empresa.'),
    });
  }
  protected salvar() {
    if (this.salvando() || !this.nome.trim() || !/^[a-z0-9][a-z0-9-]{1,79}$/.test(this.identificacao)) return;
    if (!this.razaoSocial.trim() || !this.cnpj.trim() || this.quantidadeFuncionarios === null || !Number.isInteger(this.quantidadeFuncionarios) || this.quantidadeFuncionarios < 0) {
      this.erro.set('Informe razão social, CNPJ e uma quantidade inteira de funcionários maior ou igual a zero.'); return;
    }
    this.salvando.set(true); this.erro.set(''); this.mensagem.set('');
    const dados: Omit<Empresa, 'id'> = { nome: this.nome.trim(), identificacao: this.identificacao, ativo: this.ativo,
      razaoSocial: this.razaoSocial.trim(), cnpj: this.cnpj.trim(), quantidadeFuncionarios: this.quantidadeFuncionarios,
      email: this.email.trim(), telefone: this.telefone.trim(), endereco: this.endereco.trim(), cidade: this.cidade.trim(), uf: this.uf, cep: this.cep.trim() };
    this.plataforma.salvarEmpresa(dados, this.selecionada).pipe(finalize(() => this.salvando.set(false))).subscribe({
      next: () => { this.aberto.set(false); this.mensagem.set('Empresa salva com sucesso.'); this.carregar(); },
      error: erro => this.erro.set(erro.status === 409 ? 'CNPJ ou código já cadastrado. Confira se a empresa já existe.' : erro.status === 400 ? (erro.error?.detail || 'Verifique os campos informados.') : 'Não foi possível salvar. Verifique os campos e sua autorização.'),
    });
  }
}
