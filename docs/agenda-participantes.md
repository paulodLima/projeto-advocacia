# Agenda — contatos e participantes

Entrega ADV-026-AGENDA02, em 16/10/2026, na branch feature/adv-002-perfis-permissoes, sobre o merge local 23d72ac.

## Referência e decisões

Consultados docs/index.html (participantes_contato_ids dos formulários de atividades) e a tarefa correspondente no Notion. A origem mantém uma lista de contatos independente dos responsáveis da equipe. A Agenda atual ainda não tinha essa relação.

Implementada uma lista opcional de até 50 contatos existentes por atividade, sem duplicação, para tarefas, prazos, reuniões e audiências. Todos os tipos de Contatos são aceitos, incluindo clientes PF/PJ, partes, advogados, fornecedores e parceiros. Não se cria outro cadastro de cliente nem se copia documento, telefone, email ou demais dados pessoais para a Agenda. Não foi identificado limite numérico comprovado na origem; o limite de 50 é decisão desta implementação.

Os nomes são consultados no cadastro atual; renomear um contato mantém o UUID e atualiza sua identificação na ficha. A exclusão de um contato vinculado retorna conflito: remover os vínculos antes de excluir. A exclusão não apaga silenciosamente participantes do histórico de uma atividade. Responsáveis, datas, situação, versão e eventos da Agenda continuam preservados. As edições de participantes geram o evento EDITADA existente; não há snapshot nominal ou auditoria detalhada de diferenças nesta etapa.

## Acesso e isolamento

O escritório vem exclusivamente do vínculo autenticado. Busca, seleção, leitura e alteração dos vínculos exigem acesso a Agenda e Contatos; a permissão de escrita da Agenda é exigida para salvar. O endpoint retorna somente id, nome e ativo (contatos não possuem situação de inativação atualmente). Sem acesso a Contatos, IDs e nomes são ocultados; alterações nos demais dados da Agenda e transições de situação preservam os vínculos no servidor. Tentativa de enviar participantes sem permissão retorna 403.

Referências entre empresas, inexistentes, duplicadas, nulas ou acima do limite são rejeitadas. Chaves estrangeiras compostas por empresa impedem referências entre escritórios também no banco. Escritas continuam sob bloqueio do escritório, versão e transação, inclusive exclusão de Contatos.

## API e tela

POST/PUT /api/agenda recebem participantes, uma lista de UUIDs; o mesmo campo aparece em dados na ficha e na lista. Vazio remove os vínculos quando o usuário possui acesso a Contatos. O campo omitido é tratado como vazio. Sem permissão, vazio preserva os vínculos existentes.

GET /api/agenda/opcoes inclui podeVincularContatos, válido para consulta; a escrita depende também de podeEditar.

GET /api/agenda/contatos?busca=nome consulta até 50 resultados por nome, sem interpretar % e _ como curingas. O parâmetro ids permite carregar nomes dos selecionados, limitado a 50 UUIDs. A seleção valida todos os UUIDs na mesma empresa. A ficha oferece busca explícita, adicionar, remover, nomes selecionados, estado de busca/erro e limite. Pedidos anteriores são cancelados; falhas não descartam a seleção nem o rascunho.

## Integração da developer e migrations

O histórico do PostgreSQL local confirmou V1–V24 aplicadas, sem CRM/Casos. Essas migrations foram preservadas integralmente. As cinco recebidas, ainda não aplicadas nesse banco, passaram para V25–V29. A tabela de consultas comerciais do CRM passou de agenda_evento para crm_consulta, evitando colisão com o histórico da Agenda. Serviço e testes do CRM usam a nova tabela. V30 cria agenda_participante com índice e chaves por empresa.

Fixtures de CRM/Casos usam EmpresaTestSupport e convite inicial confirmado, conforme a regra atual da plataforma. O código de produção não foi flexibilizado para aceitar autocadastro.

Em qualquer outro ambiente que tenha aplicado as versões antigas de CRM/Casos, conferir flyway_schema_history antes de atualizar: não usar flyway repair nem trocar checksums automaticamente. Nenhum ambiente remoto foi consultado ou alterado nesta sessão.

## Validações

- Regressão backend: 159 testes aprovados, incluindo os 11 de Agenda e verificações de arquitetura. Clean test executado; uma fixture nova foi corrigida e a regressão completa passou novamente.
- Frontend: 114 testes em 38 arquivos aprovados, incluindo seleção, remoção, leitura, permissões, falha e conflito; build Angular aprovado.
- PostgreSQL real: os 11 testes de Agenda passaram, com transações revertidas e sem envio de emails.
- Docker: API/app reconstruídos; PostgreSQL validou 30 migrations e aplicou V25–V30 sobre V24, sem alteração do histórico anterior.
- Chrome desktop/celular com APIs simuladas: buscar dois contatos, selecionar, salvar, reabrir ficha, recuperar nomes, preservar participantes em conflito e conclusão; regressão de edição, cancelamento, reabertura e perfil financeiro aprovada. Sem exceções JavaScript ou overflow horizontal.
- Roteiro: tmp/validar-agenda-participantes.mjs. Capturas: docs/agenda-participantes-form-desktop.png, agenda-participantes-desktop.png, agenda-participantes-mobile.png e agenda-participantes-ficha-mobile.png.

## Continuidade

Disponível localmente em http://localhost:4200/agenda. A confirmação visual do usuário com sua sessão real fica para revisão; os cenários de navegador usam dados simulados. Sem push nesta sessão. Próxima entrega do planejamento: recorrência, seguida por lembretes/notificações e workflows. A integração automática das consultas do CRM com a Agenda, casos/processos e Google Agenda permanece para entregas próprias. A tabela crm_consulta ainda não cria uma agenda_atividade.
