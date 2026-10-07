import { Component, DestroyRef, inject, signal } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize, Observable } from 'rxjs';
import { Icone } from '../../../shared/components/icone/icone';
import { CofreApiService, CofreDados, Credencial, CredencialForm } from '../services/cofre-api.service';
@Component({selector:'app-senhas-config',imports:[FormsModule,Icone],template:`
  <div class="cfg-pilha">
    <div class="cfg-linha"><div class="cfg-titulo cresce" style="margin:0"><app-icone nome="lock" /><div><h2>Senhas</h2><p>os acessos compartilhados aos sistemas do escritório</p></div></div>
      @if (podeEditar) { <button class="cfg-primario" [disabled]="ocupado()" (click)="editar() ? fechar() : nova()">{{ editar() ? 'Cancelar' : '+ Nova credencial' }}</button> }
    </div>
    <p class="cfg-info">Credenciais criptografadas no banco. Senhas são exibidas por 30 segundos ao clicar em Mostrar.</p>
    @if (carregando()) { <p role="status">Carregando cofre…</p> }
    @if (mensagem()) { <p class="cfg-info" [class.cfg-erro]="erro()" [attr.role]="erro() ? 'alert' : 'status'">{{ mensagem() }}</p> }
    @if (!pronto() && !carregando()) { <button (click)="carregar()">Tentar novamente</button> }
    @if (pronto() && !dados()?.empresaId) { <p class="cfg-info">Cadastre sua empresa para usar o cofre.</p> }
    @if (pronto() && !dados()?.configurado) { <p class="cfg-info">O administrador precisa configurar a chave de criptografia do cofre.</p> }
    @if (pronto()) { <button [disabled]="ocupado()" (click)="carregar()">Recarregar cofre</button> }
    <input [(ngModel)]="busca" placeholder="Buscar sistema, usuário…" aria-label="Buscar credencial" style="max-width:360px" />
    @if (editar()) { <fieldset class="cofre-campos" [disabled]="!podeEditar || ocupado()"><form class="cfg-card cfg-pilha" (ngSubmit)="salvar()">
      <div class="cfg-grade">
        <label>Nome do sistema *<input name="nome" [(ngModel)]="credencial.nome" maxlength="150" placeholder="Ex.: PJe" required /></label>
        <label>Endereço (URL)<input name="url" [(ngModel)]="credencial.url" maxlength="1000" type="url" placeholder="https://…" /></label>
        <label>Usuário / login<input name="usuario" [(ngModel)]="credencial.usuario" maxlength="254" autocomplete="off" /></label>
        <label>Senha<input name="senha" [type]="revelar ? 'text' : 'password'" [(ngModel)]="senha" maxlength="4096" [required]="!id" autocomplete="new-password" [placeholder]="id ? 'Deixe vazio para manter a senha' : ''" /><button type="button" class="cfg-texto" (click)="revelar = !revelar">{{ revelar ? 'Ocultar' : 'Mostrar' }}</button></label>
        <label class="cfg-largo">Descrição<input name="descricao" [(ngModel)]="credencial.descricao" maxlength="500" /></label>
        <label class="cfg-largo">Observações<textarea name="observacao" [(ngModel)]="credencial.observacao" maxlength="4000"></textarea></label>
      </div>
      <label class="cfg-check"><input name="admin" type="checkbox" [(ngModel)]="credencial.soAdmin" />Visível somente para administradores</label>
      <div class="cfg-linha"><button class="cfg-primario" type="submit">Salvar</button><button type="button" (click)="fechar()">Cancelar</button></div>
    </form></fieldset> }
    @for (item of visiveis; track item.id) { <section class="cfg-card cfg-pilha">
      <div class="cfg-linha"><strong class="cresce">{{ item.nome }}</strong>@if (item.soAdmin) { <span class="cfg-badge">Somente administradores</span> }
        @if (podeEditar) { <button [disabled]="ocupado()" (click)="abrir(item)">Editar</button><button [disabled]="ocupado()" (click)="excluir(item)">Excluir</button> }
      </div>
      @if (item.url) { <a [href]="item.url" target="_blank" rel="noopener noreferrer">{{ item.url }}</a> }
      <p>Usuário: {{ item.usuario || 'não informado' }}</p>
      @if (item.descricao) { <p>{{ item.descricao }}</p> } @if (item.observacao) { <p style="white-space:pre-wrap">{{ item.observacao }}</p> }
      <div class="cfg-linha"><span class="cresce">Senha: {{ segredos()[item.id] || '••••••••' }}</span><button [disabled]="ocupado() || !dados()?.configurado" (click)="mostrar(item)">{{ segredos()[item.id] ? 'Ocultar senha' : 'Mostrar senha' }}</button></div>
    </section> } @empty { @if (pronto() && dados()?.configurado) { <section class="cfg-card cfg-vazio">Nenhuma credencial encontrada.</section> } }
  </div>
`,styles:[`.cofre-campos { border:0;padding:0;margin:0;min-width:0; }`]})
export class SenhasConfig {
  private readonly api=inject(CofreApiService); private readonly destroyRef=inject(DestroyRef); private readonly document=inject(DOCUMENT);
  protected readonly dados=signal<CofreDados|null>(null); protected readonly pronto=signal(false); protected readonly carregando=signal(false);
  protected readonly editar=signal(false); protected readonly ocupado=signal(false); protected readonly mensagem=signal(''); protected readonly erro=signal(false);
  protected readonly segredos=signal<Record<string,string>>({}); private readonly timers=new Map<string,ReturnType<typeof setTimeout>>();
  protected busca=''; protected senha=''; protected revelar=false; protected id=''; private versao:number|null=null;
  protected credencial={nome:'',url:'',usuario:'',descricao:'',observacao:'',soAdmin:false};
  protected get podeEditar() { return this.pronto() && !!this.dados()?.empresaId && !!this.dados()?.podeEditar && !!this.dados()?.configurado; }
  protected get visiveis() { const b=this.busca.toLocaleLowerCase();return (this.dados()?.credenciais || []).filter(c=>(c.nome+' '+c.usuario).toLocaleLowerCase().includes(b)); }
  constructor() {
    const ocultar=()=>{if(this.document.hidden) { this.limpar();this.senha='';this.revelar=false; }};
    this.document.addEventListener('visibilitychange',ocultar);
    this.destroyRef.onDestroy(()=>{this.limpar();this.senha='';this.document.removeEventListener('visibilitychange',ocultar);});this.carregar();
  }
  private limpar() { this.timers.forEach(t=>clearTimeout(t));this.timers.clear();this.segredos.set({}); }
  protected carregar() {
    if(this.ocupado() || this.carregando()) return;
    this.limpar();this.fechar();this.pronto.set(false);this.carregando.set(true);
    this.api.carregar().pipe(takeUntilDestroyed(this.destroyRef),finalize(()=>this.carregando.set(false))).subscribe({next:d=>{this.dados.set(d);this.pronto.set(true);},error:e=>this.falha(e)});
  }
  private falha(e:{error?:{detail?:string}}) {this.erro.set(true);this.mensagem.set(e.error?.detail || 'Não foi possível concluir a operação. Tente novamente.');}
  private executar<T>(req:Observable<T>,receber:(d:T)=>void,msg:string) {
    if(this.ocupado() || !this.pronto() || !this.dados()?.configurado) return;
    this.ocupado.set(true);this.mensagem.set('');
    req.pipe(takeUntilDestroyed(this.destroyRef),finalize(()=>this.ocupado.set(false))).subscribe({next:d=>{receber(d);this.erro.set(false);this.mensagem.set(msg);},error:e=>this.falha(e)});
  }
  protected fechar() {this.editar.set(false);this.senha='';this.revelar=false;this.id='';this.versao=null;this.mensagem.set('');this.erro.set(false);}
  protected nova() {this.fechar();this.credencial={nome:'',url:'',usuario:'',descricao:'',observacao:'',soAdmin:false};this.editar.set(true);}
  protected abrir(c:Credencial) {this.fechar();this.id=c.id;this.versao=c.versao;this.credencial={nome:c.nome,url:c.url,usuario:c.usuario,descricao:c.descricao,observacao:c.observacao,soAdmin:c.soAdmin};this.editar.set(true);}
  protected salvar() {
    if(!this.podeEditar || !this.credencial.nome.trim() || (!this.id&&!this.senha)) return;
    const f:CredencialForm={...this.credencial,versao:this.versao,senha:this.id && !this.senha ? null : this.senha};
    this.executar(this.api.salvar(f,this.id || undefined),c=>{this.dados.update(d=>d ? {...d,credenciais:[...d.credenciais.filter(i=>i.id!==c.id),c]} : d);this.ocultar(c.id);this.fechar();},'Credencial salva no cofre.');
  }
  protected excluir(c:Credencial) {if(!this.podeEditar) return;this.executar(this.api.excluir(c),()=>{this.ocultar(c.id);this.dados.update(d=>d ? {...d,credenciais:d.credenciais.filter(i=>i.id!==c.id)} : d);if(this.id===c.id)this.fechar();},'Credencial excluída.');}
  private ocultar(id:string) {const t=this.timers.get(id);if(t)clearTimeout(t);this.timers.delete(id);this.segredos.update(s=>{const copia={...s};delete copia[id];return copia;});}
  protected mostrar(c:Credencial) {
    if(this.segredos()[c.id]) {this.ocultar(c.id);return;}
    this.executar(this.api.revelar(c.id),r=>{if(this.document.hidden)return;this.segredos.update(s=>({...s,[c.id]:r.senha}));this.timers.set(c.id,setTimeout(()=>this.ocultar(c.id),30000));},'');
  }
}
