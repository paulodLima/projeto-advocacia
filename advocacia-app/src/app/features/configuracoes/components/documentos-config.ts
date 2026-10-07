import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import { DocumentosApiService, DocumentosConfigDados } from '../services/documentos-api.service';
import { Component, DestroyRef, inject, signal, viewChildren } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MODELOS_DOCUMENTO, VARIAVEIS_DOCUMENTO } from '../models/referencia';
import { ConfiguracoesLocalService } from '../services/configuracoes-local.service';
import { EmpresaApiService } from '../services/empresa-api.service';
import { EditorModelo } from './editor-modelo';

@Component({ selector: 'app-documentos-config', imports: [FormsModule, EditorModelo], template: `
  <div class="cfg-pilha"><div><h2>Documentos</h2><p>O timbrado e os textos dos documentos que a intranet emite.</p></div>
    @if (aviso()) { <p class="cfg-info" [class.cfg-erro]="erro()" [attr.role]="erro() ? 'alert' : 'status'">{{ aviso() }}</p> }
    @if (carregando()) { <p role="status">Carregando documentos…</p> }
    @if (!pronto() && !carregando()) { <button (click)="carregar()">Tentar novamente</button> }
    @if (pronto() && !dados()?.empresaId) { <p class="cfg-info">Cadastre sua empresa antes de configurar os documentos.</p> }
    <fieldset class="documentos-campos cfg-pilha" [disabled]="!podeEditar || salvando()">
    <section class="cfg-card cfg-pilha"><div><h3>Papel timbrado</h3><p>A marca vai no alto de todo documento; o rodapé, no pé de todas as páginas.</p></div>
      <div class="cfg-grade"><div><h4>Marca do documento</h4><p>PNG ou SVG com fundo transparente.</p><div class="cfg-linha" style="margin-top:8px"><div class="cfg-imagem">@if (documento.logo) { <img [src]="documento.logo" alt="Marca do documento" /> } @else { sem imagem }</div><div><label class="cfg-upload">{{ documento.logo ? 'Trocar imagem' : 'Enviar imagem' }}<input type="file" accept="image/png,image/jpeg,image/svg+xml" (change)="imagem($event, 'logo')" /></label>@if (documento.logo) { <button class="cfg-texto" (click)="documento.logo = ''">remover</button> }</div></div></div>
        <div><h4>Rodapé</h4><p>Faixa larga e baixa — ela ocupa a largura da página.</p><div class="cfg-linha" style="margin-top:8px"><div class="cfg-imagem" style="width:260px;height:60px;max-width:100%">@if (documento.rodape) { <img [src]="documento.rodape" alt="Rodapé do documento" /> } @else { sem imagem }</div><label class="cfg-upload">Enviar imagem<input type="file" accept="image/png,image/jpeg,image/svg+xml" (change)="imagem($event, 'rodape')" /></label>@if (documento.rodape) { <button class="cfg-texto" (click)="documento.rodape = ''">remover</button> }</div>
          <p style="margin-top:8px">Posição da arte no rodapé</p><div class="cfg-linha">@for (opcao of alinhamentos; track opcao.id) { <label class="cfg-check"><input type="radio" name="alinhamento" [value]="opcao.id" [(ngModel)]="documento.alinhamento" />{{ opcao.nome }}</label> }</div>
          <p>Vale quando a arte é mais estreita que a página. Faixa que ocupa a largura inteira fica igual nas três opções.</p><label class="cfg-check" style="margin-top:8px"><input type="checkbox" [(ngModel)]="documento.repetir" />Repetir a arte lado a lado até preencher a largura</label>
        </div></div>
      <div class="cfg-separador cfg-pilha"><div><h3>O que aparece no cabeçalho</h3><p>Vale para recibo, fatura, demonstrativo e cálculo. O nome do escritório e o título do documento aparecem sempre.</p></div>
        <div class="cfg-pilha" style="gap:8px"><label class="cfg-check"><input type="checkbox" [(ngModel)]="documento.documento" />CNPJ, OAB e inscrição municipal</label><label class="cfg-check"><input type="checkbox" [(ngModel)]="documento.endereco" />Endereço</label><label class="cfg-check"><input type="checkbox" [(ngModel)]="documento.contato" />Telefone, e-mail e site</label></div>
        <p>O rodapé é a imagem que você subiu ao lado; ele não segue estas marcas. Sem imagem, o pé da página repete nome e CNPJ.</p>
        <div><button class="cfg-primario" (click)="previa = true">Visualizar o papel timbrado</button><p>Mostra como fica com o que está na tela agora — inclusive o que você ainda não salvou.</p></div>
      </div>
    </section><div><button class="cfg-primario" (click)="salvarPapel()">Salvar papel timbrado</button></div>
    <p>O que você escreve aqui é o que sai nos documentos. O resto — cabeçalho, tabelas e totais — é montado pelo sistema.</p>
    <div class="cfg-dupla"><div class="cfg-principal cfg-pilha">@for (modelo of modelos; track modelo.chave) {
          <section class="cfg-card cfg-pilha"><h3>{{ modelo.titulo }}</h3>@for (trecho of modelo.trechos; track trecho.k) {
        <div><div class="cfg-linha"><label class="cresce" [for]="modelo.chave + '-' + trecho.k">{{ trecho.r }}</label><button class="cfg-texto" (click)="restaurar(modelo.chave, trecho.k, trecho.padrao)">voltar ao texto original</button></div>
          <p>{{ trecho.dica || '' }}</p><app-editor-modelo [rotulo]="trecho.r" [chave]="modelo.chave + ':' + trecho.k" [(ngModel)]="textos[modelo.chave + ':' + trecho.k]" [disabled]="!podeEditar || salvando()" (foco)="focado = modelo.chave + ':' + trecho.k" />
        </div>
      }</section>
    }<div><button class="cfg-primario" (click)="salvarModelos()">Salvar modelos</button></div></div>
      <aside class="cfg-lateral"><section class="cfg-card"><h3>Variáveis</h3><p>Clique para acrescentar ao trecho em que você está.</p>@for (variavel of variaveis; track variavel.v) { <button class="variavel" (click)="inserir(variavel.v)" [disabled]="!focado"><code>{{ '{{' + variavel.v + '}}' }}</code><span>{{ variavel.r }}</span></button> }<p class="cfg-separador">As variáveis marcadas como “frase inteira” já trazem a vírgula e somem quando o dado não existe.</p></section></aside>
    </div>
    </fieldset>
  </div>
  @if (previa) { <div class="cfg-modal" (click)="previa = false" (keydown.escape)="previa = false"><section class="cfg-modal-cartao" role="dialog" aria-modal="true" aria-label="Prévia do papel timbrado" (click)="$event.stopPropagation()"><div class="cfg-linha"><h3 class="cresce">Prévia do papel timbrado</h3><button (click)="previa = false" autofocus>Fechar ×</button></div><div class="papel">
    <header>@if (documento.logo) { <img [src]="documento.logo" alt="Marca do escritório" /> }<h2>{{ dadosEmpresa['nome_fantasia'] || dadosEmpresa['razao_social'] || 'Gestão Advocacia' }}</h2>
      @if (documento.documento) { <p>{{ dadosEmpresa['cnpj'] }} {{ dadosEmpresa['oab_sociedade'] || dadosEmpresa['oab'] }} {{ dadosEmpresa['inscricao_municipal'] }}</p> } @if (documento.endereco) { <p>{{ dadosEmpresa['endereco'] }} {{ dadosEmpresa['cidade'] }} {{ dadosEmpresa['uf'] }}</p> } @if (documento.contato) { <p>{{ dadosEmpresa['telefone'] }} {{ dadosEmpresa['email'] }} {{ dadosEmpresa['site'] }}</p> }
    </header><h3>Documento de exemplo</h3><p>Exemplo de conteúdo</p><table><thead><tr><th>Descrição</th><th>Valor</th></tr></thead><tbody><tr><td>Primeira linha de exemplo</td><td>R$ 1.000,00</td></tr><tr><td>Segunda linha de exemplo</td><td>R$ 2.500,00</td></tr></tbody></table>
    <footer [style.text-align]="documento.alinhamento === 'esquerda' ? 'left' : documento.alinhamento === 'direita' ? 'right' : 'center'">@if (documento.rodape) { @if (documento.repetir) { <div style="display:flex;overflow:hidden">@for (n of [1,2,3,4,5]; track n) { <img [src]="documento.rodape" alt="Rodapé" style="flex-shrink:0;width:auto;max-width:30%" /> }</div> } @else { <img [src]="documento.rodape" alt="Rodapé" /> } } @else { <p>{{ dadosEmpresa['razao_social'] || 'Gestão Advocacia' }} {{ dadosEmpresa['cnpj'] }}</p> }</footer>
  </div></section></div> }
`, styles: `.documentos-campos { border:0;padding:0;margin:0;min-width:0; } .variavel { display:block; width:100%; text-align:left; background:transparent; border:0; padding:5px 0; } .variavel code { color:var(--tema-primaria);font-size:11px;display:block; } .variavel span { color:#7d6c5e;font-size:10px;display:block; } .papel { margin-top:16px;background:white;padding:48px 32px;min-height:650px;display:flex;flex-direction:column;gap:20px; } .papel header img { max-width:120px;max-height:75px; } .papel footer { margin-top:auto; } .papel footer img { max-width:100%;max-height:70px; } .texto-previa { min-height:40px;background:white;border:1px solid #6b635b24;border-radius:8px;padding:12px;font-size:12px; }` })
export class DocumentosConfig {
  protected readonly store = inject(ConfiguracoesLocalService);
  private readonly empresaApi = inject(EmpresaApiService);
  protected get dadosEmpresa() { return this.empresaApi.empresa()?.dados ?? {}; }
  protected readonly modelos = MODELOS_DOCUMENTO;
  protected readonly variaveis = VARIAVEIS_DOCUMENTO;
  private readonly editores = viewChildren(EditorModelo);
  protected readonly alinhamentos = [{ id: 'esquerda', nome: 'Lateral esquerda' }, { id: 'centro', nome: 'Centralizado' }, { id: 'direita', nome: 'Lateral direita' }];
  protected documento = { logo: '', rodape: '', alinhamento: 'centro', repetir: false, documento: true, endereco: true, contato: true };
  protected textos: Record<string, string> = Object.fromEntries(this.modelos.flatMap(m => m.trechos.map(t => [m.chave + ':' + t.k, t.padrao])));
  protected focado = '';
  protected previa = false;
  private readonly api = inject(DocumentosApiService);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly dados = signal<DocumentosConfigDados | null>(null);
  protected readonly pronto = signal(false);
  protected readonly carregando = signal(false);
  protected readonly salvando = signal(false);
  protected readonly aviso = signal('');
  protected readonly erro = signal(false);
  protected get podeEditar() { return this.pronto() && this.dados()?.papel === 'MASTER'; }
  constructor() { this.store.aviso.set(''); this.carregar(); }
  protected carregar() {
    if (this.carregando()) return;
    this.carregando.set(true); this.aviso.set('');
    this.api.carregar().pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.carregando.set(false))).subscribe({
      next: dados => { this.dados.set(dados); this.documento = { ...dados.documento }; this.textos = Object.fromEntries(this.modelos.flatMap(m => m.trechos.map(t => [m.chave + ':' + t.k, dados.modelos[m.chave + ':' + t.k] ?? t.padrao]))); this.pronto.set(true); this.erro.set(false); },
      error: erro => this.falha(erro),
    });
  }
  private falha(erro: { error?: { detail?: string; erros?: string[] } }) { this.erro.set(true); this.aviso.set(erro.error?.erros?.join(' · ') || erro.error?.detail || 'Não foi possível carregar ou salvar. Suas alterações permanecem na tela.'); }
  protected async imagem(event: Event, campo: 'logo' | 'rodape') {
    if (!this.podeEditar || this.salvando()) return;
    const imagem = await this.store.imagem(event, 1400, 'image/png');
    if (imagem) { this.documento[campo] = imagem; this.erro.set(false); this.aviso.set('Imagem preparada. Salve o papel timbrado para publicar.'); }
    else if (this.store.aviso()) { this.erro.set(true); this.aviso.set(this.store.aviso()); }
  }
  protected salvarPapel() {
    if (!this.podeEditar || this.salvando()) return;
    this.salvando.set(true); this.aviso.set('');
    this.api.salvarPapel({ ...this.documento }).pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.salvando.set(false))).subscribe({
      next: papel => { this.documento = { ...papel }; this.erro.set(false); this.aviso.set('Papel timbrado salvo para toda a empresa.'); }, error: erro => this.falha(erro),
    });
  }
  protected salvarModelos() {
    if (!this.podeEditar || this.salvando()) return;
    this.salvando.set(true); this.aviso.set('');
    this.api.salvarModelos({ ...this.textos }).pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.salvando.set(false))).subscribe({
      next: modelos => { this.textos = { ...modelos }; this.erro.set(false); this.aviso.set('Modelos salvos para toda a empresa.'); }, error: erro => this.falha(erro),
    });
  }
  protected restaurar(modelo: string, trecho: string, padrao: string) { if (this.podeEditar && !this.salvando()) this.textos[modelo + ':' + trecho] = padrao; }
  protected inserir(variavel: string) { if (this.podeEditar && !this.salvando()) this.editores().find(e => e.chave() === this.focado)?.inserir('{{' + variavel + '}}'); }
}

