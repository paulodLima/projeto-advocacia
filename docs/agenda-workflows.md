# Execução de workflows na Agenda

## Escopo e referência

Branch: feature/adv-002-perfis-permissoes. Planejamento: ADV-028 no Notion (3ed040a7-8ecb-8170-968a-dd3748ae5633), vinculado à entrega da Agenda ADV-026.

Referência funcional consultada: docs/index.html. A origem oferece workflow ao selecionar um tipo de tarefa, exige prazo fatal manual e chama gerar_workflow. O corpo dessa função externa não está disponível no HTML. Portanto, a regra detalhada abaixo é uma decisão explícita desta implementação, não uma regra de servidor comprovada da origem.

## Uso e planejamento

Na Agenda, criar uma atividade do tipo tarefa ou prazo e escolher um tipo de tarefa configurado como gatilho. Ativar “Gerar etapas de um workflow”, escolher um modelo, informar o prazo fatal manual e conferir as etapas antes de gerar. Se houver vários modelos para o mesmo tipo, a escolha é explícita. O rascunho da atividade individual permanece guardado ao alternar entre os modos.

O planejamento usa segunda a sexta. Feriados e dias sem expediente são informados para cada execução, até 366 datas únicas; não há inferência de feriados nacionais, locais, recessos ou suspensões judiciais.

A contagem parte do prazo fatal para trás. A última etapa termina em D menos a reserva final do modelo, em dias de trabalho. Reserva zero e prazo em dia não útil posicionam o fim no dia útil anterior. A duração de cada etapa é inclusiva: uma etapa de um dia começa e termina na mesma data; uma de dois dias ocupa dois dias úteis. A etapa anterior termina no dia útil anterior ao início da seguinte.

O prazo fatal informado permanece inalterado como referência da execução. As datas geradas são planejamento interno; não há cálculo jurídico automático. Cada etapa gera uma tarefa independente, com título e responsável configurados, datas de início/fim e histórico de criação. Não se cria uma atividade individual adicional. Não se preenchem prazo fatal, participantes, recorrência ou gatilhos encadeados nas tarefas geradas. A referência fatal e o planejamento original ficam visíveis na ficha, na seção de origem.

## Regras e persistência

O escritório vem da sessão autenticada. Acesso à Agenda e usuário ativo são obrigatórios. MASTER, administrador, advogado e assistente podem executar; financeiro permanece em consulta. A configuração dos modelos mantém as permissões existentes.

A prévia não escreve dados. A geração exige a versão do modelo e a assinatura da prévia, recalcula no servidor e rejeita alterações silenciosas no planejamento ou nos responsáveis. Responsáveis precisam estar ativos no mesmo escritório. Os registros de execução, etapas e atividades são gravados na mesma transação, sob bloqueio do escritório.

Um identificador de requisição por escritório evita duplicidade de reenvios idênticos. O formulário reutiliza esse identificador ao repetir a geração após uma falha. Reutilizá-lo com outros parâmetros gera conflito. Um reenvio confirmado retorna os mesmos IDs e o mesmo planejamento, inclusive depois de editar ou excluir o modelo.

Cada execução preserva nome, versão, gatilho, prazo manual, calendário informado, nomes/responsáveis das etapas e datas originais. Editar ou excluir o modelo não altera tarefas já geradas. Cada tarefa pode ser editada, concluída ou cancelada separadamente, preservando a origem. Não há propagação automática de mudanças entre etapas.

Backend organizado em controller/records HTTP, serviço de aplicação, comandos, domínio de planejamento e contrato/adaptador de persistência. A migration V33 cria agenda_workflow_execucao, agenda_workflow_exclusao e agenda_workflow_etapa, com chaves compostas por escritório e unicidade de requisições. V1–V32 preservadas.

## Validação

- Backend: regressão final com 173 testes aprovados; 25 testes de Agenda passaram com os cenários novos de geração, snapshots, conflitos, isolamento, permissões, CSRF e idempotência.
- PostgreSQL real: os mesmos 25 testes passaram em transações revertidas, sem envio de emails.
- Frontend: 128 testes em 41 arquivos aprovados, incluindo prévia obrigatória, invalidação por mudança de prazo, reenvio com identificador estável e rascunho manual preservado.
- Build Angular aprovado; containers locais API/app reconstruídos e V33 aplicada no PostgreSQL.
- Chrome desktop e celular com APIs simuladas: adesão ao workflow, prazo manual, prévia, geração confirmada, origem na ficha e ausência de overflow horizontal ou exceções JavaScript.
- Roteiro: tmp/validar-agenda-workflows.mjs. Capturas: agenda-workflow-desktop.png e agenda-workflow-mobile.png.

## Continuidade

Disponível em http://localhost:4200/agenda para revisão com a sessão real do usuário. Essa revisão ainda não foi atribuída como recebida. A execução de workflows entra em validação; o módulo completo de Agenda permanece em migração.

Próxima etapa: conferir os fluxos existentes de consultas CRM e casos/processos na origem e no código, definir os vínculos autorizados por escritório e integrar sua criação/atualização à Agenda. Google Agenda e canais externos de notificação seguem como entregas próprias.
