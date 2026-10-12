import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { ConviteInicial, PlataformaService } from '../../core/plataforma/plataforma.service';
import { GestaoEmpresas } from './gestao-empresas';

@Component({
  selector: 'app-admin-painel',
  imports: [RouterLink, FormsModule, DatePipe, GestaoEmpresas],
  templateUrl: './admin-painel.html',
  styleUrl: './admin-painel.scss',
})
export class AdminPainel {
  protected readonly aba = signal<'convites' | 'empresas'>('convites');
  protected readonly auth = inject(AuthService);
  private readonly plataforma = inject(PlataformaService);
  private readonly router = inject(Router);
  protected readonly convites = signal<ConviteInicial[]>([]);
  protected readonly carregando = signal(false);
  protected readonly enviando = signal(false);
  protected readonly operando = signal<string | null>(null);
  protected readonly saindo = signal(false);
  protected readonly erro = signal('');
  protected readonly aviso = signal('');
  protected nome = '';
  protected email = '';
  constructor() { this.carregar(); }
  protected carregar() {
    this.carregando.set(true);
    this.plataforma.listarConvites().pipe(finalize(() => this.carregando.set(false))).subscribe({
      next: convites => this.convites.set(convites),
      error: erro => this.falha(erro, 'Não foi possível carregar os convites.'),
    });
  }
  protected enviar() {
    if (this.enviando()) return;
    this.erro.set(''); this.aviso.set(''); this.enviando.set(true);
    this.plataforma.convidarResponsavel(this.nome.trim(), this.email.trim()).pipe(finalize(() => this.enviando.set(false))).subscribe({
      next: convite => { this.receber(convite); this.nome = ''; this.email = ''; this.aviso.set('Convite enviado. O responsável poderá completar o cadastro e cadastrar seu escritório.'); },
      error: erro => this.falha(erro, 'Não foi possível enviar o convite.'),
    });
  }
  protected editavel(convite: ConviteInicial) { return ['PENDENTE', 'ACEITO', 'EXPIRADO'].includes(convite.status); }
  protected reenviar(convite: ConviteInicial) {
    if (this.operando()) return;
    this.erro.set(''); this.aviso.set(''); this.operando.set(convite.id);
    this.plataforma.reenviarConvite(convite.id).pipe(finalize(() => this.operando.set(null))).subscribe({
      next: atualizado => { this.receber(atualizado); this.aviso.set('Convite reenviado. Validade renovada por sete dias.'); },
      error: erro => this.falha(erro, 'Não foi possível reenviar o convite.'),
    });
  }
  protected cancelar(convite: ConviteInicial) {
    if (this.operando()) return;
    this.erro.set(''); this.aviso.set(''); this.operando.set(convite.id);
    this.plataforma.cancelarConvite(convite.id).pipe(finalize(() => this.operando.set(null))).subscribe({
      next: () => { this.receber({ ...convite, status: 'CANCELADO' }); this.aviso.set('Convite cancelado. Ele não permite cadastrar um escritório.'); },
      error: erro => this.falha(erro, 'Não foi possível cancelar o convite.'),
    });
  }
  protected situacao(status: string) {
    return ({ PENDENTE: 'Aguardando primeiro acesso', ACEITO: 'Aguardando cadastro do escritório', CONCLUIDO: 'Escritório cadastrado', EXPIRADO: 'Expirado', CANCELADO: 'Cancelado' } as Record<string, string>)[status] || status;
  }
  private receber(convite: ConviteInicial) { this.convites.update(lista => [convite, ...lista.filter(item => item.id !== convite.id)]); }
  private falha(erro: { error?: { detail?: string; erros?: string[] } }, padrao: string) { this.erro.set(erro.error?.detail || erro.error?.erros?.join(' · ') || padrao); }
  protected sair() {
    if (this.saindo()) return;
    this.saindo.set(true);
    this.auth.sair().pipe(finalize(() => this.saindo.set(false))).subscribe({
      next: () => { void this.router.navigateByUrl('/login'); },
      error: () => this.erro.set('Não foi possível sair. Tente novamente.'),
    });
  }
}
