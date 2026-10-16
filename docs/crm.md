# CRM

Implementação na branch `feature/crm`, baseada nas funções `D7`, `S7`, `U7`, `ug` e `F7` e nos estilos de Leads de `docs/index.html`.

## Telas e funcionamento

- **Leads:** indicadores, busca, filtros de temperatura/origem/situação, retornos vencidos e cartões com histórico lateral. Cadastro em painel à direita; edição em formulário.
- **Cadência:** registrar uma tentativa avança o passo, atualiza o último contato e limpa o retorno combinado. “Quer agendar consultoria” altera a situação para “Agendou consultoria”; “Sem interesse” marca perdido. A pausa permite escolher uma data de retorno, inclusive ficar sem data. Ao esgotar a régua, o lead permanece ativo para decisão.
- **Consulta:** agendamento, data/hora e situações marcada, confirmada, realizada e não compareceu. A consulta é persistida em `agenda_evento`; sua identidade é preservada nas alterações.
- **F.A.C.A.:** Fit, Assunto, Capacidade e Autoridade alternam entre não avaliado, sim e não. As quatro respostas positivas indicam qualificado.
- **Comentários:** histórico interno e menções selecionadas ao digitar `@`. Membros ativos da mesma empresa com acesso ao CRM recebem uma notificação no painel do cabeçalho.
- **Ganho:** pode apenas marcar ganho ou criar cliente e caso. A conversão preserva o histórico anterior e impede criar outro caso para o mesmo lead. Clientes existentes são procurados por email e telefone normalizados; correspondências ambíguas interrompem a operação inteira.
- **Marketing Sazonal:** cadastro de campanhas com mês, cor, descrição e etiquetas; cartões de destinatários e progresso de abordagem anual. Marcar alguém registra o contato realizado; não envia mensagens automaticamente.

As etiquetas são compartilhadas com Configurações e ligadas aos indicadores de Contatos por UUID. V17 aproveita os indicadores existentes, preservando seus vínculos. Renomear uma etiqueta mantém os clientes nas campanhas. A mesma pessoa pode estar em várias campanhas; leads perdidos não entram nos grupos. Destinatários de Contatos só são mostrados a quem tem acesso a esse módulo.

O HTML de referência carrega a régua de uma base externa e não traz seus valores. A régua inicial adotada aqui tem quatro passos: primeiro contato no dia da entrada; primeiro retorno após 2 dias; segundo após 3 dias; último após 7 dias. Cada intervalo conta da entrada ou do último contato, conforme a referência. O master pode substituir a régua pelo endpoint `PUT /api/crm/cadencia`. A data de referência é a do fuso `America/Sao_Paulo`.

## Persistência e acesso

A migração V17 cria as tabelas do CRM, histórico, comentários, menções, notificações, campanhas, etiquetas e abordagens anuais. A empresa é obtida da sessão, nunca do formulário. Membros ativos com acesso ao CRM e perfil ADMINISTRADOR, ADVOGADO ou ASSISTENTE podem editar; FINANCEIRO com permissão específica pode consultar. Criar cliente/caso exige também acesso a Contatos e Casos. Apenas o master altera a régua.

Escritas exigem sessão e CSRF, usam transação e verificam a versão conhecida. Uma alteração simultânea retorna 409 e pede recarregamento. Listagem de leads é paginada, com no máximo 100 itens por página. IDs de outras empresas retornam 404. Etiquetas utilizadas não podem ser excluídas sem remover os vínculos.

Clientes criados pela conversão podem não ter CPF ou email. Nessa situação ficam marcados com `cadastro_incompleto`, mostrado na ficha de Contatos. A edição normal continua exigindo o preenchimento dos campos obrigatórios e remove a marca quando o cadastro é completado.

## Integrações preparadas

Os módulos completos de **Casos**, **Agenda** e **Gestão Processual** ainda não foram implementados. Esta etapa cria as bases `caso` e `agenda_evento` para persistir a conversão e a consulta do CRM. Elas ainda não aparecem em telas completas desses módulos.

A área **Manutenção de planejamento** mantém a apresentação e o estado vazio da referência. A base `crm_manutencao` e o registro de contato estão preparados, mas as datas de entrega e intervalos serão definidos na futura ficha de Casos/Gestão Processual. Não há campanhas por processos sem esse módulo.

## Endpoints

| Método | Rota | Uso |
| --- | --- | --- |
| GET | `/api/crm/opcoes` | Permissões, etiquetas, membros e cadência |
| GET | `/api/crm/leads` | Busca, filtros, paginação e indicadores |
| GET | `/api/crm/leads/{id}` | Lead, tratativas e comentários |
| POST / PUT | `/api/crm/leads` / `/api/crm/leads/{id}` | Cadastro e edição |
| PUT | `/api/crm/leads/{id}/estado` | Consulta, retorno, situação e F.A.C.A. |
| POST | `/api/crm/leads/{id}/contatos` | Registrar tentativa e avançar cadência |
| POST | `/api/crm/leads/{id}/comentarios` | Comentário e menções |
| POST | `/api/crm/leads/{id}/converter` | Ganho e conversão opcional |
| DELETE | `/api/crm/leads/{id}?versao=N` | Excluir lead sem caso vinculado |
| PUT | `/api/crm/cadencia` | Configuração da régua pelo master |
| GET / POST | `/api/crm/campanhas?ano=2026` / `/api/crm/campanhas` | Consultar e criar campanhas |
| PUT / DELETE | `/api/crm/campanhas/{id}` | Atualizar/excluir com versão |
| PUT | `/api/crm/campanhas/{id}/abordagens` | Marcar/desmarcar destinatário no ano atual |
| POST | `/api/crm/manutencoes/{id}/contato` | Registrar contato de manutenção |
| GET | `/api/crm/notificacoes` | Menções pendentes do usuário |
| PUT | `/api/crm/notificacoes/{id}/lida` | Marcar notificação própria como lida |

Atualize os containers pela raiz: `docker compose -f docker/compose.yaml up --build -d`. O Flyway aplica V17 ao iniciar a API.
