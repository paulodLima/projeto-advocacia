# Gestão de Advocacia

Projeto com backend Spring Boot (Java 21), frontend Angular com SSR e PostgreSQL 17.

## Fundação white label

Após autenticar, o usuário sem escritório ativo é direcionado para
`/escritorios`, onde pode selecionar um vínculo existente. A criação pública de empresas está bloqueada.
Contas existentes não recebem vínculos
automaticamente. A seleção é validada no backend e pode ser trocada pelo cabeçalho.

As operações globais de criação, alteração e exclusão em `/api/usuarios` estão
bloqueadas nesta etapa. A consulta da própria conta permanece disponível.
A administração de membros por escritório e a matriz completa de permissões
ainda estão em desenvolvimento. Consulte as decisões, APIs e pendências em
[Fundação white label](docs/white-label-fundacao.md).

A estrutura do backend, os perfis de ambiente e o CRUD de usuário estão documentados no [README do backend](advocacia-microservice/README.md).

## Pré-requisitos

- Git para clonar o repositório.
- Docker e Docker Compose v2. No Windows e macOS, abra o Docker Desktop antes de executar os comandos. No Windows, use contêineres Linux.

Os builds são executados dentro dos contêineres: não é necessário instalar Java, Maven ou Node na máquina.

## Subir o projeto

Depois de clonar o repositório, abra um terminal na raiz do projeto e execute:

```sh
cd docker
docker compose up --build -d
```

Esse comando compila o back e o front e inicia os três serviços. Na primeira execução, o download das imagens e dependências pode levar alguns minutos. O backend aguarda o PostgreSQL ficar saudável; o frontend inicia depois que o contêiner do backend foi iniciado, mas a API pode levar mais alguns segundos para ficar disponível.

Também é possível executar a partir da raiz:

```sh
docker compose -f docker/compose.yaml up --build -d
```

| Serviço | Acesso padrão |
| --- | --- |
| Frontend | http://localhost:4200 |
| Backend | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui/index.html |
| PostgreSQL | localhost:5432 |
| Emails locais (Mailpit) | http://localhost:8025 |

O banco padrão é `advocacia`, com usuário `postgres` e senha `postgres`, para desenvolvimento local. A raiz do backend pode retornar 404 enquanto não houver um endpoint nessa rota.

Na rede Docker, o backend acessa o banco por `postgres:5432`, e o servidor do frontend recebe `API_URL=http://api:8080`. O navegador usa `http://localhost:8080`, configurado em `advocacia-app/src/environments/environment.ts`. O frontend já chama a API para autenticação e o backend aceita CORS de `http://localhost:4200` com credenciais.

## Personalizar portas e banco

Opcionalmente, copie `docker/.env.example` para `docker/.env` antes de subir. Dentro da pasta `docker`:

```powershell
# PowerShell
Copy-Item .env.example .env
```

```sh
# Linux/macOS
cp .env.example .env
```

Edite as variáveis conforme necessário:

| Variável | Padrão |
| --- | --- |
| POSTGRES_DB | advocacia |
| POSTGRES_USER | postgres |
| POSTGRES_PASSWORD | postgres |
| POSTGRES_PORT | 5432 |
| API_PORT | 8080 |
| APP_PORT | 4200 |

O arquivo `.env` é ignorado pelo Git. Use credenciais próprias em ambientes compartilhados. Se uma porta já estiver ocupada, altere a variável correspondente e execute novamente o comando de subida.

O PostgreSQL aplica usuário, senha e nome do banco somente quando inicializa um volume vazio. Alterar essas variáveis depois exige ajustar o banco existente ou recriar o volume, o que apaga seus dados.

## Comandos úteis

Execute os comandos abaixo dentro da pasta `docker`:

```sh
# Ver o estado dos serviços
docker compose ps

# Acompanhar os logs (Ctrl+C para sair)
docker compose logs -f

# Acompanhar somente o backend
docker compose logs -f api

# Reconstruir após alterações no código
docker compose up --build -d

# Parar e remover os contêineres, preservando o banco
docker compose down

# Abrir o terminal SQL com as credenciais configuradas
docker compose exec postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"'
```

Os dados do PostgreSQL ficam no volume `postgres-data` e sobrevivem à remoção dos contêineres. Para apagar o banco e começar novamente, execute **somente se quiser excluir todos os dados**:

```sh
docker compose down -v
```

Os Dockerfiles compilam a aplicação durante o build; os testes do backend não são executados nesse processo. Este Compose inclui PostgreSQL, backend, frontend e Mailpit para testar os emails localmente.

## Login por código de email

Na tela de login, informe seu email. Usuários ativos entram após validar o código; emails sem cadastro são bloqueados após a validação do código. Contas inativas continuam bloqueadas. O código tem seis dígitos, expira em 10 minutos, permite até cinco tentativas e só pode ser utilizado uma vez. Há intervalo de um minuto entre envios, limite de cinco envios por hora por email e 30 solicitações por hora por IP na instância da API. Apenas o hash do código é persistido.

Com a configuração padrão, os emails são capturados no Mailpit em http://localhost:8025, sem chegar a caixas reais. Após validar o código, a aplicação cria uma sessão em cookie HttpOnly e abre `/inicio`. A sessão expira após 30 minutos de inatividade, é perdida ao reiniciar o backend e é revogada no próximo acesso quando o usuário é desativado ou excluído. As APIs de usuário agora exigem sessão autenticada; operações de escrita também exigem token CSRF.

Não há autocadastro público. O primeiro administrador é provisionado uma única vez por configuração operacional. Consulte [Etapa 1 — Administração da plataforma](docs/etapa1-administracao-plataforma.md) para configurar o email e validar as telas. A conta administrativa entra em `/administracao` sem precisar de empresa. Cadastro de empresas, usuários e liberação de módulos serão entregues nas próximas etapas.

Quando for configurar um SMTP real, altere `MAIL_HOST`, `MAIL_PORT`, `MAIL_FROM`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_AUTH` e `MAIL_STARTTLS` em `docker/.env`. Para um SMTP com STARTTLS, use `MAIL_AUTH=true` e `MAIL_STARTTLS=true`; configure o remetente autorizado pelo provedor. Reinicie o serviço `api` após a alteração. Não publique o `.env` no Git.

O ícone Google inicia o OAuth quando Client ID e Secret estão configurados. Usuários ativos entram diretamente; emails sem cadastro são bloqueados. O login por código continua disponível.

## Configurar o login Google

As credenciais ficam somente em `docker/.env`, ignorado pelo Git. Preencha os campos abaixo nesse arquivo com seus valores reais:

```dotenv
GOOGLE_CLIENT_ID=
GOOGLE_CLIENT_SECRET=
GOOGLE_REDIRECT_URI=http://localhost:8080/login/oauth2/code/google
PUBLIC_URL=http://localhost:4200
```

Não substitua um `.env` existente copiando o exemplo por cima, pois isso apaga as credenciais locais. Client ID e Secret não estão no `.env.example`. O login Google é ativado automaticamente quando os dois campos estão preenchidos; sem eles, o login por email segue funcionando e o ícone informa que falta configurar o Google.

No cliente OAuth do Google, cadastre exatamente `http://localhost:8080/login/oauth2/code/google` como URI de redirecionamento autorizado. Para produção, use a URL HTTPS pública do backend e ajuste `PUBLIC_URL` para a URL do frontend. O backend solicita apenas `openid`, `email` e `profile`.

Após preencher as credenciais, execute na pasta `docker`:

```sh
docker compose up --build -d
```

O retorno do Google valida o email: usuários ativos entram e emails sem cadastro são bloqueados. No primeiro acesso, contas Gmail ou Google Workspace são vinculadas ao identificador estável `sub` do Google, persistido no banco. Contas Google com email externo sem Workspace usam o fluxo de código por email para evitar vínculo automático baseado em um endereço sobre o qual o Google não é autoridade. Uma conta Google diferente não substitui um vínculo existente.

### Cadastro controlado

As rotas públicas de cadastro estão desativadas; GET e POST em `/api/auth/cadastro` negam acesso. O futuro cadastro e os convites serão administrados pela plataforma.

### Envio de códigos por Gmail

Para enviar a caixas reais, configure em `docker/.env`: `MAIL_HOST=smtp.gmail.com`, `MAIL_PORT=587`, `MAIL_FROM` e `MAIL_USERNAME` com o endereço remetente, `MAIL_PASSWORD` com sua senha de aplicativo, `MAIL_AUTH=true` e `MAIL_STARTTLS=true`. Esses valores são independentes das credenciais OAuth do login Google. Após mudar o `.env`, execute `docker compose up -d --force-recreate api` na pasta `docker`. Mantenha senhas apenas no `.env` local, ignorado pelo Git.

O modelo está em `advocacia-microservice/src/main/resources/templates/email/codigo-acesso.html`. A mensagem inclui versão HTML e alternativa em texto, com identidade Gestão Advocacia, código de seis dígitos e validade de 10 minutos. Não carrega recursos externos. A prévia `docs/email-acesso-preview.html` usa um código fictício.
