import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import { EmpresaApiService, LIMITES_EMPRESA, MinhaEmpresa } from '../services/empresa-api.service';
import { DOCUMENT } from '@angular/common';
import { IdentidadeApiService, IdentidadeVisual } from '../../../core/branding/identidade-api.service';
import { IdentidadeEmpresaService } from '../../../core/branding/identidade-empresa.service';
import { FormsModule } from '@angular/forms';
import { EMPRESA_SECOES } from '../models/referencia';
import { ConfiguracoesLocalService } from '../services/configuracoes-local.service';

@Component({ selector: 'app-empresa-config', imports: [FormsModule], template: `
  <div class="cfg-dupla"><form class="cfg-principal cfg-pilha" #formEmpresa="ngForm" (ngSubmit)="salvar()">
    <div><h2>Empresa</h2><p>Os dados da sociedade. Recibo, fatura e demonstrativo importam tudo daqui — mudou aqui, muda em todos.</p></div>
    @if (carregando()) { <p role="status">Carregando sua empresa…</p> }
    @if (mensagem()) { <p class="cfg-info" [class.cfg-erro]="erro()" [attr.role]="erro() ? 'alert' : 'status'">{{ mensagem() }}</p> }
    @if (!pronto() && !carregando()) { <button type="button" (click)="carregar()">Tentar novamente</button> }
    @if (pronto() && !registro()?.id) { <p class="cfg-info">Sua conta ainda não está vinculada a uma empresa. Ao cadastrar os dados, você será o usuário master desse escritório.</p> }
    @if (pronto() && registro()?.papel === 'MEMBRO') { <p class="cfg-info">Você pode consultar os dados da sua empresa. Para alterá-los, fale com o usuário master.</p> }
    @if (pronto() && faltam.length) { <p class="cfg-info">Falta preencher: {{ faltam.join(', ') }}. Sem isso o timbrado sai incompleto.</p> }
    @for (secao of secoes; track secao.titulo) {
      <section class="cfg-card"><h3>{{ secao.titulo }}</h3>@if (secao.dica) { <p>{{ secao.dica }}</p> }
        <div class="cfg-grade tres" style="margin-top:12px">@for (campo of secao.campos; track campo.k) {
          <label [class.cfg-largo]="campo.larga">{{ campo.r }}<input [name]="campo.k" [attr.name]="campo.k" [type]="campo.k === 'email' ? 'email' : campo.tipo || 'text'" [email]="campo.k === 'email'" [required]="!!campo.obrigatorio" [maxlength]="limites[campo.k]" [disabled]="!podeEditar || salvando()" [(ngModel)]="empresa[campo.k]" /></label>
        }</div>
      </section>
    }
    @if (podeEditar) { <div><button type="submit" class="cfg-primario" [disabled]="salvando() || formEmpresa.invalid">{{ salvando() ? 'Salvando…' : registro()?.id ? 'Salvar dados da empresa' : 'Cadastrar empresa' }}</button></div> }
    <p class="cfg-info">Nenhum documento é gerado sozinho: recibo, fatura e demonstrativo saem só quando você clica, na ficha do processo.</p>
  </form><aside class="cfg-lateral larga cfg-pilha">
    <div><h2>Identidade visual</h2><p>A foto da tela de entrada e as logos do escritório.</p><p class="cfg-info">As imagens e o recorte são salvos para toda a empresa. Somente o master pode alterá-los.</p></div>
    @if (!registro()?.id) { <p class="cfg-info">Cadastre a empresa para salvar sua identidade visual.</p> }
    @if (avisoIdentidade()) { <p class="cfg-info" [class.cfg-erro]="erroIdentidade()" [attr.role]="erroIdentidade() ? 'alert' : 'status'">{{ avisoIdentidade() }}</p> }
    @if (registro()?.id && !identidadePronta()) { <button type="button" (click)="carregarIdentidade()" [disabled]="buscandoIdentidade()">{{ buscandoIdentidade() ? 'Carregando identidade…' : 'Tentar novamente' }}</button> }
    <fieldset class="identidade-campos cfg-pilha" [disabled]="!registro()?.id || !podeEditar || !identidadePronta() || salvandoIdentidade()">
    <section class="cfg-card"><h3>Foto da tela de login</h3><p>Aparece do lado direito da tela de entrada. Envie uma imagem e escolha a parte que aparece.</p>
      @if (identidade.imagens['foto_login']) {
        <div class="foto-login" (pointerdown)="iniciarArraste($event)" (pointermove)="arrastar($event)" (pointerup)="pararArraste()" (pointercancel)="pararArraste()" role="img" aria-label="Recorte da foto de login">
          <img [src]="identidade.imagens['foto_login']" alt="Foto da tela de login" [style.object-position]="identidade.posX + '% ' + identidade.posY + '%'" [style.transform]="'scale(' + identidade.zoom + ')'" />
        </div>
        <label>Zoom<input type="range" min="1" max="2.5" step="0.01" [(ngModel)]="identidade.zoom" aria-label="Zoom da foto de login" /></label>
        <p>Arraste a imagem dentro do quadro para escolher a parte que aparece.</p>
        <label>Posição horizontal<input type="range" min="0" max="100" [(ngModel)]="identidade.posX" /></label><label>Posição vertical<input type="range" min="0" max="100" [(ngModel)]="identidade.posY" /></label>
      }
      <div class="cfg-linha" style="margin-top:12px"><label class="cfg-upload">{{ identidade.imagens['foto_login'] ? 'Trocar foto' : 'Enviar foto' }}<input type="file" accept="image/png,image/jpeg,image/webp" (change)="imagem($event, 'foto_login')" /></label>
        @if (identidade.imagens['foto_login']) { <button class="cfg-texto" (click)="remover('foto_login')">Remover</button><button class="cfg-primario" (click)="salvarIdentidade()">Salvar</button> }</div>
      <p style="margin-top:8px">Sem foto enviada, a tela de login usa a imagem padrão.</p>
    </section>
    <section class="cfg-card"><h3>Logos do escritório</h3><p>Envie cada versão que tiver e marque, na mesma linha, onde ela deve ser usada. Sem nenhuma marcada, o local usa um ícone padrão.</p>
      @for (logo of logos; track logo.id) {
        <div class="cfg-separador"><div class="cfg-linha"><div class="cfg-imagem" [style.background]="logo.fundo">@if (identidade.imagens[logo.id]) { <img [src]="identidade.imagens[logo.id]" [alt]="logo.nome" /> } @else { sem arquivo }</div>
          <div class="cresce"><h3>{{ logo.nome }}</h3><p>{{ logo.dica }}</p><label class="cfg-upload">{{ identidade.imagens[logo.id] ? 'Trocar' : 'Enviar' }}<input type="file" accept="image/png,image/jpeg,image/webp,image/svg+xml" (change)="imagem($event, logo.id)" /></label>
            @if (identidade.imagens[logo.id]) { <button class="cfg-texto" style="margin-left:8px" (click)="remover(logo.id)">Remover</button> }</div></div>
          <div class="cfg-linha" style="margin-top:12px">@for (uso of usos; track uso.id) { <button class="uso" [class.selecionado]="identidade.usos[uso.id] === logo.id" [disabled]="!identidade.imagens[logo.id]" (click)="usar(uso.id, logo.id)">{{ uso.nome }}</button> }</div>
        </div>
      }
    </section>
    <button type="button" class="cfg-primario" (click)="salvarIdentidade()">{{ salvandoIdentidade() ? 'Salvando…' : 'Salvar identidade visual' }}</button>
    </fieldset>
    @if (linkLogin) { <label>Link de entrada do escritório<input readonly [value]="linkLogin" /><small>Compartilhe este link para mostrar sua marca antes do login.</small></label> }
  </aside></div>
`, styles: `.identidade-campos { border:0;padding:0;margin:0;min-width:0; } .foto-login { width:100%; height:230px; overflow:hidden; border-radius:12px; cursor:grab; background:#e5e0d8; touch-action:none; }
 .foto-login img { width:100%;height:100%;object-fit:cover;pointer-events:none; } .config-ui .uso { border-radius:99px;font-size:11px;padding:4px 10px; } .uso.selecionado { background:var(--tema-primaria);color:white; }` })
export class EmpresaConfig {
  protected readonly store = inject(ConfiguracoesLocalService);
  protected readonly api = inject(EmpresaApiService);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly limites = LIMITES_EMPRESA;
  protected readonly pronto = signal(false);
  protected readonly carregando = signal(false);
  protected readonly salvando = signal(false);
  protected readonly mensagem = signal('');
  protected readonly erro = signal(false);
  protected readonly registro = signal<MinhaEmpresa | null>(null);
  protected readonly secoes = EMPRESA_SECOES;
  protected empresa: Record<string, string> = {};
  private readonly identidadeApi = inject(IdentidadeApiService);
  private readonly marca = inject(IdentidadeEmpresaService);
  private readonly document = inject(DOCUMENT);
  protected identidade: IdentidadeVisual = { imagens: {}, usos: {}, zoom: 1, posX: 50, posY: 50 };
  protected readonly identidadePronta = signal(false);
  protected readonly buscandoIdentidade = signal(false);
  protected readonly salvandoIdentidade = signal(false);
  protected readonly avisoIdentidade = signal('');
  protected readonly erroIdentidade = signal(false);
  private identidadeEmpresaId: string | null = null;
  protected get linkLogin() { const id = this.registro()?.id; return id ? (this.document.defaultView?.location.origin || '') + '/login?empresa=' + encodeURIComponent(id) : ''; }
  protected readonly logos = [
    { id: 'logo_completa_branca', nome: 'Completa · branca', dica: 'Desenho + nome.', fundo: '#4f5a49' },
    { id: 'logo_simples_branca', nome: 'Simplificada · branca', dica: 'Só o desenho.', fundo: '#4f5a49' },
    { id: 'logo_completa_marrom', nome: 'Completa · marrom', dica: 'Desenho + nome.', fundo: '#f7f4ef' },
    { id: 'logo_simples_marrom', nome: 'Simplificada · marrom', dica: 'Só o desenho.', fundo: '#f7f4ef' },
  ];
  protected readonly usos = [{ id: 'menu_expandido', nome: 'Menu expandido' }, { id: 'menu_recolhido', nome: 'Menu recolhido' }, { id: 'login', nome: 'Login' }];
  private arraste: { x: number; y: number; posX: number; posY: number } | null = null;
  protected get faltam() { return this.secoes.flatMap(s => s.campos).filter(c => c.obrigatorio && !this.empresa[c.k]?.trim()).map(c => c.r); }
  constructor() { this.store.aviso.set(''); this.carregar(); }
  protected get podeEditar() { return this.pronto() && (!this.registro()?.id || this.registro()?.papel === 'MASTER'); }
  protected carregar() {
    this.carregando.set(true); this.mensagem.set('');
    this.api.carregar().pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.carregando.set(false))).subscribe({
      next: empresa => { this.registro.set(empresa); this.empresa = { ...empresa.dados }; this.pronto.set(true); this.carregarIdentidade(); },
      error: erro => this.falha(erro, 'Não foi possível carregar sua empresa.'),
    });
  }
  protected salvar() {
    if (!this.podeEditar || this.salvando() || this.faltam.length) return;
    this.salvando.set(true); this.mensagem.set('');
    const dados = Object.fromEntries(this.secoes.flatMap(secao => secao.campos.map(campo => [campo.k, this.empresa[campo.k]?.trim() ?? ''])));
    const request = this.registro()?.id ? this.api.salvar(dados) : this.api.criar(dados);
    request.pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.salvando.set(false))).subscribe({
      next: empresa => { this.registro.set(empresa); this.empresa = { ...empresa.dados }; this.erro.set(false); this.mensagem.set('Dados da empresa salvos com sucesso.'); this.carregarIdentidade(); },
      error: erro => this.falha(erro, 'Não foi possível salvar sua empresa. Tente novamente.'),
    });
  }
  private falha(erro: { error?: { detail?: string; erros?: string[] } }, padrao: string) {
    this.erro.set(true); this.mensagem.set(erro.error?.erros?.join(' · ') || erro.error?.detail || padrao);
  }
  protected carregarIdentidade() {
    const id = this.registro()?.id;
    if (!id || this.buscandoIdentidade() || (id === this.identidadeEmpresaId && this.identidadePronta())) return;
    this.identidadeEmpresaId = id; this.buscandoIdentidade.set(true); this.avisoIdentidade.set('');
    this.identidadeApi.carregar().pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.buscandoIdentidade.set(false))).subscribe({
      next: resposta => { this.identidade = structuredClone(resposta.identidade); this.identidadePronta.set(true); this.marca.receber(resposta); },
      error: erro => { this.erroIdentidade.set(true); this.avisoIdentidade.set(erro.error?.detail || 'Não foi possível carregar a identidade visual.'); },
    });
  }
  protected salvarIdentidade() {
    if (!this.podeEditar || !this.registro()?.id || !this.identidadePronta() || this.salvandoIdentidade()) return;
    this.salvandoIdentidade.set(true); this.avisoIdentidade.set('');
    this.identidadeApi.salvar(structuredClone(this.identidade)).pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.salvandoIdentidade.set(false))).subscribe({
      next: resposta => { this.identidade = structuredClone(resposta.identidade); this.marca.receber(resposta); this.erroIdentidade.set(false); this.avisoIdentidade.set('Identidade visual salva para toda a empresa.'); },
      error: erro => { this.erroIdentidade.set(true); this.avisoIdentidade.set(erro.error?.detail || 'Não foi possível salvar. Suas alterações permanecem no formulário.'); },
    });
  }
  protected async imagem(event: Event, id: string) {
    if (!this.podeEditar || !this.identidadePronta() || this.salvandoIdentidade()) return;
    this.avisoIdentidade.set('');
    const imagem = await this.store.imagem(event, 1400, id === 'foto_login' ? 'image/jpeg' : 'image/png');
    if (imagem) { this.identidade.imagens[id] = imagem; this.erroIdentidade.set(false); this.avisoIdentidade.set('Imagem preparada. Clique em Salvar identidade visual para publicar.'); }
    else if (this.store.aviso()) { this.erroIdentidade.set(true); this.avisoIdentidade.set(this.store.aviso()); }
  }
  protected remover(id: string) {
    if (!this.podeEditar || !this.identidadePronta() || this.salvandoIdentidade()) return;
    delete this.identidade.imagens[id];
    for (const uso of Object.keys(this.identidade.usos)) if (this.identidade.usos[uso] === id) delete this.identidade.usos[uso];
  }
  protected usar(uso: string, id: string) {
    if (!this.podeEditar || !this.identidadePronta() || this.salvandoIdentidade()) return;
    if (this.identidade.usos[uso] === id) delete this.identidade.usos[uso]; else this.identidade.usos[uso] = id;
  }
  protected iniciarArraste(event: PointerEvent) { if (!this.podeEditar || this.salvandoIdentidade()) return; (event.currentTarget as HTMLElement).setPointerCapture(event.pointerId); this.arraste = { x: event.clientX, y: event.clientY, posX: this.identidade.posX, posY: this.identidade.posY }; }
  protected arrastar(event: PointerEvent) { if (!this.arraste) return; this.identidade.posX = Math.max(0, Math.min(100, this.arraste.posX - (event.clientX - this.arraste.x) / 2)); this.identidade.posY = Math.max(0, Math.min(100, this.arraste.posY - (event.clientY - this.arraste.y) / 2)); }
  protected pararArraste() { this.arraste = null; }
}

