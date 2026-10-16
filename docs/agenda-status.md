# Resumo da entrega da Agenda — 16/10/2026

Branch: feature/adv-002-perfis-permissoes.

## Implementado

- Agenda com tarefas, prazos manuais, reuniões e audiências, calendário/lista, filtros, responsáveis, participantes e histórico de edição, conclusão, reabertura e cancelamento.
- Recorrência diária, semanal, mensal e anual: concluir gera somente a próxima ocorrência; reabrir/concluir não duplica. Datas mensais e anuais preservam a base original.
- Lembretes pessoais internos, antecedência configurável, contador no sino, preferências e leitura por usuário; abrir aviso leva à ficha autorizada.
- Execução de workflows por tipo de tarefa, prazo fatal manual, prévia obrigatória, dias úteis com exclusões informadas, responsáveis, histórico e proteção contra geração duplicada.
- Consultas CRM e tarefas dos casos integradas à seção de vínculos e às contagens do calendário. Alterações acompanham a origem; abrir o vínculo leva ao lead ou à tarefa exata do caso.
- Isolamento por escritório autenticado, permissões de cada módulo e privacidade das tarefas. Migrations até V34; versões anteriores preservadas.

## Validações registradas

177 testes na regressão completa do backend; 133 testes frontend em 42 arquivos; 29 testes de Agenda no PostgreSQL real, com rollback. A execução final PostgreSQL inclui recuperação de tarefas cujo tipo de cadastro foi removido. Build Angular, containers locais e schema V34 aprovados. Chrome desktop/celular com APIs simuladas validou vínculos e regressões de workflows/lembretes.

Essas evidências são de desenvolvimento local, sem atribuir aceite com sessão real nem implantação em produção.

## O que falta

- Revisão funcional com a sessão real do usuário e aceite das entregas.
- Lembretes pessoais para consultas CRM e tarefas dos casos vinculadas; os lembretes atuais cobrem atividades próprias da Agenda.
- Integração com Gestão Processual, após implementar seu cadastro e fluxos: hoje o módulo é somente uma reserva.
- Integração com Google Agenda e canais externos de notificação.
- Recorrência automática e execução de workflows no contexto das tarefas dos casos, se incluídas na respectiva entrega. Atualmente esses recursos automáticos pertencem às atividades próprias da Agenda.
- Cálculo jurídico de prazos não está implementado. A data fatal é manual; o planejamento do workflow usa segunda a sexta e datas sem expediente informadas, sem presumir calendários judiciais.

## Próximo passo recomendado

Primeiro validar os fluxos locais em http://localhost:4200/agenda com uma sessão real, incluindo um usuário restrito e uma tarefa privada, corrigindo eventuais problemas antes do aceite.

A próxima implementação recomendada é estender os lembretes pessoais às consultas e tarefas vinculadas, preservando suas permissões, privacidade e fonte de edição. A integração processual depende do futuro módulo de Processos. A Agenda completa permanece em migração; etapas implementadas permanecem em validação até o aceite.

## Commits da sequência

- e1541c9 — recorrência.
- 3adb357 — lembretes pessoais.
- 8a40cf1 — execução de workflows.
- e6f6db3 — integração CRM e tarefas dos casos.

Este resumo consolida o estado atual; relatórios de etapas anteriores preservam o histórico de implementação e validação.
