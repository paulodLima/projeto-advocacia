import { Component, DestroyRef, effect, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import { MascaraDirective } from '../../../shared/directives/mascara.directive';
import {
  Caso,
  FichaCaso,
  FormularioCaso,
  OpcoesCasos,
  dinheiroCampo,
  valorMonetario,
} from '../models/caso.model';
import { CasosApiService } from '../services/casos-api.service';
@Component({
  selector: 'app-caso-form',
  imports: [FormsModule, MascaraDirective],
  templateUrl: './caso-form.html',
})
export class CasoForm {
  readonly caso = input<Caso | null>(null);
  readonly opcoes = input.required<OpcoesCasos>();
  readonly salvo = output<FichaCaso>();
  readonly cancelar = output<void>();
  readonly ocupadoChange = output<boolean>();
  private readonly api = inject(CasosApiService);
  private readonly destroy = inject(DestroyRef);
  protected readonly ocupado = signal(false);
  protected readonly erro = signal('');
  protected novoCliente = false;
  protected cliente = { nome: '', documento: '', telefone: '' };
  protected proposta = '';
  protected form: FormularioCaso = {
    versao: null,
    titulo: '',
    clienteId: null,
    grupoId: null,
    responsavelId: null,
    partesAdversas: '',
    observacoes: '',
    valorProposta: null,
    propostaEm: null,
    novoCliente: null,
  };
  protected readonly grupoAtual = (g: { id: string }) => g.id === this.caso()?.grupoId;
  private tituloAutomatico = '';
  constructor() {
    effect(() => {
      const c = this.caso();
      this.form = c
        ? {
            versao: c.versao,
            titulo: c.titulo,
            clienteId: c.clienteId,
            grupoId: c.grupoId,
            responsavelId: c.responsavelId,
            partesAdversas: c.partesAdversas,
            observacoes: c.observacoes,
            valorProposta: c.valorProposta,
            propostaEm: c.propostaEm,
            novoCliente: null,
          }
        : {
            versao: null,
            titulo: '',
            clienteId: null,
            grupoId: null,
            responsavelId: null,
            partesAdversas: '',
            observacoes: '',
            valorProposta: null,
            propostaEm: null,
            novoCliente: null,
          };
      this.proposta = dinheiroCampo(c?.valorProposta ?? null);
      this.novoCliente = false;
      this.tituloAutomatico = '';
    });
  }
  protected sugerirTitulo() {
    const nome = this.novoCliente
      ? this.cliente.nome
      : this.opcoes().clientes.find((c) => c.id === this.form.clienteId)?.nome;
    if (!nome) return;
    const titulo =
      nome + (this.form.partesAdversas.trim() ? ' x ' + this.form.partesAdversas.trim() : '');
    if (!this.form.titulo.trim() || this.form.titulo === this.tituloAutomatico) {
      this.tituloAutomatico = titulo;
      this.form.titulo = titulo.slice(0, 300);
    }
  }
  protected alternarCliente() {
    this.novoCliente = !this.novoCliente;
    this.form.clienteId = null;
    this.sugerirTitulo();
  }
  protected salvar() {
    if (this.ocupado()) return;
    let payload: FormularioCaso;
    try {
      payload = {
        ...this.form,
        clienteId: this.novoCliente ? null : this.form.clienteId,
        valorProposta: valorMonetario(this.proposta),
        propostaEm: this.form.propostaEm || null,
        novoCliente: this.novoCliente ? { ...this.cliente } : null,
      };
    } catch (e) {
      this.erro.set((e as Error).message);
      return;
    }
    this.ocupado.set(true);
    this.ocupadoChange.emit(true);
    this.erro.set('');
    this.api
      .salvar(this.caso()?.id ?? null, payload)
      .pipe(
        takeUntilDestroyed(this.destroy),
        finalize(() => {
          this.ocupado.set(false);
          this.ocupadoChange.emit(false);
        }),
      )
      .subscribe({
        next: (f) => this.salvo.emit(f),
        error: (e) =>
          this.erro.set(
            e.error?.detail ||
              'Não foi possível salvar o caso. Recarregue antes de tentar novamente se houve outra alteração.',
          ),
      });
  }
}
