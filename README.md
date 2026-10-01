# Gestão de Advocacia

Projeto com backend Spring Boot (Java 21), frontend Angular com SSR e PostgreSQL 17.

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

Na tela de login, informe seu email. Usuários ativos entram após validar o código; emails novos seguem para `/cadastro`, onde informam o nome completo e criam a conta. Contas inativas continuam bloqueadas. O código tem seis dígitos, expira em 10 minutos, permite até cinco tentativas e só pode ser utilizado uma vez. Há intervalo de um minuto entre envios, limite de cinco envios por hora por email e 30 solicitações por hora por IP na instância da API. Apenas o hash do código é persistido.

Com a configuração padrão, os emails são capturados no Mailpit em http://localhost:8025, sem chegar a caixas reais. Após validar o código, a aplicação cria uma sessão em cookie HttpOnly e abre `/inicio`. A sessão expira após 30 minutos de inatividade, é perdida ao reiniciar o backend e é revogada no próximo acesso quando o usuário é desativado ou excluído. As APIs de usuário agora exigem sessão autenticada; operações de escrita também exigem token CSRF.

Não há autocadastro público. Para cadastrar o primeiro usuário, um administrador deve inserir o registro no banco. Abra o terminal SQL com o comando mostrado acima e execute, substituindo nome e email pelos dados desejados:

```sql
INSERT INTO usuario (id, nome, email, status)
VALUES (gen_random_uuid(), 'Seu Nome', 'seu-email@example.com', 'ATIVO');
```

Use email em minúsculas. Nenhum usuário padrão é criado automaticamente. O modelo atual ainda não diferencia permissões administrativas entre usuários autenticados.

Quando for configurar um SMTP real, altere `MAIL_HOST`, `MAIL_PORT`, `MAIL_FROM`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_AUTH` e `MAIL_STARTTLS` em `docker/.env`. Para um SMTP com STARTTLS, use `MAIL_AUTH=true` e `MAIL_STARTTLS=true`; configure o remetente autorizado pelo provedor. Reinicie o serviço `api` após a alteração. Não publique o `.env` no Git.

O ícone Google inicia o OAuth quando Client ID e Secret estão configurados. Usuários ativos entram diretamente; emails novos verificados seguem para completar o cadastro. O login por código continua disponível.

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

O retorno do Google valida o email: usuários ativos entram e emails novos seguem para completar o cadastro. A conta só é criada após enviar o nome completo. No primeiro acesso, contas Gmail ou Google Workspace são vinculadas ao identificador estável `sub` do Google, persistido no banco. Contas Google com email externo sem Workspace usam o fluxo de código por email para evitar vínculo automático baseado em um endereço sobre o qual o Google não é autoridade. Uma conta Google diferente não substitui um vínculo existente.

### Cadastro após validar o email

`GET /api/auth/cadastro` informa o email validado da sessão e `POST /api/auth/cadastro` recebe apenas `nome`. O email é definido pelo backend e não pode ser trocado no formulário. A autorização de cadastro expira em 15 minutos e é removida ao concluir. Antes da conclusão, essa sessão não autoriza acesso às APIs protegidas. No Google, o vínculo `sub` é salvo junto com a criação da conta.

O cadastro atual cria um usuário ativo com nome e email. Empresas, isolamento de dados e perfil master ainda dependem da implementação de multiempresa.

### Envio de códigos por Gmail

Para enviar a caixas reais, configure em `docker/.env`: `MAIL_HOST=smtp.gmail.com`, `MAIL_PORT=587`, `MAIL_FROM` e `MAIL_USERNAME` com o endereço remetente, `MAIL_PASSWORD` com sua senha de aplicativo, `MAIL_AUTH=true` e `MAIL_STARTTLS=true`. Esses valores são independentes das credenciais OAuth do login Google. Após mudar o `.env`, execute `docker compose up -d --force-recreate api` na pasta `docker`. Mantenha senhas apenas no `.env` local, ignorado pelo Git.

O modelo está em `advocacia-microservice/src/main/resources/templates/email/codigo-acesso.html`. A mensagem inclui versão HTML e alternativa em texto, com identidade Gestão Advocacia, código de seis dígitos e validade de 10 minutos. Não carrega recursos externos. A prévia `docs/email-acesso-preview.html` usa um código fictício.
