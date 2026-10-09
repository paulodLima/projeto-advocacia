import { Component, computed, effect, inject, signal } from '@angular/core';
import { AcessoService } from '../../../../core/auth/acesso.service';
import { ActivatedRoute, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { registerLocaleData } from '@angular/common';
import pt from '@angular/common/locales/pt';
import { ABAS_CONFIGURACAO, AbaConfiguracao } from '../../models/configuracoes.model';
import { ConfiguracoesLocalService } from '../../services/configuracoes-local.service';
import { PerfilConfig } from '../../components/perfil-config';
import { EmpresaConfig } from '../../components/empresa-config';
import { EquipeConfig } from '../../components/equipe-config';
import { DocumentosConfig } from '../../components/documentos-config';
import { CadastrosConfig } from '../../components/cadastros-config';
import { WorkflowsConfig } from '../../components/workflows-config';
import { FinanceiroConfig } from '../../components/financeiro-config';
import { OliviaConfig } from '../../components/olivia-config';
import { SenhasConfig } from '../../components/senhas-config';
import { TemaConfig } from '../../components/tema-config';
registerLocaleData(pt);

@Component({
  selector: 'app-configuracoes',
  imports: [PerfilConfig, EmpresaConfig, EquipeConfig, DocumentosConfig, CadastrosConfig, WorkflowsConfig, FinanceiroConfig, OliviaConfig, SenhasConfig, TemaConfig],
  templateUrl: './configuracoes.html',
  styleUrl: './configuracoes.scss',
})
export class Configuracoes {
  protected readonly store = inject(ConfiguracoesLocalService);
  private readonly acesso = inject(AcessoService);
  protected readonly abas = computed(() => ABAS_CONFIGURACAO.filter(aba => this.acesso.permiteConfiguracao(aba.id)));
  protected readonly aba = signal<AbaConfiguracao>('perfil');
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  constructor() {
    this.route.queryParamMap.pipe(takeUntilDestroyed()).subscribe(parametros => {
      const id = parametros.get('secao');
      this.aba.set(this.abas().find(a => a.id === id)?.id ?? 'perfil');
      this.store.aviso.set('');
    });
    effect(() => { if (!this.acesso.permiteConfiguracao(this.aba())) this.aba.set('perfil'); });
  }
  protected selecionar(aba: AbaConfiguracao) { void this.router.navigate([], { relativeTo: this.route, queryParams: { secao: aba }, queryParamsHandling: 'merge' }); }
}
