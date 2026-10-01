# Backend de Advocacia

O pacote base permanece `com.advocacia_microservice`. A aplicação usa organização por funcionalidade, com camadas internas em cada módulo.

```text
src/main/java/com/advocacia_microservice/
├── AdvocaciaMicroserviceApplication.java
├── config/
│   ├── OpenApiConfig.java
│   ├── SecurityConfig.java
│   └── JacksonConfig.java
├── shared/
│   ├── exception/
│   ├── security/
│   ├── validation/
│   └── util/
├── auth/
├── usuario/
│   ├── api/
│   │   ├── UsuarioController.java
│   │   ├── request/
│   │   │   ├── CriarUsuarioRequest.java
│   │   │   └── AtualizarUsuarioRequest.java
│   │   └── response/UsuarioResponse.java
│   ├── application/
│   │   ├── CriarUsuarioUseCase.java
│   │   ├── AtualizarUsuarioUseCase.java
│   │   ├── BuscarUsuarioUseCase.java
│   │   └── ExcluirUsuarioUseCase.java
│   ├── domain/
│   │   ├── Usuario.java
│   │   ├── StatusUsuario.java
│   │   └── UsuarioRepository.java
│   └── infrastructure/persistence/
│       ├── UsuarioEntity.java
│       ├── JpaUsuarioRepository.java
│       └── UsuarioRepositoryImpl.java
├── cliente/
├── processo/
├── agenda/
├── documento/
├── financeiro/
└── notificacao/

src/main/resources/
├── application.yml
├── application-dev.yml
├── application-hml.yml
├── application-prd.yml
└── db/migration/V1__create_usuario.sql

src/test/
├── java/com/advocacia_microservice/
│   ├── AdvocaciaMicroserviceApplicationTests.java
│   └── usuario/UsuarioUseCasesTests.java
└── resources/application-test.yml
```

Os módulos ainda sem implementação têm `package-info.java`, preservando as pastas no Git. As migrations de cliente e processo devem ser adicionadas quando seus campos e relacionamentos forem definidos; não há migrations vazias que o Flyway marcaria como executadas.

## Como aplicar o padrão

- `api`: recebe HTTP, valida os requests e converte respostas. Não acessa JPA diretamente.
- `application`: coordena os casos de uso e define os limites de transação.
- `domain`: representa dados e regras, além do contrato do repositório. Não depende de Spring nem JPA.
- `infrastructure`: implementa o contrato com JPA e converte entidade para domínio.
- `shared`: contém somente componentes usados por mais de uma funcionalidade.
- `config`: contém configurações técnicas da aplicação.

É uma boa base para um monólito modular: facilita encontrar o código e evoluir as funcionalidades separadamente. O nome do diretório `microservice` não transforma cada módulo em um serviço independente. Separar entidade JPA e domínio tem um custo de conversão, mas permite manter as regras sem dependência da persistência. Para funcionalidades pequenas, evite acrescentar interfaces e abstrações sem necessidade.

`SecurityConfig` configura CORS com credenciais para `http://localhost:4200`, sessões e proteção CSRF. O CRUD exige autenticação por código de email de um usuário cadastrado e ativo. `JacksonConfig` permanece reservado; o Jackson usa a configuração padrão do Spring Boot. Não existe senha no modelo de usuário. Permissões por perfil administrativo ainda não estão implementadas.

## Autenticação

- `GET /api/auth/csrf`: obtém token e nome do header CSRF para operações de escrita.
- `POST /api/auth/codigo`: recebe `{ "email": "seu@email.com" }` e retorna `desafioId`. Usa resposta genérica para emails inexistentes, inativos ou com limite de envios atingido.
- `POST /api/auth/validar`: recebe `{ "desafioId": "uuid", "codigo": "123456" }`. Ao validar, renova o ID da sessão e devolve os dados do usuário.
- `GET /api/auth/me`: consulta a sessão atual.
- `POST /api/auth/logout`: encerra a sessão.

O frontend envia cookies e obtém o token CSRF antes de cada operação de escrita. O código é gerado com SecureRandom, armazenado como hash BCrypt, expira em 10 minutos e permite cinco tentativas. A validação bloqueia a linha no banco para impedir consumo concorrente do mesmo código. O serviço usa Spring Mail e as variáveis SMTP documentadas no README da raiz. O perfil `prd` exige cookie Secure, portanto precisa ser servido por HTTPS.

Para testar localmente e cadastrar o primeiro usuário, consulte o [README da raiz](../README.md).

## Executar com Docker

Na raiz do repositório:

```sh
docker compose -f docker/compose.yaml up --build -d
```

As instruções completas estão no [README da raiz](../README.md). O Compose principal continua em `docker/compose.yaml`. O arquivo `compose.yaml` deste módulo é o exemplo de banco gerado pelo Spring Initializr; ele não é usado automaticamente, pois `spring.docker.compose.enabled` está desativado.

## Perfis e banco

O perfil padrão é `dev`, com PostgreSQL local em `localhost:5432/advocacia` e usuário/senha `postgres`. Dentro do Docker, as variáveis do Compose substituem esses valores.

Para `hml` ou `prd`, configure `SPRING_PROFILES_ACTIVE` e forneça `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` e `SPRING_DATASOURCE_PASSWORD`. O Swagger fica desativado em `prd`.

O Flyway aplica as migrations e o Hibernate apenas valida o schema (`ddl-auto: validate`). Novas alterações de banco devem usar uma nova migration versionada; não edite migrations já aplicadas.

## Modelo de usuário

| Método | Rota | Resultado |
| --- | --- | --- |
| POST | `/api/usuarios` | Cria usuário ativo; retorna 201 e Location |
| GET | `/api/usuarios/{id}` | Busca pelo UUID; retorna 200 ou 404 |
| PUT | `/api/usuarios/{id}` | Atualiza nome, email e status; retorna 200 |
| DELETE | `/api/usuarios/{id}` | Exclui usuário; retorna 204 ou 404 |

Exemplo de criação:

```json
{
  "nome": "Maria Silva",
  "email": "maria@example.com"
}
```

Para atualização, envie também `"status": "ATIVO"` ou `"status": "INATIVO"`. O email é normalizado para minúsculas e deve ser único. Dados inválidos retornam 400 e email duplicado retorna 409. A restrição única do banco também cobre criações concorrentes.

Documentação interativa em http://localhost:8080/swagger-ui/index.html no perfil de desenvolvimento.

## Testes

Dentro de `advocacia-microservice`:

```powershell
.\mvnw.cmd clean test
```

```sh
./mvnw clean test
```

Os testes usam H2 em modo PostgreSQL, aplicam a migration e verificam inicialização, CRUD e conflito de email na criação/atualização. Não exigem Docker. O comportamento específico do PostgreSQL ainda deve ser verificado nesse banco.
