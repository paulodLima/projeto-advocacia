# Agenda — recorrência de tarefas

Implementação em 16/10/2026 na branch feature/adv-002-perfis-permissoes, sobre 0c5e3f1.

## Referência e decisão funcional

Consultados docs/index.html e ADV-026 no Notion. O formulário de origem oferece nunca, diária, semanal, mensal e anual somente para tarefas. A origem informa explicitamente que apenas registra a frequência e ainda não gera próximas ocorrências. A geração implementada no destino é uma decisão nova desta entrega, registrada no planejamento; não foi inferida de chamadas externas.

A tarefa recorrente exige data inicial. A primeira conclusão cria exatamente uma próxima tarefa pendente; nenhuma série futura é criada de uma vez. A próxima aparece na lista e no calendário da sua data, que pode estar fora do filtro atual. Cada ocorrência tem UUID, versão, situação e histórico próprios. Não há tarefa em segundo plano nem dependência de a aplicação permanecer aberta.

A programação segue datas civis em America/Sao_Paulo, incluindo finais de semana e feriados. Não é cálculo jurídico de prazo. Atrasos na conclusão não mudam a data programada: a próxima pode nascer atrasada se a anterior for concluída muito depois da data planejada.

## Datas e edição

A próxima data é calculada a partir da data-base e do índice de ocorrência. Mensal: 31/01 → 28/02 → 31/03; anual: 29/02/2028 → 28/02/2029, voltando a 29/02 no próximo ano bissexto. Horários e duração da tarefa são preservados. Data final e prazo fatal são deslocados pelo mesmo número de dias da data inicial; não representam cálculos de dias úteis.

Alterar a data inicial ou frequência da ocorrência pendente reinicia a base a partir dela. Alterar título, responsáveis, participantes ou demais campos preserva a base. As mudanças afetam essa ocorrência e futuras geradas por ela, sem modificar tarefas já existentes. Prazos, reuniões e audiências não aceitam repetição.

Cancelar não gera próxima tarefa. Reabrir não exclui a próxima já existente; concluir novamente não gera duplicata. Cancelar uma ocorrência também não cancela outras já geradas. Para encerrar a repetição, selecionar Não se repete na ocorrência pendente e salvar. Sem data, uma tarefa não pode ser recorrente.

## Segurança, referências e transação

O escritório é obtido pelo vínculo autenticado, com as mesmas regras de Agenda e de escrita existentes. A sucessora usa as referências internas da atividade autorizada; sem acesso a Contatos, os participantes permanecem preservados no servidor e ocultos na resposta. Não há oportunidade para escolher escritório, tarefa anterior, data-base ou índice pelo cliente.

Responsáveis e tipo de tarefa devem continuar ativos para gerar a próxima. Caso contrário, a conclusão retorna conflito e não grava nada: corrigir a ficha ou desativar a repetição antes de concluir. Sem recorrência, a conclusão continua permitindo referências históricas inativas.

Conclusão, sucessora, vínculos e eventos são uma transação, sob bloqueio do escritório e versão. Há unicidade no banco por empresa e atividade anterior para impedir sucessoras duplicadas. O histórico registra CONCLUIDA e RECORRENCIA_GERADA na original e CRIADA na próxima, com a data programada. A próxima não herda os eventos antigos.

## Arquitetura e migration

RepeticaoAgenda e CicloRecorrencia calculam datas no domínio, sem Spring/JPA. AgendaService coordena transação, permissão e geração. AgendaRepository define contratos; JdbcAgendaRepository persiste estado e vínculo. O controller mantém DTOs HTTP em arquivos próprios.

V31 adiciona frequência, data-base, data da última programação, índice e anterior_id; chaves compostas por escritório, frequência/tipo e índice válidos e unicidade. Migrations V1–V30 não foram alteradas. A coluna de última programação permite distinguir mudança de data de uma simples mudança de situação e preservar a âncora.

## API e interface

POST/PUT /api/agenda recebem repeticao: NUNCA, DIARIA, SEMANAL, MENSAL ou ANUAL. O campo aparece em dados na resposta. Omitido, equivale a NUNCA. A conclusão usa o PATCH de situação existente; não há endpoint para inserir IDs de série. Responsáveis e participantes mantêm seus limites atuais.

O formulário oferece frequência somente para tarefas, exige data quando há repetição e explica que a próxima será criada ao concluir. Trocar o tipo desativa a repetição no formulário. A confirmação de conclusão avisa sobre a geração. O rascunho permanece preservado em falha/conflito.

## Validação e continuidade

- Backend: 164 testes passaram na regressão completa; clean test executado na implementação, com correção posterior de uma fixture HTTP e nova execução completa aprovada. Inclui 16 testes de Agenda e verificações de arquitetura.
- PostgreSQL real: os 16 testes de Agenda passaram usando transações revertidas, sem envio de emails. Validados fim de mês/ano bissexto, preservação de vínculos sem acesso a Contatos, datas relativas, cancelamento, encerramento de repetição, reabertura sem duplicação e referência inativa sem gravação parcial.
- Frontend: 116 testes em 38 arquivos aprovados; build Angular aprovado.
- Docker: API e app reconstruídos e iniciados. Flyway validou 31 migrations; V31 aplicada no PostgreSQL local.
- Chrome desktop/celular com APIs simuladas: configuração semanal, persistência e geração simulada da próxima tarefa; regressão de participantes, conflitos, edição, conclusão/reabertura/cancelamento e perfil de consulta. Sem exceções JavaScript ou overflow horizontal. A geração real foi verificada nos testes PostgreSQL.
- Roteiro: tmp/validar-agenda-recorrencia.mjs. Capturas: docs/agenda-recorrencia-form-desktop.png, agenda-recorrencia-desktop.png, agenda-recorrencia-mobile.png e agenda-recorrencia-ficha-mobile.png.

Disponível para revisão em http://localhost:4200/agenda com a sessão real do usuário. Sem push nesta sessão. A validação visual automatizada utiliza dados simulados; não foi atribuída ao usuário uma confirmação ainda não recebida.

Próxima entrega planejada: lembretes/notificações, seguida de workflows e integrações. A Agenda completa segue em migração; recorrência não implementa Google Agenda, integração automática de consultas CRM, casos/processos ou cálculo jurídico de prazos.
