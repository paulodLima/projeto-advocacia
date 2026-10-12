# Gestão administrativa de empresas e contratos

Entrega de 12/10/2026 na branch `feature/adv-002-perfis-permissoes`.

## Referência e decisão

Consultados `docs/index.html`, a funcionalidade PLAT-002 e a última sessão no Notion. A origem é a intranet de um escritório; administração global de contratantes é uma ampliação white label, sem fluxo equivalente comprovado no HTML. O painel atual de primeiro convite não oferecia gestão das empresas já cadastradas. O usuário autorizou suspensão por inadimplência, reativação, encerramento, controle de módulos e histórico, mantendo os dados.

## Operação

Em `/administracao`, abrir **Empresas e contratos**, buscar por nome ou CNPJ, filtrar por situação e clicar em **Gerenciar**. A ficha mostra responsável, email e usuários ativos; permite definir serviço Ativo/Suspenso/Contrato encerrado, financeiro Não informado/Regular/Inadimplente e início do contrato. Escolher módulos, informar motivo e confirmar o impacto antes de salvar. Uma falha preserva o rascunho. Em conflito de versão, recarregar a ficha e revisar as escolhas.

Situação financeira é anotação manual; não gera cobranças nem suspensão automática. Suspensão é reversível. Encerramento registra o fim do contrato e também pode ser revertido explicitamente pelo administrador, com motivo e histórico. Nenhuma dessas medidas remove dados. Reativação não altera situação de usuários ou permissões da equipe. Transferência de responsável, cobrança automática e exclusão definitiva não fazem parte desta entrega.

## Segurança e persistência

- Somente administrador ativo da plataforma acessa as APIs administrativas. O responsável da empresa não recebe esse privilégio.
- A empresa dos usuários vem do vínculo autenticado. A situação é consultada no filtro de segurança em cada pedido, inclusive em sessão aberta. Empresa suspensa/encerrada invalida a sessão e retorna 401 nos endpoints protegidos. A mesma conta precisa entrar novamente após reativação. Empresa B não é afetada por medidas em A.
- Módulos efetivos são a interseção da liberação contratual e das permissões da equipe, inclusive para MASTER. APIs de módulos retirados retornam 403. `/api/equipe/me` permanece consultável para obter as permissões; Início/Configurações são obrigatórios para acesso básico à conta e administração do próprio cadastro. A gestão de equipe não pode elevar a liberação contratual.
- O navegador pode manter a tela já carregada até a próxima navegação/consulta; o servidor bloqueia os novos pedidos. A suspensão não cancela retroativamente transações já iniciadas.
- A migration V23 adiciona contrato e eventos administrativos, sem alterar migrations anteriores. A situação também atualiza `escritorio.ativo`, usado pelos módulos existentes. O contrato usa versão otimista e bloqueio da linha do escritório para evitar alterações simultâneas perdidas. Gravações de contrato, módulos e histórico são atômicas.
- Os eventos registram autor, data, ação, estado anterior/atual e motivo. Alterações de financeiro, início e módulos recebem eventos próprios. A listagem traz somente dados administrativos, sem dar acesso a dados de negócio.
- A migração preserva liberações explícitas anteriores e libera os módulos atuais sem configuração para manter o comportamento existente. Novas empresas começam com o catálogo atual liberado; o administrador pode retirar módulos na ficha. Módulos adicionados depois do cadastro permanecem bloqueados até liberação explícita. Esta política inicial não representa integração automática com planos comerciais.

## API

- `GET /api/plataforma/admin/gestao-empresas?busca=&situacao=&pagina=0`: até 20 empresas por página.
- `GET /api/plataforma/admin/gestao-empresas/{id}`: ficha, catálogo e histórico.
- `PUT /api/plataforma/admin/gestao-empresas/{id}`: `versao`, `situacao`, `financeiro`, `inicio`, `modulos` e `motivo`. Exige sessão administrativa e CSRF; retorna ficha atualizada. Versão desatualizada retorna 409.

## Validação

- Backend: `mvn clean test`, Java 21, 115 testes sem falhas/erros. Inclui 4 testes de gestão administrativa: sessão aberta revogada, MASTER/funcionário bloqueados, outra empresa preservada, usuário inativo mantido após reativação, módulo revogado continua bloqueado, encerramento, histórico, conflito sem alteração parcial, autorização global/CSRF e filtros. Persistência funcional dessa suíte usa H2 em modo PostgreSQL.
- Frontend: 84 testes em 30 arquivos passaram, incluindo confirmação/motivo, gravação após resposta e preservação do rascunho em conflito. Build Angular passou.
- Docker: imagens de API/app reconstruídas e containers locais atualizados. PostgreSQL validou 23 migrations, aplicou V23 sobre V22 e iniciou a API com schema JPA compatível. Consulta de leitura confirmou V23 e dois contratos existentes ativos. Nenhuma empresa real foi suspensa ou encerrada na validação.
- Chrome headless: desktop e celular validaram abertura da ficha, confirmação obrigatória, suspensão, anotação financeira e histórico com APIs simuladas. Sem erro JavaScript ou overflow horizontal em 390px. Capturas em `docs/gestao-empresas-desktop.png` e `docs/gestao-empresas-mobile.png`; roteiro `tmp/validar-gestao-empresas.mjs`.

Entrega em revisão, disponível no ambiente local; aceite com sessão administrativa real pendente. Alterações desta entrega permanecem locais, sem commit/push. Próximo passo: conferir Administração → Empresas e contratos com o administrador real e revisar as liberações contratuais existentes; depois definir cobrança/avisos automáticos e transferência de responsável se necessários.
