import { MascaraDirective } from '../../../shared/directives/mascara.directive';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize, Observable } from 'rxjs';
import { MeuPerfil, MeuPerfilApiService } from '../services/meu-perfil-api.service';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../core/auth/auth.service';
import { ConfiguracoesLocalService } from '../services/configuracoes-local.service';
import { Icone } from '../../../shared/components/icone/icone';

@Component({ selector: 'app-perfil-config', imports: [FormsModule, Icone, MascaraDirective], template: `
  <div class="cfg-titulo"><app-icone nome="users" /><div><h2>Meu perfil</h2><p>seus dados e sua foto na intranet</p></div></div>
  <div class="cfg-card cfg-linha" style="padding:12px 16px;margin-bottom:20px">
    <div class="cfg-avatar" style="width:44px;height:44px;font-size:18px">@if (api.perfil()?.foto) { <img [src]="api.perfil()!.foto" alt="Sua foto" /> } @else { {{ iniciais }} }</div>
    <div><h3>{{ auth.usuario()?.nome }}</h3><p>{{ auth.usuario()?.email }}</p></div>
  </div>
  @if (carregando()) { <p role="status">Carregando seu perfil…</p> }
  @if (mensagem()) { <p class="cfg-info" [attr.role]="erro() ? 'alert' : 'status'">{{ mensagem() }}</p> }
  @if (!pronto() && !carregando()) { <button type="button" (click)="carregar()">Tentar novamente</button> }
  <div class="cfg-dupla"><div class="cfg-principal cfg-pilha">
    <section class="cfg-card"><h3>Minha foto</h3><p>Aparece no cabeçalho e ao lado do seu nome. Sem foto, a intranet usa suas iniciais.</p>
      <div class="cfg-linha"><div class="cfg-avatar">@if (api.perfil()?.foto) { <img [src]="api.perfil()!.foto" alt="Sua foto" /> } @else { {{ iniciais }} }</div>
        <div><label class="cfg-upload">{{ api.perfil()?.foto ? 'Trocar foto' : 'Enviar foto' }}<input type="file" accept="image/png,image/jpeg,image/webp" [disabled]="!pronto() || salvando()" (change)="foto($event)" /></label>
          @if (api.perfil()?.foto) { <button class="cfg-texto" style="margin-left:12px" [disabled]="!pronto() || salvando()" (click)="removerFoto()">Remover</button> }<p>Até 2 MB. Reduzida para até 512 pixels antes do envio.</p></div>
      </div>
    </section>
    <form class="cfg-card cfg-pilha" (ngSubmit)="salvar()"><div><h3>Meus dados</h3><p>O que você escrever aqui aparece na sua ficha da equipe. É o mesmo registro — não há cópia a atualizar.</p></div>
      <div class="cfg-grade"><label>Telefone<input name="telefone" appMascara="telefone" [(ngModel)]="perfil.telefone" maxlength="30" [disabled]="!pronto() || salvando()" placeholder="(61) 90000-0000" type="tel" /></label>
        <label>E-mail de contato<input name="email" [(ngModel)]="perfil.emailPessoal" type="email" maxlength="254" [disabled]="!pronto() || salvando()" placeholder="para fora do institucional" /></label>
        <label class="cfg-largo">Endereço<input name="endereco" [(ngModel)]="perfil.endereco" maxlength="500" [disabled]="!pronto() || salvando()" placeholder="Rua, número, complemento — cidade/UF" /></label></div>
      <div class="cfg-linha"><button class="cfg-primario" type="submit" [disabled]="!pronto() || salvando()">{{ salvando() ? 'Salvando…' : 'Salvar meus dados' }}</button></div>
      <p>Seu e-mail de acesso é <strong>{{ auth.usuario()?.email }}</strong> e não muda por aqui: é ele que abre a intranet.</p>
    </form>
  </div><aside class="cfg-lateral"><section class="cfg-card cfg-pilha"><div><h3>Minha ficha</h3><p>Registrado pelo escritório. Para corrigir algo aqui, fale com o administrador.</p></div>
    <div><div class="cfg-linha"><span class="cresce">Cargo</span><span>—</span></div><div class="cfg-linha"><span class="cresce">Vínculo</span><span>—</span></div></div>
    <div class="cfg-separador"><h4>Férias</h4><p>Nenhum período registrado.</p></div>
    <div class="cfg-separador"><h4>Histórico</h4><p>Nenhum registro ainda.</p></div>
  </section></aside></div>
` })
export class PerfilConfig {
  protected readonly auth = inject(AuthService);
  protected readonly store = inject(ConfiguracoesLocalService);
  protected readonly api = inject(MeuPerfilApiService);
  private readonly destroyRef = inject(DestroyRef);
  protected perfil = { telefone: '', emailPessoal: '', endereco: '' };
  protected readonly carregando = signal(false);
  protected readonly pronto = signal(false);
  protected readonly salvando = signal(false);
  protected readonly mensagem = signal('');
  protected readonly erro = signal(false);
  constructor() { this.store.aviso.set(''); this.carregar(); }
  protected get iniciais() { return (this.auth.usuario()?.nome ?? '').split(/\s+/).slice(0, 2).map(p => p[0]).join('').toUpperCase(); }
  protected carregar() {
    this.carregando.set(true); this.mensagem.set('');
    this.api.carregar().pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.carregando.set(false))).subscribe({
      next: perfil => { this.perfil = { telefone: perfil.telefone, emailPessoal: perfil.emailPessoal, endereco: perfil.endereco }; this.pronto.set(true); },
      error: erro => this.falha(erro, 'Não foi possível carregar seu perfil.'),
    });
  }
  protected salvar() {
    if (!this.pronto() || this.salvando()) return;
    this.salvando.set(true);
    this.gravar(this.api.salvar({ ...this.perfil }), 'Dados salvos com sucesso.', true);
  }
  protected async foto(event: Event) {
    if (!this.pronto() || this.salvando()) return;
    this.salvando.set(true); this.mensagem.set(''); this.store.aviso.set('');
    const foto = await this.store.imagem(event, 512, 'image/jpeg');
    if (this.destroyRef.destroyed) return;
    if (!foto) { this.salvando.set(false); this.mensagem.set(this.store.aviso()); this.erro.set(true); this.store.aviso.set(''); return; }
    this.gravar(this.api.salvarFoto(foto), 'Foto salva com sucesso.');
  }
  protected removerFoto() {
    if (!this.pronto() || this.salvando()) return;
    this.salvando.set(true);
    this.gravar(this.api.removerFoto(), 'Foto removida.');
  }
  private gravar(request: Observable<MeuPerfil>, mensagem: string, atualizarDados = false) {
    this.mensagem.set('');
    request.pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.salvando.set(false))).subscribe({
      next: perfil => {
        if (atualizarDados) this.perfil = { telefone: perfil.telefone, emailPessoal: perfil.emailPessoal, endereco: perfil.endereco };
        this.erro.set(false); this.mensagem.set(mensagem);
      },
      error: erro => this.falha(erro, 'Não foi possível salvar. Tente novamente.'),
    });
  }
  private falha(erro: { error?: { detail?: string; erros?: string[] } }, padrao: string) {
    this.erro.set(true); this.mensagem.set(erro.error?.erros?.join(' · ') || erro.error?.detail || padrao);
  }
}
