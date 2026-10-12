# Convite inicial e cadastro do escritório

Decisão de produto de 09/10/2026. Branch: `feature/adv-002-perfis-permissoes`.

## Fluxo implementado

1. O administrador da plataforma acessa `/administracao` e envia o primeiro convite com nome e email do responsável.
2. O convidado confirma o email por código de acesso, completa seu nome em `/cadastro` e segue para Configurações → Empresa.
3. O responsável cadastra o escritório. O servidor consome o convite e vincula a conta como `MASTER` da empresa criada.
4. Em Configurações → Equipe, o responsável cadastra/gerencia funcionários e envia convites. Funcionários novos confirmam o email e completam o próprio cadastro antes de entrar no escritório.

Não há autocadastro aberto. O email do cadastro vem da sessão verificada; o cliente não escolhe empresa nem privilégio global. A autorização da empresa usa o vínculo do usuário autenticado. Funcionários não administram a plataforma nem a equipe. Convites iniciais têm validade de sete dias, reenvio e cancelamento antes da conclusão; convites contraditórios de responsável e funcionário são rejeitados. Falha no envio do convite desfaz sua persistência.

## Referência e integração

Consultados `docs/index.html` e planejamento do Projeto Advocacia no Notion. A configuração do escritório e a equipe recebidas da developer foram reaproveitadas; o convite inicial é a decisão white label autorizada nesta sessão.

O merge trouxe erros de compilação, modelos paralelos de escritório/empresa e migrations com versões duplicadas. Foram corrigidos imports, composição de serviços e pacotes conforme AGENTS.md. DTOs HTTP foram extraídos dos controllers; entidades e adaptadores receberam seus pacotes próprios.

Migrations já aplicadas V1–V9 foram preservadas. Scripts ainda não aplicados recebidos da developer foram renumerados V10–V21. V22 integra os escritórios existentes ao modelo de empresa e adiciona os convites iniciais. Os vínculos históricos permanecem preservados; no modelo atual, cada conta usa uma empresa, preferindo o vínculo de proprietário ao integrar dados anteriores.

## Evidências de validação

- Backend: `mvn clean test`, Java 21, 111 testes, sem falhas ou erros.
- Frontend: 82 testes em 29 arquivos, sem falhas; build Angular e imagem Docker passaram.
- PostgreSQL local: validação de 22 migrations e inicialização da API com schema JPA validado, preservando migrations anteriores.
- Chrome local, APIs reais e Mailpit: primeiro convite, confirmação por código, cadastro pessoal, criação do escritório como MASTER, convite/cadastro de funcionário, mesmo escritório e entrada em `/inicio` passaram.
- O navegador confirmou redirecionamento do responsável sem escritório para Configurações → Empresa e carregamento da administração e da equipe. As mutações desse roteiro foram realizadas por chamadas reais à API dentro do navegador; testes de componentes cobrem os formulários.
- API negou administração global para responsável e funcionário, administração de equipe pelo funcionário e criação de segundo escritório pelo responsável.
- Testes cobrem ausência, cancelamento e expiração de convite, reenvio, falha SMTP, autenticação Google verificada e restrições de empresa. OAuth Google não foi exercitado com uma conta externa real.

Para o roteiro local, SMTP foi temporariamente direcionado ao Mailpit e depois restaurado sem alterar `.env`. Foram criados registros fictícios identificáveis: `responsavel.1791558880214@example.test`, `funcionario.1791558880214@example.test` e empresa `2f09393b-2566-49d7-8c26-d4897e9675a5`. Nenhum email de validação foi enviado pelo Gmail.

## Pendências e próximo passo

Entrega em revisão para aceite do usuário. Alterações desta sessão permanecem locais, sem commit ou push. Merge registrado em `5d3934b` e correção inicial em `b7adfec`.

O trabalho local anterior está preservado em `stash@{0}`, `backup-local-antes-merge-developer-2026-10-09`. A conciliação do catálogo/liberação de módulos desse stash é uma tarefa posterior; permissões de equipe não comprovam liberação de módulos por empresa.

Limites atuais: uma empresa por conta; transferência de responsável e fluxo de recuperação administrativa ainda não implementados. Sugestão para próxima etapa: definir transferência de responsabilidade com auditoria para evitar dependência permanente de uma única conta.
