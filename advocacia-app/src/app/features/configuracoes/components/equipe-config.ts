import { Component, DestroyRef, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { finalize, Observable } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import { EquipeApiService, Equipe, MembroEquipe, ConfiguracaoMembro, configuracaoMembroInicial } from '../services/equipe-api.service';
import { PermissoesMembro } from './permissoes-membro';
import { MeuPerfilApiService } from '../services/meu-perfil-api.service';
import { Icone } from '../../../shared/components/icone/icone';

@Component({ selector: 'app-equipe-config', imports: [FormsModule, Icone, PermissoesMembro, DatePipe], template: `
  <div class="cfg-pilha"><div class="cfg-titulo" style="margin:0"><app-icone nome="users" /><div><h2>Equipe</h2><p>quem faz parte do escritório</p></div></div>
    <section class="cfg-info"><p style="color:inherit;font-size:14px">O acesso usa e-mail com código ou uma conta Google. O vínculo é criado no primeiro acesso com o e-mail verificado. Inativar ou suspender um membro revoga o acesso e preserva seus registros.</p><button class="cfg-primario" style="margin-top:12px" [disabled]="!pronto() || ocupado()" (click)="adicionando = !adicionando">{{ adicionando ? 'Cancelar' : '+ Adicionar membro' }}</button></section>
    @if (carregando()) { <p role="status">Carregando a equipe…</p> }
    @if (mensagem()) { <p class="cfg-info" [class.cfg-erro]="erro()" [attr.role]="erro() ? 'alert' : 'status'">{{ mensagem() }}</p> }
    @if (!pronto() && !carregando()) { <button type="button" (click)="carregar()">Tentar novamente</button> }
    @if (adicionando) { <form class="cfg-card cfg-pilha" #formConvite="ngForm" (ngSubmit)="convidar()"><div class="cfg-grade"><label>Nome completo *<input name="nome" [(ngModel)]="nome" maxlength="150" required [disabled]="ocupado()" /></label><label>E-mail *<input name="email" type="email" email [(ngModel)]="email" maxlength="254" required [disabled]="ocupado()" /></label></div>
      <app-permissoes-membro [dados]="novo" [convite]="true" [disabled]="ocupado()" />
      <div><button class="cfg-primario" type="submit" [disabled]="ocupado() || formConvite.invalid">{{ ocupado() ? 'Enviando…' : 'Enviar convite' }}</button></div><p>O convite será enviado por e-mail e terá validade de 7 dias.</p>
    </form> }
    @if (equipe().convites.length) { <section class="cfg-card"><h3>Convites aguardando o primeiro acesso ({{ equipe().convites.length }})</h3>@for (convite of equipe().convites; track convite.id) { <div class="cfg-linha cfg-separador"><div class="cresce"><h4>{{ convite.nome }}</h4><p>{{ convite.email }} · {{ rotulosPerfil[convite.perfil] }}</p><p>Validade: {{ convite.expiraEm | date:'dd/MM/yyyy HH:mm' }}</p></div><span class="cfg-badge">{{ convite.status === 'EXPIRADO' ? 'Expirado' : 'Pendente' }}</span><button [disabled]="ocupado()" (click)="reenviar(convite.id)">Reenviar</button><button [disabled]="ocupado()" (click)="cancelar(convite.id)">Cancelar</button></div> }</section> }
    <div class="cfg-grade cartoes">@for (membro of equipe().membros; track membro.id) { <section class="cfg-card cfg-pilha"><div class="cfg-linha"><div class="cfg-avatar">@if (membro.id === auth.usuario()?.id && meuPerfil.perfil()?.foto) { <img [src]="meuPerfil.perfil()!.foto" alt="Sua foto" /> } @else { {{ iniciais(membro.nome) }} }</div><div class="cresce"><h3>{{ membro.configuracao.nomeExibicao || membro.nome }}</h3><p>{{ membro.email }}</p><span class="cfg-badge">{{ membro.papel === 'MASTER' ? 'Master' : rotulosSituacao[membro.configuracao.situacao] }}</span>@if (membro.id === auth.usuario()?.id) { <span class="cfg-badge">você</span> }</div></div>
      @if (membro.papel === 'MASTER') { <div class="cfg-separador"><p>O master possui acesso completo. Seus dados pessoais são editados em Meu perfil.</p></div> }
      @else { <div class="cfg-separador"><app-permissoes-membro [dados]="rascunhos[membro.id]" [disabled]="ocupado()" /></div><div><button class="cfg-primario" type="button" [disabled]="ocupado()" (click)="salvar(membro)">Salvar membro</button></div> }
    </section> }</div>
  </div>
` })
export class EquipeConfig {
  protected readonly auth = inject(AuthService);
  protected readonly meuPerfil = inject(MeuPerfilApiService);
  private readonly api = inject(EquipeApiService);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly equipe = signal<Equipe>({ membros: [], convites: [] });
  protected readonly carregando = signal(false);
  protected readonly pronto = signal(false);
  protected readonly ocupado = signal(false);
  protected readonly mensagem = signal('');
  protected readonly erro = signal(false);
  protected readonly rotulosPerfil = { ADMINISTRADOR: 'Administrador', ADVOGADO: 'Advogado', ASSISTENTE: 'Assistente', FINANCEIRO: 'Financeiro' };
  protected readonly rotulosSituacao = { ATIVO: 'Ativo', INATIVO: 'Inativo', SUSPENSO: 'Suspenso' };
  protected rascunhos: Record<string, ConfiguracaoMembro> = {};
  protected adicionando = false;
  protected nome = ''; protected email = ''; protected novo = configuracaoMembroInicial();
  constructor() { this.carregar(); }
  protected carregar() {
    this.carregando.set(true); this.mensagem.set('');
    this.api.carregar().pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.carregando.set(false))).subscribe({
      next: equipe => { this.equipe.set(equipe); this.rascunhos = Object.fromEntries(equipe.membros.map(m => [m.id, structuredClone(m.configuracao)])); this.pronto.set(true); },
      error: erro => this.falha(erro, 'Não foi possível carregar a equipe.'),
    });
  }
  protected iniciais(nome: string) { return nome.split(/\s+/).filter(Boolean).slice(0, 2).map(p => p[0]).join('').toUpperCase(); }
  protected convidar() {
    if (!this.nome.trim() || !this.email.trim() || !this.pronto()) return;
    this.executar(this.api.convidar(this.nome.trim(), this.email.trim().toLowerCase(), this.novo), convite => {
      this.equipe.update(equipe => ({ ...equipe, convites: [...equipe.convites, convite] }));
      this.nome = ''; this.email = ''; this.novo = configuracaoMembroInicial(); this.adicionando = false;
    }, 'Convite enviado com sucesso.');
  }
  protected salvar(membro: MembroEquipe) {
    const configuracao = structuredClone(this.rascunhos[membro.id]);
    configuracao.admissao ||= null; configuracao.nascimento ||= null;
    this.executar(this.api.salvar({ ...membro, configuracao }), salvo => {
      this.equipe.update(equipe => ({ ...equipe, membros: equipe.membros.map(m => m.id === salvo.id ? salvo : m) }));
      this.rascunhos[salvo.id] = structuredClone(salvo.configuracao);
    }, 'Dados e permissões do membro salvos.');
  }
  protected cancelar(id: string) { this.executar(this.api.cancelar(id), () => this.equipe.update(equipe => ({ ...equipe, convites: equipe.convites.filter(c => c.id !== id) })), 'Convite cancelado.'); }
  protected reenviar(id: string) { this.executar(this.api.reenviar(id), convite => this.equipe.update(equipe => ({ ...equipe, convites: equipe.convites.map(c => c.id === id ? convite : c) })), 'Convite reenviado.'); }
  private executar<T>(request: Observable<T>, concluir: (valor: T) => void, mensagem: string) {
    if (this.ocupado()) return;
    this.ocupado.set(true); this.mensagem.set('');
    request.pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.ocupado.set(false))).subscribe({
      next: resultado => { concluir(resultado); this.erro.set(false); this.mensagem.set(mensagem); },
      error: erro => this.falha(erro, 'Não foi possível salvar. Tente novamente.'),
    });
  }
  private falha(erro: { error?: { detail?: string; erros?: string[] } }, padrao: string) { this.erro.set(true); this.mensagem.set(erro.error?.erros?.join(' · ') || erro.error?.detail || padrao); }
}
