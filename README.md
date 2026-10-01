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

Na tela de login, informe o email de um usuário cadastrado e ativo. O código tem seis dígitos, expira em 10 minutos, permite até cinco tentativas e só pode ser utilizado uma vez. Há intervalo de um minuto entre envios, limite de cinco envios por hora por usuário e 30 solicitações por hora por IP na instância da API. Apenas o hash do código é persistido.

Com a configuração padrão, os emails são capturados no Mailpit em http://localhost:8025, sem chegar a caixas reais. Após validar o código, a aplicação cria uma sessão em cookie HttpOnly e abre `/inicio`. A sessão expira após 30 minutos de inatividade, é perdida ao reiniciar o backend e é revogada no próximo acesso quando o usuário é desativado ou excluído. As APIs de usuário agora exigem sessão autenticada; operações de escrita também exigem token CSRF.

Não há autocadastro público. Para cadastrar o primeiro usuário, um administrador deve inserir o registro no banco. Abra o terminal SQL com o comando mostrado acima e execute, substituindo nome e email pelos dados desejados:

```sql
INSERT INTO usuario (id, nome, email, status)
VALUES (gen_random_uuid(), 'Seu Nome', 'seu-email@example.com', 'ATIVO');
```

Use email em minúsculas. Nenhum usuário padrão é criado automaticamente. O modelo atual ainda não diferencia permissões administrativas entre usuários autenticados.

Quando for configurar um SMTP real, altere `MAIL_HOST`, `MAIL_PORT`, `MAIL_FROM`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_AUTH` e `MAIL_STARTTLS` em `docker/.env`. Para um SMTP com STARTTLS, use `MAIL_AUTH=true` e `MAIL_STARTTLS=true`; configure o remetente autorizado pelo provedor. Reinicie o serviço `api` após a alteração. Não publique o `.env` no Git.

O ícone Google fica como opção secundária na tela, mas a integração OAuth ainda depende da configuração do provedor. Emails Gmail já podem usar o fluxo de código, desde que estejam cadastrados.
