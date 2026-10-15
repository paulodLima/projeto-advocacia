# Casos — cadastro, tarefas e financeiro

Implementação inicial na branch `featura/casos`, usando o módulo de Casos de `docs/index.html` como referência. A tela usa linhas de casos, busca, filtros de situação, formulário em painel lateral, ficha com cabeçalho na cor do tema e abas. Os dados são persistidos no PostgreSQL; não há importação de dados do protótipo.

## Disponível

- Cadastro e edição: título, cliente existente ou novo cliente, área, responsável, partes adversas, observações, valor apresentado e data da proposta.
- Título sugerido a partir do cliente e das partes adversas, sem substituir um título personalizado.
- Busca literal por título ou cliente, filtros Ativos/Convertidos/Encerrados/Todos e paginação.
- Desfecho comercial: em aberto, fechado, perdido, cliente desistiu ou sem resposta. Fechado permite registrar valor e atribui a data do fechamento. Outros desfechos removem o valor/data de fechamento e permitem motivo; em aberto também limpa o motivo. O desfecho comercial não encerra automaticamente o caso.
- Histórico de reuniões/anotações com data e autor. Textos simples, sem editor de HTML ou menções nesta etapa.
- Encerramento e reativação. Casos encerrados podem ser consultados; reative antes de editar.
- Exclusão de casos manuais com confirmação. Casos vinculados ao CRM não podem ser excluídos; podem ser encerrados para preservar o vínculo.
- Casos criados pela conversão de leads aparecem na listagem. A ficha conserva o snapshot de tratativas e comentários do CRM. O botão **Abrir caso** está disponível no lead convertido para quem tem permissão de conversão.

Novo cliente criado nesta tela é PF e pode começar com cadastro incompleto. CPF e telefone são opcionais, mas precisam ter a quantidade correta de dígitos quando preenchidos. São exibidos com máscara no formulário e salvos sem máscara. Complete o cadastro em Contatos. CPF duplicado na mesma empresa impede a criação e orienta selecionar o contato existente. Cliente e caso são criados na mesma transação.

Áreas vêm de Configurações → Cadastros e sistemas → Grupos. Responsáveis são membros ativos com acesso a Casos. Áreas já vinculadas podem ser mantidas após serem desativadas.

## API e segurança

A empresa é obtida pela sessão. MASTER ou membros ativos com acesso a Casos e perfil ADMINISTRADOR, ADVOGADO ou ASSISTENTE podem editar. FINANCEIRO com acesso a Casos pode consultar. Criar cliente também exige acesso a Contatos. O snapshot do CRM é retornado somente para quem tem acesso ao CRM.

| Método | Endpoint | Uso |
| --- | --- | --- |
| GET | `/api/casos/opcoes` | Permissões, contatos, áreas e responsáveis |
| GET | `/api/casos?busca=&status=ativo&pagina=0&tamanho=24` | Lista paginada |
| GET | `/api/casos/{id}` | Caso, anotações e snapshot do CRM |
| POST | `/api/casos` | Cadastro |
| PUT | `/api/casos/{id}` | Edição |
| PUT | `/api/casos/{id}/comercial` | Desfecho, valor fechado e motivo |
| PUT | `/api/casos/{id}/situacao` | Encerrar ou reativar |
| POST | `/api/casos/{id}/anotacoes` | Registrar reunião/anotação |
| DELETE | `/api/casos/{id}?versao=N` | Excluir caso manual |

Atualizações e anotações exigem a versão atual; conflitos retornam 409. Escritas exigem CSRF e são transacionais. As respostas usam `Cache-Control: no-store`. CPF não tem validação de dígitos verificadores nesta etapa, seguindo o comportamento de Contatos. Valores têm até duas casas decimais; o frontend usa formato brasileiro, como `1.234,56`.

A migração **V19** amplia a tabela `caso` criada no CRM e cria `caso_anotacao`, preservando os casos e snapshots anteriores. Para atualizar o ambiente local, execute na raiz:

```sh
docker compose -f docker/compose.yaml up --build --no-deps -d api app
```

## Próximas etapas

Editor de anotações com menções e transformação em processo ainda não estão implementados. No Financeiro, emissão pelo ASAAS e documentos em PDF ficam para outra etapa. Divisão e repasses dependem das parcerias do processo, seguindo o protótipo. O filtro Convertidos mantém a referência visual para a futura integração com Gestão Processual; esta etapa não cria casos com essa situação.

## Tarefas do caso

A aba **Tarefas** segue a referência `docs/index.html`: lista de pendentes e concluídas, ponto de situação, prazo fatal/data e responsáveis. Clique na linha para abrir a ficha em painel lateral de 680 pixels. O painel permite criar, editar, realizar, reabrir, adiar, delegar, comentar e excluir com confirmação.

O formulário aceita título livre ou um tipo ativo de Configurações → Cadastros e sistemas → Tarefas; início/fim, horários, prazo fatal, vários responsáveis, local, descrição, dia inteiro, preferência de agenda, repetição e os marcadores Importante/Urgente/Recorrente/Privada. Responsáveis devem ser membros ativos com acesso a Casos. Datas invertidas e horários finais inválidos são rejeitados. O adiamento muda a data e o prazo fatal, quando houver, e desloca o término para preservar a duração do período.

Realizar abre a pergunta **Qual providência foi tomada?**. O texto é opcional e fica nos comentários. Data e autor da conclusão são persistidos. Reabrir remove os metadados da conclusão atual e conserva os comentários. Descrição, comentários e providência são textos simples nesta etapa, sem editor de HTML ou menções. A repetição segue o comportamento do protótipo: fica registrada e ainda não gera ocorrências automaticamente. A preferência de agenda também fica salva para a futura integração; ainda não cria eventos na Agenda. Processo e Categoria mantêm sua posição no formulário, desabilitados e com indicação de que os módulos/cadastros correspondentes ainda não estão disponíveis. Não há cálculo automático de prazos processuais nesta etapa.

Tarefas privadas são visíveis ao criador, responsáveis e master, com a restrição aplicada também na API. Escrita utiliza as mesmas permissões de Casos e exige caso ativo. Encerrados podem ser consultados. IDs de outro caso/empresa ou de tarefa privada sem acesso retornam 404. Cada tarefa tem sua própria versão: alterações simultâneas retornam 409. Escritas exigem CSRF e são transacionais; respostas usam `Cache-Control: no-store`.

A migração **V20** cria `caso_tarefa`, `caso_tarefa_responsavel` e `caso_tarefa_comentario`. Excluir uma tarefa remove seus responsáveis e comentários; excluir um caso manual remove suas tarefas. Nenhum dado antigo é importado do navegador.

| Método | Endpoint | Uso |
| --- | --- | --- |
| GET | `/api/casos/{caso}/tarefas/opcoes` | Permissões, tipos e membros |
| GET | `/api/casos/{caso}/tarefas` | Listar tarefas visíveis |
| GET | `/api/casos/{caso}/tarefas/{id}` | Ficha e comentários |
| POST | `/api/casos/{caso}/tarefas` | Criar com `{ versao: null, dados: {...} }` |
| PUT | `/api/casos/{caso}/tarefas/{id}` | Editar dados e versão |
| PUT | `/api/casos/{caso}/tarefas/{id}/estado` | Concluir/reabrir; versão, status e providência |
| PUT | `/api/casos/{caso}/tarefas/{id}/adiamento` | Versão e nova data |
| PUT | `/api/casos/{caso}/tarefas/{id}/responsaveis` | Versão e IDs dos responsáveis |
| POST | `/api/casos/{caso}/tarefas/{id}/comentarios` | Versão e texto |
| DELETE | `/api/casos/{caso}/tarefas/{id}?versao=N` | Excluir versão conhecida |

## Financeiro do caso

A aba Financeiro utiliza os quatro indicadores da referência: Honorários combinados, Já recebido, A receber e Despesas a reembolsar. Honorários e lançamentos seguem os blocos e cartões de parcelas do `docs/index.html`; as despesas podem ser cadastradas na seção complementar, utilizando as operações de despesas presentes no protótipo.

- Honorários Contratuais, Êxito e Sucumbência: valor fixo ou percentual; base × percentual é calculado no servidor com decimal e arredondamento a centavos. Percentual sem base registra a expectativa, sem inventar um valor nem gerar cobrança.
- Lançamentos: Nova entrada/Nova saída, cobrança rápida, descrição, valor total, modalidade, até 60 parcelas, primeiro vencimento, categoria, contato e observações. Categorias vêm de Configurações → Financeiro e respeitam a direção. Contatos devem pertencer à empresa.
- Parcelamento: divisão exata em centavos, diferença na primeira parcela e vencimentos mensais a partir da data inicial. Um vencimento em 31/01 passa a 28/02 (ou 29/02), voltando a 31/03. O total não muda. Todas as parcelas são inseridas na mesma transação.
- Baixa manual e estorno, alteração do vencimento e titularidade (escritório/cliente). Uma parcela paga deve ser estornada antes de excluir ou mudar o vencimento. O resumo de recebimentos considera somente entradas: pagamentos de saídas não aumentam Já recebido/A receber.
- Despesas: descrição, valor, data e condição reembolsável; marcar reembolsada ou estornar reembolso atualiza o total pendente. Despesas e parcelas são registros distintos: cadastrar a despesa não cria uma cobrança automaticamente.

A API exige usuário ativo com acesso a **Casos e Financeiro**. O acesso explícito a Financeiro autoriza as operações financeiras; não concede edição dos dados gerais do caso. Casos encerrados podem ser consultados; reative antes de alterar o financeiro. Não há importação do protótipo. O financeiro possui versão própria compartilhada por honorários, parcelas e despesas. Uma alteração concorrente retorna 409, preservando o rascunho no frontend. Escritas exigem CSRF e usam transações; consultas usam `Cache-Control: no-store`.

Cada operação registra autor, identificadores e data em `caso_financeiro_evento`. Casos com histórico financeiro devem ser encerrados e não podem ser excluídos, mesmo após remover seus lançamentos, para preservar a auditoria. A migração **V21** cria as tabelas do financeiro de Casos e preserva dados de configurações já existentes.

**Limites desta etapa:** lançamentos e baixas são registros internos. Não há emissão ou envio real de boleto/PIX/cartão, integração com ASAAS, assinatura automática, alteração de saldos das contas em Configurações, nota fiscal, recibo ou fatura em PDF. A modalidade fica registrada; escolher Assinatura recorrente cria as parcelas informadas e não uma assinatura em gateway. Os botões ASAAS/Fatura em PDF ficam desabilitados com indicação. Conforme a referência, divisão de honorários e repasses dependem de parceiros vinculados ao processo; não são adicionados artificialmente aos casos.

| Método | Endpoint | Uso |
| --- | --- | --- |
| GET | `/api/casos/{caso}/financeiro` | Versão, honorários, parcelas, despesas e resumo |
| GET | `/api/casos/{caso}/financeiro/opcoes` | Permissão de edição, categorias ativas e contatos |
| POST | `/api/casos/{caso}/financeiro/honorarios` | Tipo, valor/percentual/base, observações e versão |
| DELETE | `/api/casos/{caso}/financeiro/honorarios/{id}?versao=N` | Excluir honorário |
| POST | `/api/casos/{caso}/financeiro/lancamentos` | Criar grupo de parcelas |
| PUT | `/api/casos/{caso}/financeiro/lancamentos/{id}/baixa` | Versão, pago e data da baixa |
| PUT | `/api/casos/{caso}/financeiro/lancamentos/{id}/vencimento` | Versão e nova data |
| PUT | `/api/casos/{caso}/financeiro/lancamentos/{id}/titularidade` | Versão e cliente/escritorio |
| DELETE | `/api/casos/{caso}/financeiro/lancamentos/{id}?versao=N` | Excluir parcela não paga |
| POST | `/api/casos/{caso}/financeiro/despesas` | Versão, descrição, valor, data e reembolsavel |
| PUT | `/api/casos/{caso}/financeiro/despesas/{id}/reembolso` | Versão e reembolsada |
| DELETE | `/api/casos/{caso}/financeiro/despesas/{id}?versao=N` | Excluir despesa não reembolsada |
