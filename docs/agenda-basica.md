# Agenda básica do escritório

Entrega de 12/10/2026 na branch `feature/adv-002-perfis-permissoes`. Base anterior publicada no commit `a15f25a`, com convites e gestão administrativa das empresas. A Agenda é a próxima entrega, desenvolvida separadamente dessa base.

## Referência e escopo

Consultados `docs/index.html`, ADV-026 e suas dependências no Notion. O destino tinha somente pacote reservado e rotas vazias para Agenda. A origem contém tarefas, prazos, reuniões e audiências; título livre ou tipo configurado; data inicial/final, prazo fatal, dia inteiro, horário inicial/final, modalidade/local, múltiplos responsáveis e operações de cumprir/reabrir, adiar e delegar.

Esta etapa entrega atividades manuais com calendário mensal e lista paginada, filtros por período/tipo/situação/responsável/título, cadastro/edição, conclusão/cancelamento/reabertura e histórico. Adiar e delegar são feitos editando datas e responsáveis. A tarefa pode ficar sem data. O tipo de tarefa usa o UUID do cadastro existente; renomear o cadastro não altera o título histórico e não perde o vínculo. Um tipo usado na Agenda não pode ser excluído; pode ser desativado.

Integrações identificadas na origem ficam para as próximas etapas: processos/casos, participantes de Contatos, categorias/flags de tarefas, taskscore, anexos/comentários/menções, recorrência, notificações, execução de workflows, cálculo jurídico de prazos, aniversários e Google Agenda. Nenhuma chamada externa do HTML foi tomada como comprovação de funcionamento do servidor.

## Operação

Abrir `/agenda` com usuário ativo de escritório ativo e módulo liberado. O calendário mostra totais do dia, com destaque para prazos fatais pendentes. Clique num dia para ver suas atividades; clique novamente ou em **Ver período inteiro** para limpar a seleção. A listagem inclui as tarefas sem data quando a opção estiver marcada. O calendário não depende da primeira página da lista: seus totais consideram todas as atividades do período.

Em **Nova atividade**, escolher o tipo, escrever título ou selecionar tipo de tarefa, informar datas e escolher até 20 responsáveis. Reuniões/audiências exigem data; o horário inicial/final é obrigatório quando não forem de dia inteiro. Prazo exige prazo fatal manual. Para concluir, cancelar ou reabrir, abrir a atividade e confirmar a operação. Cancelamento exige motivo e mantém o registro; providência/observação nas demais operações é opcional. Salvar alterações do formulário antes de mudar a situação evita descartar um rascunho.

Concluídas/canceladas precisam ser reabertas antes de editar. Uma versão desatualizada retorna conflito: o formulário conserva o rascunho e oferece **Recarregar ficha**, que descarta o rascunho e consulta a versão atual. Nenhum sucesso é mostrado antes da resposta do servidor.

## Regras e acesso

- Empresa obtida exclusivamente do vínculo autenticado. IDs de atividades de outra empresa retornam 404. `empresaId` enviado pelo cliente não concede acesso.
- Empresa suspensa/encerrada e usuário inativo são bloqueados pelo filtro de segurança, inclusive em sessões abertas. Módulo Agenda retirado retorna 403.
- MASTER, ADMINISTRADOR, ADVOGADO e ASSISTENTE com acesso a Agenda podem editar todas as atividades do próprio escritório; FINANCEIRO pode consultar. As permissões da equipe são intersectadas com a liberação contratual.
- Responsáveis são contas/membros ativos da própria empresa; tipos de tarefa precisam estar ativos na mesma empresa para salvar. Opções inativas continuam identificadas nas consultas e devem ser removidas/substituídas numa edição. Alterar situação não exige substituir referências históricas inativas. Excluir uma conta remove sua responsabilidade, preservando a atividade; o histórico mantém o nome do autor.
- Datas e horários são civis, no fuso `America/Sao_Paulo`, sem conversão do horário escolhido para UTC. Histórico/conclusão usam instantes; a tela os exibe em Brasília.
- Prazo fatal é aceito para tarefa/prazo e não pode preceder a data de execução/fim. Aparece no calendário e na listagem também quando estiver fora da data planejada. Se fatal e execução caírem no mesmo dia, a atividade é contada uma vez. Compromissos de vários dias aparecem em cada dia do intervalo.
- Períodos de consulta: até 366 dias; resumo do calendário: até 62 dias. Paginação padrão 50, máximo 100. Títulos/busca até 150 caracteres, descrição até 4000, local/link até 1000, observação de histórico até 2000, até 20 responsáveis. Datas finais podem ir até 365 dias depois do início.
- Conteúdos são texto, sem execução de HTML. Links online aceitam HTTP/HTTPS sem credenciais embutidas. Dia inteiro elimina horários; horário final no mesmo dia precisa ser posterior ao inicial.

## Arquitetura e banco

Backend organizado em `agenda/api/request`, `api/response`, `application/command`, `application`, `domain/repository` e `infrastructure/persistence/repository`. Controllers não acessam SQL/repositories e não declaram DTOs internos. O domínio valida dados sem framework; `AgendaService` coordena autorização, regras e transação; o adaptador JDBC persiste e converte. Nenhuma migration anterior foi alterada.

V24 cria atividades, responsáveis e eventos, com índices por empresa/data/prazo e chaves compostas para impedir referências entre empresas. A versão e o bloqueio do escritório serializam escritas; gravação da atividade, responsáveis e evento é atômica. Consultas da lista carregam responsáveis em lote, sem uma consulta por atividade; histórico é carregado somente na ficha. O resumo do calendário lê somente marcos de data/situação e não recebe descrições ou históricos. Os testes de arquitetura foram ampliados para incluir Agenda.

Frontend separado em modelos, serviço de API, página da Agenda, calendário mensal e formulário. Tema usa as variáveis existentes do escritório. O formulário abre em dialog nativo, com foco/modalidade e fechamento por teclado; fechamento é bloqueado durante gravação. Consultas anteriores são canceladas ao mudar filtros.

## API

- `GET /api/agenda/opcoes`: permissão de edição, responsáveis, tipos de tarefa e fuso.
- `GET /api/agenda?inicio=AAAA-MM-DD&fim=AAAA-MM-DD`: lista; opcionais `tipo`, `situacao`, `responsavel`, `busca`, `incluirSemData`, `pagina`, `tamanho`.
- `GET /api/agenda/calendario?inicio=AAAA-MM-DD&fim=AAAA-MM-DD`: totais diários; mesmos filtros, sem tarefas sem data.
- `GET /api/agenda/{id}`: ficha e histórico.
- `POST /api/agenda`: cria com versão zero.
- `PUT /api/agenda/{id}`: edita com versão atual.
- `PATCH /api/agenda/{id}/situacao`: `versao`, `situacao` e `observacao`.

POST/PUT recebem tipo (`TAREFA`, `PRAZO`, `REUNIAO`, `AUDIENCIA`), título, tipoTarefaId opcional, data/dataFim, hora/horaFim, prazoFatal, diaInteiro, modalidade (`PRESENCIAL`/`ONLINE`), local, descrição e UUIDs de responsáveis. Mutações exigem CSRF. A resposta traz id, versão, dados, situação, timestamps e histórico; não expõe empresaId como contexto selecionável pelo cliente.

## Validações e continuidade

- `mvn clean test`: 122 testes sem falhas/erros, incluindo sete cenários da Agenda e quatro verificações de arquitetura.
- Os sete testes da Agenda também passaram contra o PostgreSQL local, usando fixtures em transações revertidas ao final; sem envio de emails.
- Frontend: 91 testes em 32 arquivos passaram, incluindo sete testes da Agenda para formulário, consulta e proteção do rascunho. Build Angular e imagens Docker passaram, sem aviso de orçamento dos estilos da Agenda.
- PostgreSQL validou 24 migrations e aplicou V24 sobre V23; API e app locais atualizados. Agenda passou de EM_DESENVOLVIMENTO para DISPONIVEL no catálogo, preservando as liberações por empresa.
- Chrome desktop/celular com APIs simuladas validou prazo com múltiplos responsáveis, conflito/rascunho/recarregamento, edição, conclusão/reabertura/cancelamento e perfil de consulta. Sem erros JavaScript ou overflow horizontal. Capturas em `docs/agenda-desktop.png`, `docs/agenda-mobile.png` e `docs/agenda-ficha-mobile.png`; roteiro em `tmp/validar-agenda.mjs`.

Entrega em revisão para aceite do usuário com sessão real no ambiente local. Próximo passo: validar o fluxo e então integrar participantes de Contatos, recorrência/lembretes e workflows em entregas próprias. A primeira etapa não calcula prazos jurídicos nem sincroniza o Google Agenda.

## Fechamento e retomada em 13/10/2026

O usuário solicitou commit/push da Agenda e encerramento da sessão. A publicação será registrada no Notion com o hash confirmado do commit, na branch `feature/adv-002-perfis-permissoes`.

Na retomada, começar pela confirmação visual com o usuário: ele relatou que ainda via **Em breve**. A consulta ao frontend local confirmou a nova rota `/agenda` e o bundle com a Agenda implementada no container da porta 4200. Não foi comprovado qual endereço/versão estava aberto na sessão dele. Orientação dada: abrir `http://localhost:4200/agenda` com usuário do escritório e atualizar com Ctrl + Shift + R. A URL efetivamente utilizada e o aceite com sessão real permanecem pendentes; não tratar o relato como resolvido apenas com os testes automatizados.

Depois de confirmar o acesso e corrigir eventuais problemas, seguir com participantes/clientes de Contatos vinculados aos compromissos. Recorrência, lembretes, workflows e Google Agenda seguem a ordem definida no planejamento, em entregas posteriores. Consultar `docs/index.html` e a tarefa ativa no Notion antes de implementar.
