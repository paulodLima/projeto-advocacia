# Integra??o da developer ? 16/10/2026

Branch: feature/adv-002-perfis-permissoes. Base local: 00e91a9.
Origem consultada e integrada: origin/developer, 28541bd.

Recebidos CRM, campanhas sazonais, atalhos configur?veis, m?scaras e Casos com tarefas e financeiro. Preservadas Agenda, convites e gest?o administrativa dos escrit?rios.

Cinco conflitos conciliados preservando rotas dos tr?s m?dulos, RouterLink e m?scaras, atalhos, dom?nio Rotina e bloqueio de exclus?o de tipos usados na Agenda. AtalhosRequest em arquivo pr?prio; imports dos reposit?rios corrigidos em CRM/Casos.

## Valida??o
- Build Angular aprovado.
- Frontend: 111 testes em 38 arquivos aprovados.
- Backend: compila??o principal e de testes aprovada. mvn clean test: 155 testes, zero falhas, 147 erros; inicializa??o bloqueada pelo Flyway por vers?o 17 duplicada.
- H? colis?es nas vers?es V17, V18, V19, V20 e V21. Nenhuma migration alterada ou renumerada; conferir flyway_schema_history dos ambientes antes de conciliar.
- Integra??o funcional e inicializa??o da API n?o validadas. Containers n?o atualizados.

## Pr?ximo passo
Primeiro conciliar o hist?rico de migrations de CRM/Casos e da branch atual sem modificar migrations aplicadas, executar novamente testes e validar inicializa??o.
Depois retomar ADV-026-AGENDA02: contatos/clientes e participantes nos compromissos, com isolamento por escrit?rio.
O fechamento mais recente no Notion confirma o aceite da Agenda b?sica; a pend?ncia visual no documento local anterior j? foi encerrada pelo usu?rio.
Sem push nesta sess?o.
