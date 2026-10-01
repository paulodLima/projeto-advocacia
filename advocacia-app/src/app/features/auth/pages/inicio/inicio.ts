import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/auth/auth.service';
import { Icone } from '../../../../shared/components/icone/icone';
import { PainelInicio } from '../../../../shared/components/painel-inicio/painel-inicio';

@Component({
  selector: 'app-inicio',
  imports: [RouterLink, Icone, PainelInicio],
  templateUrl: './inicio.html',
  styleUrl: './inicio.scss',
})
export class Inicio {
  protected readonly auth = inject(AuthService);
  protected readonly primeiroNome = computed(() => this.auth.usuario()?.nome.trim().split(/\s+/)[0] || '');
  protected readonly saudacao = new Date().getHours() < 12 ? 'BOM DIA' : new Date().getHours() < 18 ? 'BOA TARDE' : 'BOA NOITE';
  protected readonly indicadoresAbertos = signal(true);
  protected readonly atualizacao = signal('movimentacoes');
  protected readonly indicadores = [
    { label: 'Processos ativos', apoio: 'judiciais e administrativos em curso', icon: 'scale', destino: 'processual', cor: 'verde' },
    { label: 'Tarefas pendentes', apoio: 'prazos e compromissos a cumprir', icon: 'file', destino: 'agenda', cor: 'taupe' },
    { label: 'Leads em cadência', apoio: 'oportunidades em acompanhamento', icon: 'crm', destino: 'crm', cor: 'dourado' },
    { label: 'Casos em andamento', apoio: 'trabalho que ainda não virou processo', icon: 'folder-open', destino: 'casos', cor: 'verde' },
  ];
  protected readonly atalhos = [
    { label: 'Agenda', icon: 'calendar', destino: 'agenda' },
    { label: 'Casos', icon: 'folder-open', destino: 'casos' },
    { label: 'Contatos', icon: 'users', destino: 'contatos' },
    { label: 'Documentos', icon: 'file', destino: 'documentos' },
  ];
}
