# Agenda integrada ao CRM e às tarefas dos casos

## Referência, escopo e isolamento

Referência funcional: docs/index.html. Na origem, consultas do CRM criam/atualizam tarefas da Agenda; realizada vira concluída, não compareceu vira cancelada e desativar a consulta remove a tarefa. O calendário usa Mostrar na agenda e datas; tarefas ligadas a casos/processos preservam seu vínculo. Chamadas externas não provam regras de servidor.

Destino conferido: CRM usa crm_consulta; casos usam caso_tarefa e caso_tarefa_responsavel; Processo contém somente package-info e a navegação de módulo futuro. Esta entrega integra as duas fontes funcionais existentes. Não há módulo processual para conectar; sua integração depende da implementação desse cadastro.

A empresa é obtida da sessão em CRM, Casos e Agenda. A projeção mantém empresa e chaves compostas para lead/tarefa/caso. Consultar exige usuário e escritório ativos, módulo Agenda e acesso ao módulo de origem. Tarefas privadas são filtradas no SQL antes da contagem e paginação: somente autor, responsáveis ou master. Perder acesso ao módulo ou delegar uma tarefa privada remove a visibilidade correspondente na próxima consulta. Abrir a origem revalida suas próprias permissões.

## Funcionamento

- Consultas agendadas com data aparecem como Consulta CRM, com o horário civil informado no CRM (Brasília). Marcada/confirmada são pendentes; realizada é concluída; não compareceu é cancelada. Alterar data/situação/nome do lead atualiza o vínculo. Desativar a consulta ou limpar a data remove o vínculo. Excluir lead também o remove.
- Não é inventado um horário final para consultas. O CRM não oferece responsável da consulta; o registro aparece sem responsável e não entra no filtro de uma pessoa específica.
- Tarefas dos casos aparecem quando Mostrar na agenda está ativo. Sem data, aparecem ao incluir tarefas sem data. Preservam período, horário, prazo fatal e responsáveis. Edição, adiamento, delegação, conclusão/reabertura e exclusão atualizam a projeção na mesma transação. Desmarcar Mostrar na agenda oculta o registro sem duplicar a tarefa.
- O calendário soma atividades próprias e vínculos autorizados, respeitando os mesmos filtros. Um período e um prazo fatal no mesmo dia contam uma única ocorrência de cada tarefa. Clicar no dia aplica o mesmo recorte à seção de vínculos.
- Consultas e tarefas vinculadas possuem seção paginada própria, com busca, tipo, situação, responsável e período da Agenda. A paginação é independente da lista de atividades criadas diretamente na Agenda.
- Abrir consulta leva a /crm?lead=UUID. Abrir tarefa leva a /casos?id=UUID&aba=tarefas&tarefa=UUID e abre a tarefa exata após carregar a lista autorizada e as permissões. Uma tarefa indisponível mostra erro sem abrir outra ficha. Editar ou concluir continua usando a API original, suas versões e CSRF.

Esta decisão mantém uma única fonte editável, evitando divergência entre a ficha do caso/lead e uma cópia manual da Agenda. Não se aplica a recorrência automática da Agenda nem a execução de workflows às tarefas dos casos. Os lembretes pessoais atuais continuam vinculados às atividades próprias da Agenda; os registros desta seção não são duplicados na tabela de atividades para produzir avisos.

## Arquitetura e migração

V34 cria agenda_vinculo, projeção normalizada com índices por empresa/data e empresa/prazo, unicidade por fonte e FKs por escritório com exclusão em cascata. V1–V33 não foram alteradas.

As escritas existentes de CRM e tarefas de casos chamam o contrato VinculosAgendaRepository na sua transação, mantendo o bloqueio do escritório. O adaptador converte os dados das fontes e atualiza a projeção. Uma falha na projeção interrompe a transação da origem, sem confirmação parcial.

Tipos antigos removidos do cadastro usam o título de fallback Tarefa quando não há título livre. Na inicialização, InicializarVinculosAgenda recupera projeções ausentes dos registros existentes, em lotes de 100 por fonte, com transação e bloqueio por escritório. A reexecução não duplica vínculos. Não importa dados do HTML nem cria registros de negócio adicionais. Consultas GET não reindexam nem escrevem.

VinculosAgendaService deriva as permissões autenticadas; o contrato e modelos são de domínio sem framework. Controller e resposta HTTP estão em arquivos próprios. GET /api/agenda/vinculos usa Cache-Control no-store. Paginação ocorre no SQL; responsáveis da página são consultados em lote.

## Evidências e continuidade

Branch feature/adv-002-perfis-permissoes, após 8a40cf1.

- Regressão completa do backend: 177 testes aprovados. 29 testes de Agenda aprovados em H2 e no PostgreSQL real, com transações revertidas. Novos cenários verificam reagendamento, ciclo de situação da consulta, remoção, identidade estável, tarefas e adiamento, delegação, exclusão de caso, privacidade, módulos, empresa, filtros, paginação, reindexação repetida e autenticação.
- 133 testes frontend em 42 arquivos aprovados, incluindo links e parâmetros, cancelamento de consultas antigas, falha/retry, abertura exata da tarefa e erro para tarefa indisponível.
- Build Angular aprovado; API e app reconstruídos no Docker; V34 aplicada e schema validado.
- Chrome desktop/celular com APIs simuladas: seção de vínculos, consulta/tarefa, links e abertura da tarefa exata no caso, sem overflow horizontal ou exceções JavaScript.
- Roteiro tmp/validar-agenda-vinculos.mjs; capturas agenda-vinculos-desktop.png, agenda-vinculos-mobile.png e agenda-vinculos-tarefa-mobile.png.

Disponível em http://localhost:4200/agenda para revisão com a sessão real do usuário. A validação visual automatizada usa APIs simuladas; a sincronização real foi verificada nos testes PostgreSQL. Sem atribuir aceite ainda não recebido.

Gestão Processual, Google Agenda, lembretes das fontes vinculadas e canais externos permanecem entregas posteriores. O módulo completo da Agenda continua em migração.
