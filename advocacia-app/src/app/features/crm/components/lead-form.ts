import { MascaraDirective } from '../../../shared/directives/mascara.directive';
import { Component, DestroyRef, effect, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import { Lead, OpcoesCrm, TEMPERATURAS, novoCadastro } from '../models/crm.model';
import { CrmApiService } from '../services/crm-api.service';

@Component({ selector: 'app-lead-form', imports: [FormsModule, MascaraDirective], templateUrl: './lead-form.html' })
export class LeadForm {
  readonly lead = input<Lead | null>(null);
  readonly opcoes = input.required<OpcoesCrm>();
  readonly salvo = output<Lead>();
  readonly cancelar = output<void>();
  readonly ocupadoChange = output<boolean>();
  private readonly api = inject(CrmApiService);
  private readonly destroy = inject(DestroyRef);
  protected cadastro = novoCadastro();
  protected readonly temperaturas = TEMPERATURAS;
  protected readonly ocupado = signal(false);
  protected readonly erro = signal('');
  constructor() {
    effect(() => {
      this.cadastro = this.lead()
        ? { ...this.lead()!.dados.cadastro, etiquetas: [...this.lead()!.dados.cadastro.etiquetas] }
        : novoCadastro();
    });
  }
  protected etiqueta(id: string) {
    this.cadastro.etiquetas = this.cadastro.etiquetas.includes(id)
      ? this.cadastro.etiquetas.filter((t) => t !== id)
      : [...this.cadastro.etiquetas, id];
  }
  protected salvar() {
    if (this.ocupado()) return;
    this.erro.set('');
    this.ocupado.set(true);
    this.ocupadoChange.emit(true);
    this.api
      .salvar(this.cadastro, this.lead())
      .pipe(
        takeUntilDestroyed(this.destroy),
        finalize(() => {
          this.ocupado.set(false);
          this.ocupadoChange.emit(false);
        }),
      )
      .subscribe({
        next: (l) => this.salvo.emit(l),
        error: (e) => this.erro.set(e.error?.detail || 'Não foi possível salvar o lead.'),
      });
  }
}
