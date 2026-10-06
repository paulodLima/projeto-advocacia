# Fundação white label — primeira entrega da ADV-002

> Registro histórico do primeiro protótipo. O escopo foi revisado: usuários comuns não criam empresas e não existe autocadastro público. A etapa 1 atual está em [Administração da plataforma](etapa1-administracao-plataforma.md). As referências abaixo à criação de empresa pelo usuário descrevem o protótipo anterior, cujo acesso público foi removido.

## Referência funcional

A análise de docs/index.html identificou perfil_permissao, usuario_permissao
(concedida ou negada), modulo_permissao e abas_permitidas. Na origem,
editar ou gerir também permite ler; abas restringem a navegação.
Os perfis e suas permissões vêm do servidor: o HTML não contém a matriz completa.
Essa matriz ainda precisa ser definida e validada antes da conclusão da ADV-002.

## Decisões desta entrega

- Usuario representa a identidade global autenticada. Não recebe perfil global de administrador.
- Escritorio representa o contexto de isolamento dos futuros módulos.
- Escritorio_membro vincula uma identidade ao escritório, com perfil e estado ativo.
- PROPRIETARIO identifica o criador do escritório; MEMBRO é reservado para os próximos fluxos.
  Esses valores ainda não constituem a matriz de permissões por operação.
- Criar um escritório cria o vínculo de proprietário na mesma transação.
- Contas existentes não são vinculadas automaticamente. Não há migração implícita de dados para um escritório.
- A seleção é guardada na sessão, após conferir o vínculo no banco.
  A consulta do contexto revalida o vínculo e remove a seleção quando ele for revogado.
- Um usuário pode criar escritórios e pertencer a vários, com perfis distintos.
- Administração global de contas via /api/usuarios foi bloqueada; GET da própria conta continua disponível.
  A futura administração de membros não deve alterar o nome, email ou status global de outro usuário.

## API implementada

| Método | Caminho | Comportamento |
| --- | --- | --- |
| GET | /api/escritorios | Lista apenas vínculos ativos do usuário |
| POST | /api/escritorios | Recebe nome e cria escritório e proprietário |
| PUT | /api/escritorios/{id}/contexto | Seleciona somente escritório com vínculo ativo |
| GET | /api/escritorios/contexto | Retorna seleção após revalidar acesso |

As operações exigem autenticação e as escritas exigem CSRF.
Nenhum módulo futuro deve usar o ID do escritório recebido como prova de autorização.
Deverá validar acesso e incluir escritorio_id nas consultas e nas relações entre dados.

## Pendências

- Validação visual da tela de criação e seleção do escritório.
- Administração e convites de membros, sem expor contas de outros escritórios.
- Matriz de perfis, permissões individuais e abas, com bloqueios no backend.
- Identidade visual persistida e carregada por escritório.
- Validação da migração no PostgreSQL e validação visual do fluxo completo.

A ADV-002 permanece em migração. Esta entrega fornece a base de contexto,
não conclui a reprodução das permissões da origem.

## Frontend

A rota /escritorios lista os vínculos, permite criar um escritório e selecionar
o contexto. Sem contexto válido, as outras rotas autenticadas direcionam para
essa tela. O cabeçalho permite trocar o escritório; o logout limpa o contexto
local. O backend continua responsável por validar acesso: o guard serve para
guiar a navegação e não substitui autorização na API.

## Validação desta sessão

- Backend: 23 testes passaram, incluindo quatro cenários novos de escritórios.
- Frontend: compilação de produção passou e 10 testes passaram, incluindo três cenários de guard.
- Git diff --check passou.
- A migração V5 foi aplicada nos testes H2 em modo PostgreSQL.
- PostgreSQL real e navegação visual ainda não foram validados; Docker Desktop não estava em execução.
