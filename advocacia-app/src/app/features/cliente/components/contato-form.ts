import { MascaraDirective } from '../../../shared/directives/mascara.directive';
import { Component, DestroyRef, effect, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  Contato,
  ContatoFormulario,
  IndicadorContato,
  OpcoesContato,
  TIPOS_CONTATO,
  contatoVazio,
} from '../models/contato.model';
import { ContatosApiService } from '../services/contatos-api.service';

@Component({
  selector: 'app-contato-form',
  imports: [FormsModule, MascaraDirective],
  templateUrl: './contato-form.html',
  styles: ':host{display:block;min-width:0}',
})
export class ContatoForm {
  readonly contato = input<Contato | null>(null);
  readonly opcoes = input.required<OpcoesContato>();
  readonly salvo = output<Contato>();
  readonly cancelar = output<void>();
  readonly indicadorCriado = output<IndicadorContato>();
  private readonly api = inject(ContatosApiService);
  private readonly destroyRef = inject(DestroyRef);
  protected form: ContatoFormulario = contatoVazio();
  protected readonly tipos = TIPOS_CONTATO;
  protected readonly salvando = signal(false);
  protected readonly erro = signal('');
  protected readonly buscandoCep = signal(false);
  protected readonly erroCep = signal('');
  protected readonly criandoIndicador = signal(false);
  protected novoIndicador = '';
  private cepSequencia = 0;
  protected readonly estados = [
    'Solteiro(a)',
    'Casado(a)',
    'Divorciado(a)',
    'Viúvo(a)',
    'União estável',
  ];
  protected readonly qualificacao = [
    { k: 'data_nascimento', r: 'Data de nascimento', tipo: 'date' },
    { k: 'profissao', r: 'Profissão', tipo: 'text' },
    { k: 'estado_civil', r: 'Estado civil', tipo: 'select' },
    { k: 'rg', r: 'RG', tipo: 'text' },
    { k: 'rg_orgao_emissor', r: 'Órgão emissor', tipo: 'text' },
    { k: 'nacionalidade', r: 'Nacionalidade', tipo: 'text' },
  ];
  protected readonly endereco = [
    { k: 'cep', r: 'CEP' },
    { k: 'logradouro', r: 'Logradouro', larga: true },
    { k: 'numero', r: 'Número' },
    { k: 'complemento', r: 'Complemento' },
    { k: 'bairro', r: 'Bairro' },
    { k: 'cidade', r: 'Cidade', larga: true },
    { k: 'uf', r: 'UF' },
  ];
  protected readonly representanteCampos = [
    { k: 'nome', r: 'Nome' },
    { k: 'cpf', r: 'CPF' },
    { k: 'cargo', r: 'Cargo' },
    { k: 'telefone', r: 'Telefone' },
    { k: 'data_nascimento', r: 'Data de nascimento', tipo: 'date' },
    { k: 'rg', r: 'RG' },
    { k: 'rg_orgao_emissor', r: 'Órgão emissor' },
    { k: 'nacionalidade', r: 'Nacionalidade' },
    { k: 'estado_civil', r: 'Estado civil', tipo: 'select' },
  ];
  constructor() {
    effect(() => {
      const c = this.contato();
      this.form = c
        ? {
            versao: c.versao,
            dados: { ...c.dados },
            representantes: c.representantes.map((r) => ({ ...r })),
            indicadores: [...c.indicadores],
          }
        : contatoVazio();
    });
  }
  protected tipoPessoa(tipo: string) {
    this.form.dados['tipo_pessoa'] = tipo;
  }
  protected mudar(campo: string, valor: string) {
    if (campo === 'cep') {
      valor = valor
        .replace(/\D/g, '')
        .slice(0, 8)
        .replace(/(\d{5})(\d)/, '$1-$2');
      this.cepSequencia++;
    }
    if (campo === 'uf') valor = valor.toUpperCase().slice(0, 2);
    this.form.dados[campo] = valor;
  }
  protected alternarIndicador(id: string) {
    this.form.indicadores = this.form.indicadores.includes(id)
      ? this.form.indicadores.filter((x) => x !== id)
      : [...this.form.indicadores, id];
  }
  protected adicionarRepresentante() {
    this.form.representantes.push(
      Object.fromEntries(this.representanteCampos.map((c) => [c.k, ''])),
    );
  }
  protected removerRepresentante(i: number) {
    this.form.representantes.splice(i, 1);
  }
  protected buscarCep() {
    const cep = this.form.dados['cep'].replace(/\D/g, '');
    if (cep.length !== 8) return;
    const sequencia = ++this.cepSequencia;
    const anteriores = { ...this.form.dados };
    this.buscandoCep.set(true);
    this.erroCep.set('');
    this.api
      .cep(cep)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (r) => {
          this.buscandoCep.set(false);
          if (sequencia !== this.cepSequencia) return;
          if (r.erro) {
            this.erroCep.set('CEP não encontrado. Preencha o endereço manualmente.');
            return;
          }
          for (const [campo, valor] of Object.entries({
            logradouro: r.logradouro,
            bairro: r.bairro,
            cidade: r.localidade,
            uf: r.uf,
          })) {
            if (valor && this.form.dados[campo] === anteriores[campo])
              this.form.dados[campo] = valor;
          }
        },
        error: () => {
          this.buscandoCep.set(false);
          if (sequencia === this.cepSequencia)
            this.erroCep.set('Não foi possível consultar o CEP. Preencha o endereço manualmente.');
        },
      });
  }
  protected criarIndicador() {
    if (!this.novoIndicador.trim() || this.criandoIndicador()) return;
    this.criandoIndicador.set(true);
    this.api
      .indicador(this.novoIndicador)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (i) => {
          this.indicadorCriado.emit(i);
          if (!this.form.indicadores.includes(i.id)) this.form.indicadores.push(i.id);
          this.novoIndicador = '';
          this.criandoIndicador.set(false);
        },
        error: (e) => {
          this.erro.set(e.error?.detail || 'Não foi possível criar o indicador.');
          this.criandoIndicador.set(false);
        },
      });
  }
  protected salvar() {
    if (this.salvando() || !this.opcoes().podeEditar) return;
    this.salvando.set(true);
    this.erro.set('');
    const payload = {
      ...this.form,
      dados: { ...this.form.dados },
      representantes:
        this.form.dados['tipo_pessoa'] === 'PJ'
          ? this.form.representantes.map((r) => ({ ...r }))
          : [],
      indicadores: [...this.form.indicadores],
    };
    this.api
      .salvar(payload, this.contato()?.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (c) => {
          this.salvando.set(false);
          this.salvo.emit(c);
        },
        error: (e) => {
          this.salvando.set(false);
          this.erro.set(e.error?.detail || 'Não foi possível salvar o contato. Tente novamente.');
        },
      });
  }
}
