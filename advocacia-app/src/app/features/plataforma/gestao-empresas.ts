import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { GestaoEmpresa, PlataformaService } from '../../core/plataforma/plataforma.service';

@Component({ selector: 'app-gestao-empresas', imports: [FormsModule, DatePipe], templateUrl: './gestao-empresas.html', styleUrl: './gestao-empresas.scss' })
export class GestaoEmpresas {
  private readonly api = inject(PlataformaService);
  protected readonly empresas = signal<GestaoEmpresa[]>([]);
  protected readonly ficha = signal<GestaoEmpresa | null>(null);
  protected readonly carregando = signal(false);
  protected readonly salvando = signal(false);
  protected readonly erro = signal('');
  protected readonly aviso = signal('');
  protected busca = ''; protected filtro = ''; protected pagina = 0;
  protected situacao = ''; protected financeiro = ''; protected inicio = ''; protected motivo = ''; protected confirmado = false;
  protected modulos: Record<string, boolean> = {};
  constructor() { this.carregar(); }
  protected carregar(pagina = 0) {
    if (this.carregando() || this.salvando()) return;
    this.carregando.set(true); this.erro.set('');
    this.api.listarGestaoEmpresas(this.busca.trim(), this.filtro, pagina).pipe(finalize(() => this.carregando.set(false))).subscribe({
      next: lista => { this.empresas.set(lista); this.pagina = pagina; }, error: e => this.falha(e),
    });
  }
  protected abrir(id: string) {
    if (this.carregando() || this.salvando()) return;
    this.carregando.set(true); this.erro.set(''); this.aviso.set(''); this.ficha.set(null);
    this.api.carregarGestaoEmpresa(id).pipe(finalize(() => this.carregando.set(false))).subscribe({ next: e => this.receber(e), error: e => this.falha(e) });
  }
  private receber(e: GestaoEmpresa) {
    this.ficha.set(e); this.situacao = e.situacao; this.financeiro = e.financeiro; this.inicio = e.inicio ?? '';
    this.modulos = Object.fromEntries(e.modulos.map(m => [m.codigo, m.liberado])); this.motivo = ''; this.confirmado = false;
  }
  protected salvar() {
    const empresa = this.ficha();
    if (!empresa || this.salvando() || !this.confirmado || !this.motivo.trim()) return;
    this.salvando.set(true); this.erro.set(''); this.aviso.set('');
    this.api.alterarContrato(empresa.id, { versao: empresa.versao, situacao: this.situacao, financeiro: this.financeiro, inicio: this.inicio || null,
      modulos: Object.keys(this.modulos).filter(c => this.modulos[c]), motivo: this.motivo.trim() }).pipe(finalize(() => this.salvando.set(false))).subscribe({
      next: e => { this.receber(e); this.empresas.update(lista => lista.map(item => item.id === e.id ? e : item)); this.aviso.set('Contrato atualizado e medida registrada no histórico.'); },
      error: e => this.falha(e),
    });
  }
  protected nomeSituacao(valor: string) { return ({ ATIVA: 'Ativa', SUSPENSA: 'Suspensa', ENCERRADA: 'Contrato encerrado', NAO_INFORMADO: 'Não informado', REGULAR: 'Regular', INADIMPLENTE: 'Inadimplente' } as Record<string,string>)[valor] ?? valor; }
  protected nomeAcao(valor: string) { return ({ CONTRATO_ATUALIZADO: 'Contrato atualizado', EMPRESA_REATIVADA: 'Empresa reativada', EMPRESA_SUSPENSA: 'Empresa suspensa', CONTRATO_ENCERRADO: 'Contrato encerrado', FINANCEIRO_ATUALIZADO: 'Situação financeira alterada', INICIO_ATUALIZADO: 'Início do contrato alterado', MODULO_ALTERADO: 'Módulo alterado' } as Record<string,string>)[valor] ?? valor; }
  private falha(e: { error?: { detail?: string } }) { this.erro.set(e.error?.detail || 'Não foi possível concluir a operação. Tente novamente.'); }
}
