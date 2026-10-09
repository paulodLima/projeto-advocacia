# Gestão de Advocacia

Projeto com backend Spring Boot (Java 21), frontend Angular com SSR e PostgreSQL 17.

A estrutura do backend, os perfis de ambiente e o CRUD de usuário estão documentados no [README do backend](advocacia-microservice/README.md).

O módulo CRM acompanha leads, cadências de contato, consultas, qualificação e campanhas de Marketing Sazonal, com dados separados por empresa. Consulte os fluxos, endpoints e integrações previstas na [documentação do CRM](docs/crm.md).

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

O cadastro cria um usuário ativo com nome e email. Quando há convite válido para o e-mail verificado, a conta é vinculada à empresa como MEMBRO. Sem convite, a conta começa sem empresa; ao cadastrar a empresa em Configurações, recebe o vínculo MASTER. Contas antigas não são agrupadas automaticamente. Os módulos futuros deverão usar o mesmo vínculo para separar os dados por empresa.

## Empresa e vínculo do usuário

Configurações → Empresa consulta e salva os dados da sociedade no PostgreSQL. A migration `V6__create_empresa_e_vinculo.sql` cria `empresa` (identificação e CNPJ único), `empresa_dado` (campos cadastrais validados) e `empresa_usuario` (uma empresa por usuário, com papel MASTER ou MEMBRO). O cadastro da empresa e do vínculo MASTER acontece na mesma transação; uma conta vinculada não pode cadastrar outra empresa.

| Método | Endpoint | Operação |
| --- | --- | --- |
| GET | `/api/empresas/minha` | Consultar a empresa da sessão. Sem vínculo, retorna `id: null`, `papel: null` e `dados: {}`. |
| POST | `/api/empresas` | Cadastrar a empresa e vincular o usuário da sessão como MASTER. |
| PUT | `/api/empresas/minha` | Atualizar a própria empresa; permitido somente ao MASTER. |

Escritas recebem `{ "dados": { "razao_social": "…", "cnpj": "…", "endereco": "…", "cidade": "…" } }`. Os quatro campos do exemplo são obrigatórios; os outros 20 campos da tela são opcionais e omissões são salvas como strings vazias. As respostas têm `id`, `papel` e `dados`. O backend aceita apenas os campos definidos em `CampoEmpresa`, verifica limites, email, UF, data e alíquota e normaliza o CNPJ para verificar duplicidade. Não há consulta de situação cadastral nem validação de dígitos verificadores de CPF/CNPJ nesta etapa.

Todos os endpoints exigem sessão de usuário ativo; POST e PUT exigem CSRF. O ID da empresa vem do vínculo da sessão, sem endpoint para escolher outra empresa pelo ID. MEMBRO consulta os dados, mas não altera. Cadastrar um CNPJ já usado retorna conflito e não associa a conta à empresa existente; o vínculo de membros é feito pelos convites de Equipe.

Os endpoints de `/api/usuarios` existentes também usam o vínculo: apenas MASTER cria e administra membros da própria empresa, e a criação já vincula a nova conta como MEMBRO. Consultar outra conta exige MASTER da mesma empresa; cada usuário continua podendo consultar a própria conta. Esses endpoints não alteram nem excluem o MASTER. A tela de Equipe usa seus próprios endpoints para convites, ficha e permissões.

## Equipe, convites e permissões

O MASTER administra a equipe em Configurações → Equipe. A migration `V7__create_equipe_e_convites.sql` adiciona a ficha dos membros, restrições de módulos e convites. Os vínculos existentes recebem os valores iniciais durante a migration. A listagem busca membros, fichas e telefones em lotes e não carrega as fotos pessoais de todos os usuários.

| Método | Endpoint | Operação |
| --- | --- | --- |
| GET | `/api/equipe/me` | Consultar o próprio vínculo e os módulos efetivamente permitidos. |
| GET | `/api/equipe` | MASTER lista membros e convites pendentes da própria empresa. |
| PUT | `/api/equipe/membros/{id}` | MASTER altera ficha, situação e permissões de um membro da própria empresa. |
| POST | `/api/equipe/convites` | MASTER grava o convite e envia o e-mail pelo SMTP configurado. |
| DELETE | `/api/equipe/convites/{id}` | Cancelar convite pendente da própria empresa. |
| POST | `/api/equipe/convites/{id}/reenviar` | Reenviar convite e renovar a validade por 7 dias. |

O convite recebe `nome`, `email` e `configuracao` (`nomeExibicao`, `telefone: ""`, `admissao: null`, `nascimento: null`, `perfil`, `situacao: "ATIVO"`, `restrito`, `abas`, `enviaDocumento`). A atualização do membro recebe diretamente `configuracao`, incluindo telefone, datas e situação. Perfis aceitos: `ADMINISTRADOR`, `ADVOGADO`, `ASSISTENTE`, `FINANCEIRO`. Situações: `ATIVO`, `INATIVO`, `SUSPENSO`. O telefone é o mesmo registro de Meu perfil; os outros dados pessoais são preservados. O MASTER não pode ser alterado, suspenso ou excluído por esses endpoints.

O convite usa o e-mail como identidade: o vínculo é criado somente após o login verificado por código ou Google, ou após concluir o cadastro novo com o e-mail já validado. O vínculo nasce como MEMBRO; o perfil Administrador não transforma a conta em MASTER. Convites cancelados ou expirados não são aceitos. E-mails já vinculados a uma empresa ou desativados são recusados; convites ativos duplicados também. Falhas de SMTP retornam 503 e desfazem a gravação ou renovação do convite. O modelo HTML e a alternativa em texto ficam em `templates/email/convite-equipe.html`.

| Perfil sem restrição personalizada | Módulos |
| --- | --- |
| MASTER / Administrador | Todos os módulos. Apenas MASTER administra Empresa e Equipe. |
| Advogado / Assistente | Todos, exceto Financeiro, Relatórios e Produtos. |
| Financeiro | Agenda, Contatos, Financeiro, Relatórios e Produtos. |

Início e configurações pessoais são sempre acessíveis. Com `restrito: true`, apenas as abas selecionadas são acrescentadas a esses dois módulos. Uma conta sem empresa tem apenas Início e Configurações. O frontend filtra menu, busca e abas administrativas e verifica permissões ao navegar por URL. O backend aplica as permissões aos prefixos dos módulos em `/api/{modulo}`; `/api/documentos/assinaturas` também exige `enviaDocumento`. Os módulos ainda sem API permanecem em desenvolvimento; novos endpoints devem manter esse controle e filtrar seus próprios dados pela empresa da sessão.

Inativar ou suspender atualiza também a situação do usuário, e a sessão é revogada na próxima requisição. Reativar exige novo login depois da revogação. Convites e membros antigos que eram apenas rascunhos no navegador não são importados ou enviados automaticamente.

As logos e a foto de login da empresa também são salvas no servidor. A prévia de documentos já lê os dados da empresa do servidor, e modelos e papel timbrado também usam seus próprios endpoints. Rascunhos antigos da empresa no navegador não são importados automaticamente: preencha os dados e clique em Cadastrar empresa.

## Identidade visual da empresa

Em Configurações → Empresa → Identidade visual, o MASTER envia as quatro versões de logo, escolhe a versão para menu expandido, menu recolhido e login, e ajusta zoom e posição da foto de entrada. Clique em **Salvar identidade visual** para publicar as alterações para toda a empresa. MEMBRO pode consultar, mas não editar. Rascunhos antigos do navegador não são importados automaticamente.

| Método | Endpoint | Operação |
| --- | --- | --- |
| GET | `/api/empresas/minha/identidade` | Consultar a identidade da empresa vinculada à sessão. |
| PUT | `/api/empresas/minha/identidade` | MASTER salva imagens, usos e recorte; exige sessão ativa e CSRF. |
| GET | `/api/public/empresas/{empresaId}/identidade` | Consultar somente nome e identidade visual pública para a tela de login. |

O PUT recebe `{ "imagens": {}, "usos": {}, "zoom": 1, "posX": 50, "posY": 50 }`. As chaves de imagens são `foto_login`, `logo_completa_branca`, `logo_simples_branca`, `logo_completa_marrom` e `logo_simples_marrom`; valores são data URLs PNG ou JPEG. `usos` associa `menu_expandido`, `menu_recolhido` e `login` a uma logo enviada. Remover uma imagem e sua seleção do corpo limpa esses registros. Zoom aceita 1–2,5 e posições 0–100. Cada imagem aceita até 2 MB e 1400 pixels por lado; o frontend redimensiona e converte WebP/SVG para PNG e a foto para JPEG. O backend verifica o conteúdo real e reencoda os pixels antes de gravar. A migration `V8__create_empresa_identidade.sql` guarda imagens e ajustes no PostgreSQL.

O campo **Link de entrada do escritório** fornece `/login?empresa=<UUID>`. Esse link mostra a marca antes da autenticação; a entrada comum `/login` usa Gestão Advocacia. O endpoint público não expõe CNPJ, contato, membros nem dados fiscais. O parâmetro do link não concede vínculo nem acesso à empresa: depois do login, a identidade vem exclusivamente da empresa da sessão. Imagens ausentes usam os padrões do produto. A marca é restaurada ao sair ou trocar de conta.

## Cadastros e sistemas

Configurações → Cadastros e sistemas usa o PostgreSQL para as nove listas do escritório, rotinas e acessos rápidos. Somente MASTER cria, edita ou exclui; membros ativos podem consultar os dados da própria empresa. A empresa é obtida exclusivamente da sessão. Contas sem empresa recebem coleções vazias e precisam cadastrar uma empresa para editar. Os rascunhos antigos do localStorage não são importados automaticamente.

| Método | Endpoint | Operação |
| --- | --- | --- |
| GET | `/api/config/cadastros` | Listas, rotinas, sistemas, empresaId e papel do usuário. |
| GET | `/api/config/cadastros/inicio` | Apenas rotinas e sistemas habilitados da empresa, sem carregar as listas. |
| POST | `/api/config/cadastros/listas/{tipo}` | Criar item com ID gerado pelo servidor. |
| PUT | `/api/config/cadastros/listas/{tipo}/{id}` | Atualizar item da própria empresa. |
| DELETE | `/api/config/cadastros/listas/{tipo}/{id}` | Excluir item sem vínculos dependentes. |
| POST | `/api/config/cadastros/rotinas` | Cadastrar nome e período da rotina. |
| DELETE | `/api/config/cadastros/rotinas/{id}` | Excluir rotina da própria empresa. |
| PUT | `/api/config/cadastros/sistemas` | Salvar a lista completa de acessos rápidos em uma transação. |

Todos exigem sessão ativa; escritas também exigem CSRF. A migration `V9__create_cadastros_sistemas_rotinas.sql` cria tabelas, índices por empresa e chaves estrangeiras. Os tipos aceitos são `grupos`, `documentos`, `acoes`, `fases`, `etapas`, `relacoes`, `tarefas`, `origens` e `etiquetas`. Escritas de itens recebem `{ "campos": { "nome": "…" }, "ativo": true }`; o servidor aceita somente os campos da lista, limita os nomes a 150 caracteres e rejeita nomes duplicados dentro da mesma lista e empresa. Siglas e códigos aceitam até 20 caracteres. Cor usa `#RRGGBB`. Classificação é inteira; pontos aceitam duas casas decimais; ambos de 0 a 1.000.000.

Ações recebem o UUID do grupo em `campos.grupo`; etapas e tarefas recebem o UUID da fase em `campos.fase`. O backend exige referência ativa do tipo correto e da mesma empresa. Renomear o grupo ou fase preserva seus vínculos. Excluir um grupo ou fase referenciado retorna 409. A ligação com processos e documentos será acrescentada quando esses módulos forem implementados; ainda não existe consulta de uso nesses módulos. Na tela, cada linha tem botão **Salvar**; falhas preservam o rascunho e exibem a mensagem do servidor.

Rotinas recebem `{ "nome": "…", "periodo": "diaria" }`, com períodos `diaria`, `semanal`, `mensal` ou `anual`; nomes repetidos no mesmo período e empresa são recusados. Elas aparecem agrupadas no Início. Esta etapa cadastra e exibe as rotinas; marcações de execução e histórico ainda não são implementados.

Sistemas recebem `{ "sistemas": [{ "id": "UUID", "nome": "…", "url": "https://…", "ativo": true, "icone": "link", "cor": "#e4dbd2", "logo": "" }] }`. Aceita até 30, preserva a ordem e recusa IDs pertencentes a outra empresa. Título tem até 150 caracteres e URL até 1000; apenas HTTP/HTTPS com host e sem credenciais embutidas. Ícones: `link`, `scale`, `calendar`, `file`, `wallet`. A logo opcional é PNG/JPEG validado, até 256 KB; o frontend converte e redimensiona para até 512 pixels. Clique em **Salvar sistemas** para publicar alterações, inclusive exclusões. Se qualquer sistema for inválido, a transação preserva todos os sistemas anteriores. Atalhos habilitados aparecem para a equipe no painel Acesso rápido do Início e abrem em nova aba.

As integrações externas (Google Agenda, ASAAS, ADVBOX e demais serviços), sincronizações e curadoria de notícias continuam pendentes. O cadastro de atalhos não cria uma conexão com essas plataformas. Workflows também persiste no servidor, com referências aos tipos de tarefa por UUID.

## Configurações de Documentos

Configurações → Documentos salva papel timbrado e nove trechos dos modelos de recibo, fatura, demonstrativo e prestação de contas no PostgreSQL. A migration `V10__create_documento_config.sql` cria as tabelas por empresa. A empresa é obtida da sessão; somente MASTER edita e membros ativos podem consultar. Todos os endpoints exigem autenticação; PUT também exige CSRF.

| Método | Endpoint | Operação |
| --- | --- | --- |
| GET | `/api/config/documentos` | Consultar `empresaId`, `papel`, `documento` e `modelos`. |
| PUT | `/api/config/documentos/papel` | Salvar logo, rodapé, alinhamento e opções de cabeçalho. |
| PUT | `/api/config/documentos/modelos` | Salvar `{ "modelos": { "recibo:corpo": "<p>…</p>" } }`. |

O papel recebe `logo`, `rodape`, `alinhamento` (`esquerda`, `centro`, `direita`), `repetir`, `documento`, `endereco` e `contato`. Imagens aceitam PNG/JPEG válido até 2 MB e 1400 pixels por lado; o frontend converte SVG para PNG preservando transparência. Strings vazias removem imagens. Papel e modelos são salvos separadamente e não sobrescrevem um ao outro. A prévia continua usando as alterações do formulário antes de salvar, com os dados da empresa carregados pelo contexto autenticado.

Cada trecho aceita até 20.000 caracteres e as chaves correspondem aos nove trechos da referência. Textos vazios são preservados; chaves ausentes usam os textos originais no frontend. O PUT de modelos substitui o conjunto de personalizações; a tela envia os nove trechos. O backend sanitiza HTML com jsoup, preservando parágrafos, negrito, itálico, sublinhado, listas, alinhamento simples e variáveis como `{{cliente}}`; scripts, eventos, imagens externas e links são removidos. Nenhuma variável é executada ou substituída nesta etapa.

Erros do servidor preservam as alterações no formulário. Rascunhos antigos do navegador não são importados automaticamente. O editor fica bloqueado sem empresa, durante o salvamento ou para usuários sem papel MASTER. Esta etapa configura modelos e timbrado; emissão de documentos reais, PDFs, numeração, assinaturas e envio serão implementados com os módulos que consomem essas configurações.

## Meu perfil

Configurações → Meu perfil salva telefone, e-mail de contato, endereço e foto no PostgreSQL. A migration `V5__create_usuario_perfil.sql` cria a tabela `usuario_perfil`, vinculada ao usuário; ela é aplicada automaticamente pela API ao iniciar. O e-mail de acesso permanece separado e não é alterado por esses endpoints.

| Método | Endpoint | Operação |
| --- | --- | --- |
| GET | `/api/usuarios/me/perfil` | Consultar o próprio perfil (campos vazios até o primeiro salvamento). |
| PUT | `/api/usuarios/me/perfil` | Atualizar `telefone`, `emailPessoal` e `endereco`; strings vazias limpam os campos. |
| PUT | `/api/usuarios/me/perfil/foto` | Salvar `{ "foto": "data:image/jpeg;base64,..." }` (JPEG ou PNG válido, até 512 × 512 pixels e 512 KB). |
| DELETE | `/api/usuarios/me/perfil/foto` | Remover a foto sem alterar os demais dados. |

As respostas contêm `usuarioId`, `telefone`, `emailPessoal`, `endereco` e `foto`. Todos os endpoints exigem uma sessão de usuário ativo; as escritas também exigem o token CSRF existente. O usuário é obtido da sessão, sem receber ID de outra conta. Atualizações de dados e foto são serializadas por usuário para preservar os campos. A foto pequena é armazenada como data URL em uma coluna de texto; a identidade visual da empresa também usa o banco; imagens do papel timbrado também são salvas por empresa.

Limites dos campos: telefone 30, e-mail de contato 254 (formato validado), endereço 500 caracteres. Meu perfil, Empresa, Identidade visual, Equipe, Cadastros e sistemas, Documentos, Workflows, Financeiro e Tema já usam o banco. Senhas também usa o banco com criptografia. Olívia continua adiada. Dados antigos do perfil no navegador não são enviados automaticamente; preencha e salve a tela para registrar no banco.

### Envio de códigos por Gmail

Para enviar a caixas reais, configure em `docker/.env`: `MAIL_HOST=smtp.gmail.com`, `MAIL_PORT=587`, `MAIL_FROM` e `MAIL_USERNAME` com o endereço remetente, `MAIL_PASSWORD` com sua senha de aplicativo, `MAIL_AUTH=true` e `MAIL_STARTTLS=true`. Esses valores são independentes das credenciais OAuth do login Google. Após mudar o `.env`, execute `docker compose up -d --force-recreate api` na pasta `docker`. Mantenha senhas apenas no `.env` local, ignorado pelo Git.

O modelo está em `advocacia-microservice/src/main/resources/templates/email/codigo-acesso.html`. A mensagem inclui versão HTML e alternativa em texto, com identidade Gestão Advocacia, código de seis dígitos e validade de 10 minutos. Não carrega recursos externos. A prévia `docs/email-acesso-preview.html` usa um código fictício.

### Workflows

Configurações → Workflows salva no PostgreSQL os workflows da empresa, suas etapas em ordem, responsáveis, prazos em dias úteis, buffer D- e gatilhos por tipo de tarefa. Somente MASTER cria, salva ou exclui; membros ativos podem consultar os dados da própria empresa. A empresa é identificada pela sessão, sem aceitar empresaId enviado pelo cliente.

| Método | Endpoint | Uso |
| --- | --- | --- |
| GET | `/api/config/workflows` | Consultar workflows, tipos de tarefa e responsáveis |
| POST | `/api/config/workflows` | Criar com nome; retorna UUID e versão |
| PUT | `/api/config/workflows/{id}` | Salvar nome, buffer, versão, gatilhos e etapas |
| DELETE | `/api/config/workflows/{id}?versao=N` | Excluir a versão conhecida e suas etapas/gatilhos |

A migração V11 cria `escritorio_workflow`, `workflow_etapa` e `workflow_gatilho`. Gatilhos usam UUIDs dos tipos de tarefa ativos de Cadastros; renomear um tipo mantém o vínculo. Uma tarefa vinculada não pode ser excluída até remover seus gatilhos. Responsáveis são usuários ativos da própria equipe; etapas também podem ficar sem responsável. Opções desativadas permanecem identificadas na consulta e precisam ser removidas ou substituídas antes do próximo salvamento.

Os nomes aceitam até 150 caracteres, com unicidade do nome do workflow por empresa. Cada workflow aceita até 100 etapas e 100 gatilhos sem duplicidade, prazo de 1 a 365 dias por etapa e buffer de 0 a 365. Atualizações e exclusões exigem a versão retornada na consulta: uma versão desatualizada retorna 409, preservando o estado do banco. Recarregue os dados antes de repetir a alteração.

Criar e excluir são operações imediatas; alterações de etapas, responsáveis, ordem, prazos e gatilhos são enviadas pelo botão **Salvar workflow**, somente para o workflow escolhido. Falhas preservam os rascunhos na tela, e o sucesso só é confirmado após a resposta do servidor. Rascunhos antigos do navegador não são importados. Esta etapa configura workflows; execução automática, criação de tarefas, cálculo de datas e notificações serão integrados posteriormente ao módulo de tarefas.

### Configurações financeiras

Configurações → Financeiro salva no PostgreSQL contas e cartões, categorias, centros de custos, custos mensais, URH/competência e capacidade produtiva, mantendo o layout da tela. MASTER administra; membros ativos podem consultar os dados da própria empresa. Contas sem empresa recebem listas vazias e os parâmetros iniciais, com edição bloqueada até cadastrar a empresa.

A migração V12 cria `financeiro_config`, `financeiro_conta`, `financeiro_categoria`, `financeiro_centro` e `financeiro_custo`. Valores monetários usam NUMERIC(14,2) no banco e BigDecimal no backend. Nomes aceitam até 150 caracteres, com unicidade por empresa e tipo de cadastro, sem diferenciar maiúsculas de minúsculas. O saldo inicial pode ser negativo; custos e URH devem ser positivos. Apenas uma conta ativa pode ser padrão; desativar a conta padrão remove sua seleção. Categorias, contas e centros são desativados/reativados, preservando seus UUIDs.

| Método | Endpoint | Uso |
| --- | --- | --- |
| GET | `/api/config/financeiro` | Consultar configuração e versão |
| POST | `/api/config/financeiro/contas` | Criar conta/cartão |
| PUT | `/api/config/financeiro/contas/{id}` | Editar, definir padrão, desativar/reativar |
| POST | `/api/config/financeiro/categorias` | Criar categoria |
| PUT | `/api/config/financeiro/categorias/{id}` | Editar ou desativar/reativar categoria |
| POST | `/api/config/financeiro/centros` | Criar centro de custos |
| PUT | `/api/config/financeiro/centros/{id}` | Renomear ou desativar/reativar centro |
| POST | `/api/config/financeiro/custos` | Adicionar custo operacional mensal |
| DELETE | `/api/config/financeiro/custos/{id}?versao=N` | Excluir custo |
| PUT | `/api/config/financeiro/urh` | Salvar valor e competência AAAA-MM |
| PUT | `/api/config/financeiro/capacidade` | Salvar horas, estrutura, margem e posicionamento |

POST e PUT recebem `{ "versao": 0, "dados": { ... } }`; a versão vem da última consulta ou resposta de escrita. Todas as escritas retornam a configuração atualizada e sua nova versão. Uma versão desatualizada retorna 409 sem alterar o banco; use **Recarregar do banco** e revise a edição. Operações são atômicas, incluindo troca da conta padrão. O backend gera UUIDs e obtém a empresa pela sessão; IDs de outras empresas retornam 404.

URH e capacidade são salvas separadamente. A tela aplica somente a seção confirmada pelo servidor, preservando outros campos ainda em edição. Erros conservam os formulários, e a seleção da conta padrão/desativação/exclusão só muda após sucesso. Não há importação automática de rascunhos do localStorage.

A capacidade aceita modo direto ou por estrutura, de 1 a 10000 advogados, jornada de 1 a 168 horas semanais, produtividade de 1 a 100%, de 1 a 5 semanas por mês, margem de 0 a 100% e posicionamento de 1,5 a 10. Horas diretas vão de 1 a 1.000.000; números aceitam até duas casas decimais. Os indicadores de custo e hora são prévias no frontend. Esta etapa configura a estrutura financeira; lançamentos, conciliação, cobranças, integração bancária e a Calculadora de Honorários serão implementados em seus módulos. A URH armazenada corresponde à competência configurada, sem atualização automática ou histórico mensal nesta etapa.

### Tema por usuário

Configurações → Tema salva a preferência no PostgreSQL por usuário, permitindo manter a mesma cor em diferentes acessos, navegadores e dispositivos. Verde continua como padrão para contas sem escolha. As seis opções são `verde`, `vermelho`, `amarelo`, `azul`, `preto` e `roxo`.

A migração V13 cria `usuario_tema`, com um registro por usuário e exclusão em cascata quando a conta é removida. GET `/api/usuarios/me/tema` retorna `{ "usuarioId": "UUID", "tema": "verde" }`; PUT no mesmo endpoint recebe `{ "tema": "azul" }` e retorna a preferência salva. Os endpoints exigem sessão, e PUT exige CSRF. O usuário é obtido da sessão: a preferência é pessoal, disponível também para MEMBRO e contas sem empresa, e não permite editar o tema de outra pessoa.

A cor é aplicada imediatamente ao escolher, com indicação de salvamento. Mudanças rápidas são enviadas em sequência para que a última escolha seja a última gravada. A confirmação aparece apenas após a resposta do servidor. Se uma gravação falhar, o visual escolhido permanece como prévia e a tela informa que não foi salvo, oferecendo nova tentativa. Uma falha de carga mantém o verde e oferece repetir a consulta. Ao sair, a aplicação volta ao verde; ao trocar de usuário, cancela cargas e escritas pendentes da conta anterior e consulta a nova preferência. Escolhas antigas do localStorage não são importadas automaticamente.


### Cofre de senhas

Configurações → Senhas usa `/api/config/cofre` para credenciais compartilhadas pela empresa. Somente MASTER cadastra, edita e exclui. Membros da empresa podem consultar/revelar credenciais compartilhadas; itens com **Visível somente para administradores** aparecem exclusivamente para MASTER e membros com perfil ADMINISTRADOR. Esse perfil não concede edição. Contas sem empresa recebem uma lista vazia. A migração V14 cria `cofre_credencial`.

Usuário/login, senha, descrição e observações são criptografados com AES-256-GCM, nonce aleatório por gravação e autenticação vinculada ao UUID da empresa e da credencial. Nome, URL e visibilidade são metadados. A chave fica na variável `COFRE_KEY`, fora do banco e do Git, e deve conter 32 bytes codificados em Base64. Uma chave ausente/inválida desabilita o cofre; dados adulterados ou uma chave diferente impedem a leitura. Não há fallback para texto aberto.

No ambiente local desta implementação, uma chave foi gerada em `docker/.env`. Preserve essa chave e guarde um backup protegido junto do plano de recuperação do banco: trocar ou perder a chave impede abrir as credenciais existentes. A aplicação não implementa rotação automática de chave. Para um novo ambiente, gere sua própria chave e coloque o resultado em `COFRE_KEY` do `.env`, antes de subir a API:

```sh
docker run --rm node:24-alpine node -e "console.log(require('crypto').randomBytes(32).toString('base64'))"
```

| Método | Endpoint | Uso |
| --- | --- | --- |
| GET | `/api/config/cofre` | Listar metadados permitidos, sem senhas |
| POST | `/api/config/cofre` | Criar credencial |
| PUT | `/api/config/cofre/{id}` | Atualizar credencial com versão |
| POST | `/api/config/cofre/{id}/revelar` | Consultar a senha após ação explícita |
| DELETE | `/api/config/cofre/{id}?versao=N` | Excluir a versão conhecida |

Escritas e revelação exigem CSRF, além da sessão ativa. O backend determina a empresa e verifica a visibilidade em cada operação; IDs externos e itens ocultos retornam 404. As respostas não permitem cache (`Cache-Control: no-store`). Na edição, `senha: null` mantém a senha existente; criar exige senha. Limites: nome 150, URL 1000, login 254, senha 4096, descrição 500 e observações 4000 caracteres. URLs aceitam apenas HTTP/HTTPS sem usuário/senha embutidos. Nomes são únicos por empresa. Versões desatualizadas retornam 409, preservando o registro.

A tela permite busca, criação, edição, exclusão e revelação. A senha revelada fica apenas em memória, desaparece após 30 segundos, ao ocultar a aba do navegador ou ao sair da tela. A senha digitada é limpa ao cancelar, após salvar e ao fechar a tela. A listagem não consulta senhas automaticamente. Credenciais não são armazenadas em localStorage nem enviadas a serviços externos. Falhas preservam os formulários para nova tentativa. Esta etapa não inclui autofill, sincronização com gestores externos ou rotação de senhas dos sistemas cadastrados.

### Contatos

O menu **Contatos** segue a referência de `docs/index.html`: grade/lista, busca, filtros por tipo e indicadores, painel lateral de cadastro, ficha e edição. Os seis tipos são Cliente, Parte adversa, Parte interessada, Advogado(a), Fornecedor e Parceiro. Pessoas físicas e jurídicas têm qualificação, telefones/WhatsApp, endereço, representantes, origem, carteira, indicadores e observações. A consulta opcional ao ViaCEP usa somente o CEP informado; falhas permitem preencher o endereço manualmente.

A migração V15 cria `contato`, `contato_dado`, `contato_representante`, `contato_indicador`, `contato_tag` e `contato_evento`. Dados e referências são isolados por empresa. MASTER e membros ativos com acesso ao módulo e perfil ADMINISTRADOR, ADVOGADO ou ASSISTENTE podem editar; FINANCEIRO pode consultar. A empresa é determinada pela sessão. Contas sem empresa ou sem acesso ao módulo recebem 403, e IDs de outras empresas retornam 404.

| Método | Endpoint | Uso |
| --- | --- | --- |
| GET | `/api/contatos?busca=&tipo=&indicador=&pagina=0&tamanho=24` | Listagem paginada; tamanho máximo 100 |
| GET | `/api/contatos/opcoes` | Permissão de edição, indicadores, origens ativas e parceiros |
| GET | `/api/contatos/{id}` | Ficha completa |
| POST | `/api/contatos` | Criar contato |
| PUT | `/api/contatos/{id}` | Atualizar contato com versão |
| DELETE | `/api/contatos/{id}?versao=N` | Excluir a versão conhecida |
| POST | `/api/contatos/indicadores` | Criar indicador com `{ "nome": "..." }` |

POST e PUT recebem `{ "versao": 0, "dados": { ... }, "representantes": [], "indicadores": [] }`. A ficha retornada inclui `id`, `empresaId` e `versao`. Escritas exigem CSRF e são atômicas. Versões desatualizadas e documentos duplicados na mesma empresa retornam 409. A verificação de CPF/CNPJ considera a quantidade de dígitos e a unicidade, sem validar os dígitos verificadores. Clientes exigem nome, documento, telefone e e-mail; os demais tipos exigem nome. Limites: 20 representantes, 50 indicadores por contato, 100 indicadores por empresa e 4000 caracteres nas observações.

Origem usa o cadastro ativo de Configurações → Cadastros e sistemas. Carteira de parceiro utiliza um contato do tipo Parceiro da mesma empresa; enquanto houver contatos vinculados, esse parceiro não pode ser excluído nem mudar de tipo. Indicadores desta tela são próprios de Contatos. O histórico registra identificadores, operação e data, sem copiar os dados pessoais para o evento. Não há importação automática de dados antigos do navegador.

Esta etapa não implementa vínculos com processos nem campanhas automáticas de marketing. O cadastro profissional de parceiros está descrito na seção seguinte. A ficha informa que os processos serão integrados com Gestão Processual. Para atualizar os containers locais, execute `docker compose -f docker/compose.yaml up --build -d` na raiz do projeto; o Flyway aplica V15 ao iniciar a API.

### Parceiros

O menu **Parceiros** segue `docs/index.html`: lista em duas colunas, busca por nome/nome fantasia/OAB/cidade, filtros por estado e área, cadastro em painel lateral, ficha e edição. O formulário oferece Advogado (PF), Escritório (PJ), OAB, advogado responsável, sócios, foto, site, Instagram, endereço, áreas de atuação e observações.

Parceiros e Contatos compartilham o mesmo cadastro e UUID. Contatos do tipo Parceiro já existentes aparecem no menu, com os dados profissionais inicialmente vazios. Criar um parceiro o disponibiliza imediatamente no seletor de carteira de Contatos. Alterações dos campos comuns aparecem nas duas telas; os dados profissionais e indicadores existentes são preservados ao editar pela outra tela. A versão é compartilhada: alterações simultâneas por Contatos ou Parceiros retornam 409 para o formulário desatualizado. Cadastros profissionais devem manter o tipo Parceiro. Salvar como PF remove os sócios; a foto e os demais dados profissionais permanecem.

A migração V16 cria `parceiro_dado`, `parceiro_socio` e `parceiro_area`, vinculadas ao contato, e um índice para consultar carteiras. O backend determina a empresa pela sessão. MASTER e membros ativos com acesso a Parceiros e perfil ADMINISTRADOR, ADVOGADO ou ASSISTENTE podem editar; FINANCEIRO pode consultar. A carteira exige também permissão de Contatos, e usa vínculos reais da mesma empresa. IDs externos ou contatos de outro tipo retornam 404; acesso não autorizado retorna 403. Não há importação automática do navegador.

| Método | Endpoint | Uso |
| --- | --- | --- |
| GET | `/api/parceiros?busca=&uf=&area=&pagina=0&tamanho=24` | Listagem paginada; tamanho máximo 100 |
| GET | `/api/parceiros/opcoes` | Permissões, estados cadastrados e áreas disponíveis |
| GET | `/api/parceiros/{id}` | Ficha completa |
| GET | `/api/parceiros/{id}/carteira?pagina=0&tamanho=24` | Contatos vinculados à carteira |
| POST | `/api/parceiros` | Criar parceiro e contato compartilhado |
| PUT | `/api/parceiros/{id}` | Atualizar a versão conhecida |
| DELETE | `/api/parceiros/{id}?versao=N` | Excluir parceiro e contato compartilhado |

POST/PUT recebem `{ "versao": 0, "dados": { "nome": "...", "tipo_pessoa": "PF" }, "socios": [], "areasAtuacao": [] }`. Sócios têm `nome` e `oab`; as áreas são as 12 opções da referência. O nome é obrigatório; CPF/CNPJ, telefone e e-mail são opcionais, mas validados quando preenchidos. Documentos seguem a quantidade de dígitos e a unicidade por empresa de Contatos, sem validação dos dígitos verificadores. Limites: 20 sócios, 150 caracteres por campo comum, 254 no e-mail, 1000 no site e 4000 nas observações. Site aceita HTTP/HTTPS sem credenciais embutidas.

A foto é reduzida no navegador a até 400 pixels e enviada junto do formulário como JPEG. A API valida o conteúdo da imagem, formato JPEG/PNG, dimensões máximas de 512 × 512 e tamanho de 512 KB; a foto fica no PostgreSQL. A prévia só é persistida ao salvar. A consulta opcional ao ViaCEP transmite somente o CEP e permite preencher o endereço manualmente em caso de erro.

Escritas exigem CSRF e são atômicas, incluindo os campos compartilhados. Respostas usam `Cache-Control: no-store`. A auditoria utiliza `contato_evento` com identificadores, operação e data. Parceiros com contatos na carteira não podem ser excluídos: primeiro remova os vínculos. A exclusão remove também o contato compartilhado e seus dados profissionais. Esta etapa não implementa processos em parceria, percentuais, repasses nem saldos financeiros; essas partes da referência serão integradas com Gestão Processual e Financeiro.

Atualize os containers pela raiz com `docker compose -f docker/compose.yaml up --build -d`. O Flyway aplica V16 ao iniciar a API.
