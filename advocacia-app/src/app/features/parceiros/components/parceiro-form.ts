import { Component, DestroyRef, effect, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FotoService } from '../../../shared/services/foto.service';
import { ContatosApiService } from '../../cliente/services/contatos-api.service';
import { iniciaisContato } from '../../cliente/models/contato.model';
import { ParceirosApiService } from '../services/parceiros-api.service';
import {
  AREAS_PARCEIRO,
  ESTADOS,
  Parceiro,
  ParceiroFormulario,
  parceiroVazio,
} from '../models/parceiro.model';

@Component({
  selector: 'app-parceiro-form',
  imports: [FormsModule],
  templateUrl: './parceiro-form.html',
  styles: ':host{display:block;min-width:0}',
})
export class ParceiroForm {
  readonly parceiro = input<Parceiro | null>(null);
  readonly salvo = output<Parceiro>();
  readonly cancelar = output<void>();
  readonly ocupado = output<boolean>();
  private readonly api = inject(ParceirosApiService);
  private readonly cepApi = inject(ContatosApiService);
  private readonly fotos = inject(FotoService);
  private readonly destroyRef = inject(DestroyRef);
  protected form: ParceiroFormulario = parceiroVazio();
  protected readonly salvando = signal(false);
  protected readonly preparandoFoto = signal(false);
  protected readonly buscandoCep = signal(false);
  protected readonly erro = signal('');
  protected readonly erroCep = signal('');
  protected readonly iniciais = iniciaisContato;
  protected readonly areas = AREAS_PARCEIRO;
  protected readonly estados = ESTADOS;
  protected readonly enderecoCampos = [
    { k: 'numero', r: 'Número' },
    { k: 'complemento', r: 'Complemento' },
    { k: 'bairro', r: 'Bairro' },
  ];
  private cepSequencia = 0;
  private fotoSequencia = 0;
  constructor() {
    effect(() => {
      const p = this.parceiro();
      this.form = p
        ? {
            versao: p.versao,
            dados: { ...p.dados },
            socios: p.socios.map((s) => ({ ...s })),
            areasAtuacao: [...p.areasAtuacao],
          }
        : parceiroVazio();
    });
    this.destroyRef.onDestroy(() => {
      this.fotoSequencia++;
    });
  }
  protected pj() {
    return this.form.dados['tipo_pessoa'] === 'PJ';
  }
  protected documento(valor: string) {
    const n = valor.replace(/\D/g, '').slice(0, this.pj() ? 14 : 11);
    this.form.dados['documento'] = this.pj()
      ? n
          .replace(/^(\d{2})(\d)/, '$1.$2')
          .replace(/^(\d{2})\.(\d{3})(\d)/, '$1.$2.$3')
          .replace(/\.(\d{3})(\d)/, '.$1/$2')
          .replace(/(\d{4})(\d)/, '$1-$2')
      : n
          .replace(/^(\d{3})(\d)/, '$1.$2')
          .replace(/\.(\d{3})(\d)/, '.$1.$2')
          .replace(/(\d{3})(\d)/, '$1-$2');
  }
  protected telefone(valor: string) {
    const n = valor.replace(/\D/g, '').slice(0, 11);
    this.form.dados['telefone'] = n
      .replace(/^(\d{2})(\d)/, '($1) $2')
      .replace(n.length > 10 ? /(\d{5})(\d)/ : /(\d{4})(\d)/, '$1-$2');
  }
  protected cep(valor: string) {
    this.cepSequencia++;
    this.buscandoCep.set(false);
    this.form.dados['cep'] = valor
      .replace(/\D/g, '')
      .slice(0, 8)
      .replace(/(\d{5})(\d)/, '$1-$2');
  }
  protected buscarCep() {
    const cep = this.form.dados['cep'].replace(/\D/g, '');
    if (cep.length !== 8) return;
    const seq = ++this.cepSequencia;
    const anteriores = { ...this.form.dados };
    this.buscandoCep.set(true);
    this.erroCep.set('');
    this.cepApi
      .cep(cep)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (r) => {
          if (seq !== this.cepSequencia) return;
          this.buscandoCep.set(false);
          if (r.erro) {
            this.erroCep.set('CEP não encontrado. Preencha o endereço manualmente.');
            return;
          }
          for (const [campo, valor] of Object.entries({
            logradouro: r.logradouro,
            bairro: r.bairro,
            cidade: r.localidade,
            uf: r.uf,
          }))
            if (valor && this.form.dados[campo] === anteriores[campo])
              this.form.dados[campo] = valor;
        },
        error: () => {
          if (seq === this.cepSequencia) {
            this.buscandoCep.set(false);
            this.erroCep.set('Não foi possível consultar o CEP. Preencha o endereço manualmente.');
          }
        },
      });
  }
  protected alternarArea(area: string) {
    this.form.areasAtuacao = this.form.areasAtuacao.includes(area)
      ? this.form.areasAtuacao.filter((a) => a !== area)
      : [...this.form.areasAtuacao, area];
  }
  protected adicionarSocio() {
    if (this.form.socios.length < 20) this.form.socios.push({ nome: '', oab: '' });
  }
  protected removerSocio(i: number) {
    this.form.socios.splice(i, 1);
  }
  protected async foto(event: Event) {
    const input = event.target as HTMLInputElement;
    const arquivo = input.files?.[0];
    input.value = '';
    if (!arquivo) return;
    const seq = ++this.fotoSequencia;
    this.preparandoFoto.set(true);
    this.ocupado.emit(true);
    this.erro.set('');
    try {
      const foto = await this.fotos.reduzir(arquivo);
      if (seq === this.fotoSequencia) this.form.dados['foto_propria'] = foto;
    } catch (e) {
      if (seq === this.fotoSequencia)
        this.erro.set(e instanceof Error ? e.message : 'Não foi possível preparar a foto.');
    } finally {
      if (seq === this.fotoSequencia) {
        this.preparandoFoto.set(false);
        this.ocupado.emit(false);
      }
    }
  }
  protected salvar() {
    if (this.salvando() || this.preparandoFoto() || !this.form.dados['nome'].trim()) return;
    this.salvando.set(true);
    this.ocupado.emit(true);
    this.erro.set('');
    const payload = {
      ...this.form,
      dados: { ...this.form.dados },
      socios: this.pj() ? this.form.socios.map((s) => ({ ...s })) : [],
      areasAtuacao: [...this.form.areasAtuacao],
    };
    this.api
      .salvar(payload, this.parceiro()?.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (p) => {
          this.salvando.set(false);
          this.ocupado.emit(false);
          this.salvo.emit(p);
        },
        error: (e) => {
          this.salvando.set(false);
          this.ocupado.emit(false);
          this.erro.set(e.error?.detail || 'Não foi possível salvar o parceiro. Tente novamente.');
        },
      });
  }
}
