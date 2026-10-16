# Agenda — lembretes e notificações internas

Entrega de 16/10/2026 na branch feature/adv-002-perfis-permissoes, sobre e1541c9.

## Referência e decisões

Consultados docs/index.html e ADV-026 no Notion. A origem contém central interna de notificações com leitura, abertura da origem e atualização periódica, além de notificações por menção. Não foi identificada uma regra de antecedência ou entrega de lembretes da Agenda; as ocorrências de lembrete encontradas no HTML pertencem principalmente a assinatura de documentos. As regras abaixo são decisões explícitas do destino.

Implementados avisos internos para atividades pendentes em que o usuário autenticado é responsável, incluindo tarefas, prazos, reuniões e audiências. Atividades sem responsável e tarefas sem data não geram avisos. O administrador não recebe automaticamente avisos de toda a equipe. Não há envio de email, WhatsApp, SMS, web push ou avisos para contatos.

Cada usuário configura seus lembretes: ativos/inativos e antecedência de 0, 15, 60 ou 1440 minutos; padrão ativo, um dia antes. A antecedência vale para os marcos da atividade e do prazo fatal. Usa-se a data/horário inicial; atividade sem horário e prazo fatal manual recebem horário de lembrete às 09h de Brasília, que não altera o horário real nem a data do prazo. Datas e horários são civis em America/Sao_Paulo; não há cálculo jurídico de dias úteis.

Se execução e prazo fatal coincidirem na data e às 09h, aparece apenas um aviso de prazo fatal. Havendo horários ou datas diferentes, podem existir dois avisos. Avisos vencidos permanecem enquanto a atividade estiver pendente e o usuário não os marcar como vistos. Não há janela arbitrária para apagar pendências antigas; a consulta é paginada.

## Leitura, alterações e recorrência

A leitura é pessoal e não conclui a atividade. Outro responsável continua vendo seu próprio aviso. A marcação é idempotente por escritório, usuário, atividade, versão e marco. Editar a ficha aumenta a versão e renova os avisos, mesmo se mudar somente o título. Concluir, cancelar ou retirar a responsabilidade elimina o aviso da consulta; reabrir renova a versão. Uma próxima tarefa recorrente tem UUID próprio e aparece conforme a sua data e a antecedência pessoal.

Não há gravação ao consultar avisos. As consultas calculam os avisos atuais a partir da Agenda, sem criar notificações ou executar um agendador. Persistem-se apenas preferências e marcações de leitura. Isso evita avisos obsoletos de atividades já concluídas, adiadas ou delegadas.

## Isolamento e permissões

Escritório e usuário vêm exclusivamente da autenticação. Consultas e marcações exigem acesso ao módulo Agenda, escritório ativo e usuário ativo. Todos os perfis autorizados, inclusive FINANCEIRO de consulta, podem configurar seus próprios lembretes e marcar seus avisos; isso não concede edição da Agenda.

A consulta filtra responsabilidade atual, empresa, situação pendente e antecedência salva. Marcar uma atividade de outra empresa, de outro responsável, fora da janela ou com versão antiga retorna 404 sem revelar detalhes. Desativar lembretes antes de marcar retorna conflito. Mutações exigem CSRF. As preferências sempre se aplicam ao usuário autenticado; identificações enviadas pelo cliente não concedem acesso a outro usuário ou escritório. Não são retornados contatos, documentos, descrições ou dados da ficha além de título/tipo e marco do lembrete.

O GET responde Cache-Control: no-store. Preferências e leituras usam transação e bloqueio do escritório para serializar com alterações das atividades. Chaves compostas no banco impedem referências entre escritórios.

## API

- GET /api/agenda/lembretes: preferencias, itens, total, pagina, tamanho, consultadoEm e fusoHorario; pagina padrão 0, tamanho padrão 20, máximo 50.
- PUT /api/agenda/lembretes/preferencias: ativo e antecedenciaMinutos do próprio usuário.
- PATCH /api/agenda/lembretes/{atividadeId}/lido: versao e marco (ATIVIDADE ou PRAZO_FATAL); 204 após confirmação.
- Cada item contém atividadeId, versao, titulo, tipo, marco, data e hora.

## Interface

O sino mostra a contagem de lembretes da Agenda, atualizada a cada 45 segundos, mesmo com o painel fechado. O contador é ocultado se a consulta falhar; não se apresenta uma falha como zero avisos. A assinatura é encerrada com a saída da tela e não roda na renderização do servidor.

O painel apresenta Agenda ao lado da seção CRM existente, preferências pessoais, atualização manual, paginação, marcar como visto e abrir atividade. Não remove o aviso nem navega antes da resposta de leitura. Falhas conservam a lista e permitem tentar novamente. Alterações não salvas de preferências são preservadas; a atualização periódica não sobrescreve o rascunho. Leitura e mudança de preferências atualizam o contador.

Abrir usa /agenda?atividade=UUID e consulta a ficha no servidor mesmo fora do período filtrado. O UUID do link é validado; referências não autorizadas continuam bloqueadas na API. O formulário só abre depois de carregar as opções/permissões e preparar a tela. Ao fechar, salvar ou falhar a consulta da ficha na API, o parâmetro é limpo. Um link diferente não substitui uma ficha já aberta com rascunho. O painel possui rolagem própria e cabe no celular.

O contador do sino mede os lembretes da Agenda. As notificações CRM continuam em sua seção e não são somadas artificialmente nessa contagem.

## Arquitetura e banco

Backend por funcionalidade: LembretesAgendaController e records HTTP próprios; LembretesAgendaService coordena os casos de uso; PreferenciaLembrete valida opções e calcula o limite em Brasília sem frameworks; LembretesAgendaRepository define contrato; JdbcLembretesAgendaRepository realiza consultas e persistência.

V32 cria agenda_lembrete_preferencia e agenda_lembrete_lido, com chaves compostas por escritório/usuário e escritório/atividade, constraints e índice para pendências. V1–V31 não foram alteradas. As consultas usam CTEs/união e uma consulta para total e outra para a página; não carregam todas as atividades na aplicação nem fazem consulta por aviso.

## Validações

- Backend: mvn clean test, 170 testes aprovados, incluindo 22 de Agenda e verificações de arquitetura.
- PostgreSQL real: os 22 testes de Agenda aprovados com transações revertidas e sem envio de emails. Inclui pessoalidade, idempotência, renovação, delegação, cancelamento, recorrência, preferências, paginação, fuso, horários, empresa, módulo, CSRF e perfil de consulta.
- Frontend: 124 testes em 40 arquivos aprovados; build Angular aprovado e app final compilado no Docker.
- API e app locais reconstruídos; V32 aplicada com sucesso no PostgreSQL e API iniciada.
- Chrome desktop/celular com APIs simuladas: contador, falha de leitura sem remoção, navegação só após confirmação, ficha fora do filtro e URL limpa ao fechar, preferências, desativar/reativar e marcar como visto. Sem exceções JavaScript ou overflow horizontal.
- Roteiro: tmp/validar-agenda-lembretes.mjs. Capturas: docs/agenda-lembretes-desktop.png, agenda-lembretes-ficha-desktop.png e agenda-lembretes-mobile.png.

## Continuidade

Disponível no sino em http://localhost:4200/agenda. Etapa implementada e validada, em revisão com a sessão real do usuário; não foi atribuída uma confirmação ainda não recebida. Sem push nesta sessão.

Próxima entrega planejada: execução de workflows integrada à Agenda. Google Agenda, integração automática de consultas CRM e casos/processos e canais externos de notificação continuam em etapas próprias. O módulo completo permanece em migração.
