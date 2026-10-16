# Integração da developer — 16/10/2026

Branch: feature/adv-002-perfis-permissoes. Base local: 00e91a9.
Origem consultada e integrada: origin/developer, 28541bd. Merge: 23d72ac.

Recebidos CRM, campanhas sazonais, atalhos configuráveis, máscaras e Casos com tarefas e financeiro. Preservadas Agenda, convites e gestão administrativa dos escritórios.

Cinco conflitos conciliados preservando rotas dos três módulos, RouterLink e máscaras, atalhos, domínio Rotina e bloqueio de exclusão de tipos usados na Agenda. AtalhosRequest em arquivo próprio; imports dos repositórios corrigidos em CRM/Casos.

## Validação inicial do merge

Build Angular aprovado e 111 testes frontend aprovados. Backend compilou, mas os testes foram bloqueados pelo Flyway: versões V17–V21 duplicadas. Nenhuma migration foi alterada naquela etapa. Containers ainda não haviam sido atualizados.

## Continuação autorizada

As colisões foram conciliadas após consultar o histórico PostgreSQL local: V1–V24 preservadas e migrations novas do CRM/Casos em V25–V29. A tabela comercial crm_consulta evita a colisão com agenda_evento. Desenvolvida ADV-026-AGENDA02 com V30. Regressão completa: 159 testes backend e 114 frontend aprovados; build e containers atualizados. Detalhes, decisões e evidências em agenda-participantes.md.

O Notion confirma o aceite anterior da Agenda básica. A nova entrega de participantes segue para revisão com sessão real do usuário. Próxima entrega planejada: recorrência. Sem push.
