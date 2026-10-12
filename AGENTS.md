# Orientações do projeto

## Referência funcional e arquitetura white label

- Antes de desenvolver ou alterar qualquer funcionalidade, consultar `docs/index.html`, que contém a intranet de origem. Identificar o comportamento existente, regras, operações, vínculos e dependências relevantes; comparar com o código atual antes de implementar.
- Preservar os comportamentos necessários da origem, organizando a implementação para manutenção e escala. O HTML é referência funcional; chamadas externas não comprovam regras ou funcionamento do servidor. Registrar lacunas e decisões nas tarefas.
- O destino é uma plataforma white label para diferentes escritórios. Considerar identidade visual configurável por escritório, isolamento de dados e permissões no contexto do escritório ao definir entidades, APIs e fluxos.
- O administrador da plataforma envia o primeiro convite ao responsável pelo escritório. Após confirmar o email, o convidado completa seu cadastro, cadastra o escritório e administra os funcionários e seus convites no contexto da própria empresa. Não oferecer autocadastro sem convite nem conceder administração da plataforma ao responsável. A liberação de funcionalidades por empresa continua sendo atribuição da plataforma. A personalização visual pertence ao usuário autorizado da empresa.
- Antes de adicionar módulos de negócio, definir explicitamente como o escritório é identificado e como seus dados são isolados. Não aceitar um identificador enviado pelo cliente como autorização de acesso.
- Consultar o planejamento do Projeto Advocacia no Notion e conferir o estado do código. Ao finalizar sessões de desenvolvimento, registrar alterações, validações, pendências, branch e próximo passo; atualizar estados somente com evidências. Não marcar implementações sem validação como concluídas.

## Organização do backend

- Manter organização por funcionalidade. Controllers ficam em `api`; records HTTP em arquivos próprios em `api/request` e `api/response`. Não declarar DTOs dentro do controller nem acessar repositories, JPA ou SQL nele.
- Serviços/casos de uso em `application` coordenam regras e transações. Nos módulos `escritorio` e `plataforma`, dependem dos contratos de repositório do domínio, sem importar infraestrutura.
- Modelos e regras independentes de framework em `domain`; contratos em `domain/repository`. Comandos de aplicação em `application/command`.
- Entidades JPA em `infrastructure/persistence/entity`; interfaces Spring Data e adaptadores em `infrastructure/persistence/repository`. Persistência e conversão de entidade/domínio pertencem aos adaptadores.
- Não modificar migrations aplicadas durante refatorações. Ao mover pacotes, validar com `mvn clean test` para evitar classes antigas em `target`.
