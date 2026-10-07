# Frontend de Advocacia

Angular com componentes standalone e suporte a SSR, organizado por funcionalidade.

## Identidade visual por empresa

A identidade padrão do produto é **Gestão Advocacia**, com monograma GA em `public/images/logo-ga.svg`. Nome, logo e imagem do login são consumidos pelo `IdentidadeEmpresaService`, em `core/branding`, e têm um contrato próprio `IdentidadeEmpresa`. A foto atual permanece somente como imagem padrão.

A identidade é carregada por `IdentidadeApiService` e aplicada com `IdentidadeEmpresaService.receber(...)`: após o login usa a empresa da sessão, e antes do login usa o link público `/login?empresa=<UUID>`. Dados ausentes retornam aos padrões do produto; ao trocar de empresa, dados da marca anterior não são reaproveitados. Imagens que não carregarem também usam o padrão.

O serviço ainda não consulta uma API nem o banco. A futura identificação da empresa antes do login deverá vir de um contexto verificável, como domínio ou slug, e ser resolvida pelo backend. O isolamento dos dados e o perfil master para cadastrar usuários ainda precisam ser implementados; não são garantidos pela personalização visual.

## Estrutura

```text
src/
├── app/
│   ├── core/
│   │   ├── auth/
│   │   ├── guards/
│   │   ├── interceptors/
│   │   └── config/api-url.token.ts
│   ├── shared/
│   │   ├── components/
│   │   ├── directives/
│   │   ├── pipes/
│   │   ├── validators/
│   │   └── utils/
│   ├── layouts/
│   │   ├── authenticated-layout/
│   │   └── public-layout/
│   ├── features/
│   │   ├── auth/
│   │   ├── usuario/
│   │   ├── cliente/
│   │   ├── processo/
│   │   ├── agenda/
│   │   ├── documento/
│   │   ├── financeiro/
│   │   └── notificacao/
│   ├── app.ts / app.html / app.scss
│   ├── app.config.ts / app.config.server.ts
│   └── app.routes.ts / app.routes.server.ts
├── environments/environment.ts
├── styles.scss
├── main.ts / main.server.ts
└── server.ts
```

Modelo de funcionalidade:

```text
features/usuario/
├── pages/
│   ├── usuario-list/
│   ├── usuario-create/
│   ├── usuario-edit/
│   └── usuario-detail/
├── components/usuario-form/
├── services/usuario-api.service.ts
├── models/
│   ├── usuario.model.ts
│   ├── criar-usuario-request.model.ts
│   └── atualizar-usuario-request.model.ts
└── usuario.routes.ts
```

## Responsabilidades

- `core`: infraestrutura global, sessão, guards, interceptors e configuração da API.
- `shared`: elementos reutilizáveis entre funcionalidades, sem regras específicas de usuário ou processo.
- `layouts`: estrutura externa das páginas. Os layouts possuem apenas RouterOutlet como base; o nome authenticated não implementa autenticação.
- `features`: código específico de cada funcionalidade.
- `pages`: componentes associados às rotas.
- `components`: partes reutilizáveis dentro da funcionalidade, como o formulário de criação e edição.
- `services`: comunicação HTTP com a API.
- `models`: tipos dos dados e contratos de request/response.

Pastas reservadas usam `.gitkeep` para serem preservadas no Git. A página `/login` usa o visual de `docs/index.html`, com acesso principal por código enviado ao email. Após validar, abre `/inicio`, protegida por guard que verifica a sessão no backend. A raiz redireciona para `/login`. O Google aparece como ícone secundário: consulta `/api/auth/providers` e redireciona o navegador para `/oauth2/authorization/google` no backend. As credenciais Google ficam somente no backend. Falhas do retorno OAuth aparecem na tela com mensagens controladas. As demais funcionalidades ainda estão reservadas.

`core/auth/auth.service.ts` gerencia solicitação/validação do código, consulta da sessão e logout. `core/interceptors/session.interceptor.ts` envia cookies somente para a API configurada e busca o token CSRF antes das escritas. As credenciais não são armazenadas em localStorage. A rota `/inicio` usa renderização no cliente para não prerenderizar conteúdo de uma sessão privada. No ambiente local, os códigos chegam ao Mailpit em http://localhost:8025, conforme documentado no README da raiz.

## Rotas e componentes

Use componentes standalone e carregue cada funcionalidade sob demanda. Depois de implementar as páginas e preencher `usuario.routes.ts`, registre em `app.routes.ts`:

```ts
{
  path: 'usuarios',
  loadChildren: () => import('./features/usuario/usuario.routes')
    .then((module) => module.usuarioRoutes),
}
```

Dentro de `usuario.routes.ts`, use `loadComponent` para carregar cada página. Deixe os testes `.spec.ts` junto do arquivo testado. Adicione uma pasta `state` à funcionalidade quando surgir necessidade de estado compartilhado complexo.

## API e SSR

O `UsuarioApiService` implementa criar, buscar por ID, atualizar e excluir, seguindo os endpoints existentes de `/api/usuarios`. A listagem fica reservada até existir um endpoint correspondente no backend. Os métodos retornam Observables do HttpClient; as páginas devem consumir os resultados e tratar os erros.

O HttpClient está registrado na configuração global. A URL é centralizada no token `API_URL`:

- Navegador: `src/environments/environment.ts`, por padrão `http://localhost:8080`.
- Servidor SSR: variável `API_URL`, por padrão a URL do environment. No Compose, recebe `http://api:8080`.

O environment é incorporado no build; alterar uma variável do contêiner não muda a URL usada pelo navegador. Para outro ambiente, configure a URL acessível pelo usuário antes do build. O backend aceita CORS de `http://localhost:4200`.

Evite acessar diretamente `window` e `localStorage` em código executado no servidor. Ao implementar novas rotas, ajuste `app.routes.server.ts` para escolher entre prerenderização e renderização no servidor. Rotas dinâmicas ou com dados privados precisam de tratamento próprio; atualmente existe apenas a configuração inicial de prerenderização.

## Executar

Com Docker, na raiz do repositório:

```sh
docker compose -f docker/compose.yaml up --build -d
```

Para atualizar somente o front depois de alterações:

```sh
docker compose -f docker/compose.yaml up --build -d --no-deps app
```

Para desenvolvimento local, dentro de `advocacia-app`, com Node compatível com o Angular instalado:

```sh
npm ci
npm start
```

Acesse http://localhost:4200. Para compilar:

```sh
npm run build
```

Para executar os testes existentes:

```sh
npm test -- --watch=false
```

A rota `/cadastro` é protegida por `cadastroGuard` e renderizada no cliente. Ela exige uma validação recente por código ou Google, mostra o email verificado como somente leitura e pede nome completo. Ao concluir, a API cria a conta, inicia a sessão e o frontend abre `/inicio`.

### Menu e painel após o login

A rota `/inicio` usa o layout autenticado baseado em `docs/index.html`: menu lateral musgo recolhível, grupos Principal/Relacionamento/Ferramentas/Gestão, cabeçalho com busca, avatar e saída, saudação e cartões iniciais. O menu pode ser fixado e a preferência é salva por usuário no navegador. Em telas pequenas, abre como um painel sobre o conteúdo.

Os itens e destinos ficam em `core/navigation/menu.model.ts`. As áreas ainda sem implementação usam uma página compartilhada “Em breve”. A busca do cabeçalho encontra áreas do menu; os indicadores e cartões permanecem vazios até a integração de dados. O painel não consulta APIs dos módulos. Login, sessão e saída usam a autenticação existente.

### Temas

Em Configurações → Tema, escolha Verde (original), Vermelho, Amarelo, Azul, Preto ou Roxo. As cores são aplicadas imediatamente ao menu, fundo, saudação e controles do ambiente autenticado. A preferência é salva no PostgreSQL por usuário e carregada pela API após o login, acompanhando a conta em outros navegadores e dispositivos. Ao sair, o visual volta ao verde; ao trocar de conta, a preferência do novo usuário é consultada. A escolha muda o visual imediatamente e só confirma salvamento após a resposta do servidor.

Paletas: `src/styles/_temas.scss`. Opções e persistência: `src/app/core/theme/`. Novos componentes devem usar os tokens CSS `--tema-primaria`, `--tema-menu`, `--tema-saudacao`, `--tema-clara` e `--tema-fundo` para acompanhar a escolha.

### Configurações

A rota autenticada `/config` reproduz a organização de Configurações de `docs/index.html` em componentes Angular. As abas seguem a referência: Meu perfil, Empresa, Equipe, Documentos, Cadastros e sistemas, Workflows, Financeiro, Olívia e Senhas. Tema fica por último. A seção selecionada aparece em `?secao=empresa`, por exemplo, e permite recarregar a página ou usar voltar/avançar.

Os formulários permitem editar o perfil, dados da empresa, imagens da identidade visual, papel timbrado e modelos de documentos; cadastrar listas, rotinas e atalhos; organizar etapas e gatilhos de workflows; e configurar contas, categorias, centros de custos, URH, custos e capacidade produtiva. Os campos e textos de empresa e documentos foram extraídos da referência em `features/configuracoes/models/referencia.ts`. Estilos compartilhados ficam em `src/styles/_configuracoes.scss`, com colunas responsivas conforme a largura do conteúdo.

Meu perfil já usa o backend: telefone, e-mail de contato, endereço e foto são consultados e salvos no PostgreSQL, por meio de `MeuPerfilApiService`. O e-mail de login não muda por essa tela. Os formulários aguardam a consulta antes de permitir alterações e só confirmam o salvamento após a resposta do servidor. A foto é reduzida para JPEG de até 512 pixels; o servidor verifica o conteúdo, dimensões e limite de 512 KB. O avatar do cabeçalho e a ficha do próprio usuário em Equipe usam o mesmo perfil. Cargo, vínculo, férias e histórico ainda aguardam a etapa administrativa.

Empresa também usa o backend. `EmpresaApiService` consulta `/api/empresas/minha`; uma conta sem vínculo cadastra a empresa por POST e torna-se MASTER. Depois, usa PUT para atualizar os dados. MEMBRO visualiza o formulário sem permissão de edição. A tela verifica campos obrigatórios, limites e erros do servidor, e não confirma o salvamento antes da resposta. O contexto da empresa é limpo ao trocar de conta. A prévia de documentos utiliza os dados dessa mesma empresa. Identidade visual também usa o backend: GET/PUT `/api/empresas/minha/identidade`. O MASTER publica imagens, usos e recorte pelo botão Salvar identidade visual; MEMBRO consulta com os controles bloqueados. O link de entrada do escritório carrega somente nome e marca pelo endpoint público, sem conceder vínculo. Os arquivos são preparados como PNG/JPEG de até 1400 pixels; o backend verifica limite de 2 MB por imagem. Alterações não publicadas permanecem no formulário e não são gravadas no localStorage.

Equipe já usa o banco e o SMTP. `EquipeApiService` lista membros e convites da própria empresa, salva a ficha e permissões e permite enviar, reenviar e cancelar convites. As alterações de cada ficha são rascunhos até clicar em Salvar membro; a resposta do servidor confirma o resultado. Convites expiram em 7 dias e são aceitos no primeiro login com e-mail verificado, por código ou Google. Inativo e Suspenso revogam o acesso; o MASTER não é editável por essa tela. Administrador é um perfil de módulos e não concede a função MASTER.

`AcessoService` carrega `/api/equipe/me`; `moduloGuard` verifica permissões atuais nas rotas. O menu, busca, atalhos e abas de Configurações seguem o acesso retornado. Membros veem apenas Meu perfil, Empresa (consulta) e Tema em Configurações; o MASTER vê as abas administrativas. Início e Configurações ficam acessíveis mesmo com restrição personalizada. Os prefixos das APIs também são protegidos no backend, incluindo a permissão de assinatura em `/api/documentos/assinaturas`.

As demais abas continuam como frontend. `ConfiguracoesLocalService` salva rascunhos em `localStorage`, separados pela conta autenticada; esses rascunhos não são compartilhados entre pessoas ou dispositivos. Conexões externas, notícias e sincronizações aguardam integração com APIs. A aba Senhas usa o cofre criptografado do backend e não grava credenciais no navegador. Imagens são limitadas a 2 MB e as imagens raster são redimensionadas antes de guardar.

Rascunhos antigos de Meu perfil, Empresa e Equipe no navegador não são importados automaticamente para o banco. Preencha os dados e salve para iniciar o registro persistente; convites antigos não são enviados automaticamente.

Configurações não importa React nem se conecta ao Supabase usado na referência. Login, sessão, vínculo empresarial e permissões usam a API Spring. A persistência dos demais rascunhos permanece local até suas respectivas integrações.

Cadastros e sistemas também usa o servidor: `CadastrosApiService` consulta `/api/config/cadastros`, cria/edita/exclui itens e rotinas e salva os sistemas em lote. Formulários só confirmam sucesso após a resposta e preservam os rascunhos em falhas. Relações entre ação/grupo e tarefa ou etapa/fase usam UUIDs, apresentados como nomes nos selects. Apenas MASTER edita. O Início consulta `/api/config/cadastros/inicio`, exibe rotinas por período e adiciona os sistemas habilitados aos atalhos; essa consulta não carrega todas as listas. Workflows consulta e salva no servidor pelo WorkflowsApiService. Integrações externas, sincronizações, curadoria de notícias e marcações de conclusão das rotinas ficam para suas próprias etapas.


Documentos agora consulta e salva por DocumentosApiService em /api/config/documentos. Papel timbrado e modelos possuem PUT separados, edição exclusiva do MASTER, estados de carregamento e erro, e preservação do rascunho em falhas. Os editores contenteditable também ficam bloqueados para membros. Textos originais continuam como padrão quando não há personalização no banco; nenhum rascunho antigo do navegador é importado. A prévia usa o formulário atual e os dados da empresa. Emissão real, PDF e assinaturas permanecem para etapas futuras.

Workflows usa GET/POST `/api/config/workflows` e PUT/DELETE `/api/config/workflows/{id}`. A tela consulta tipos de tarefa e responsáveis da própria empresa, apresenta os nomes mantendo vínculos por UUID e salva cada workflow separadamente, sem sobrescrever os rascunhos dos demais. Etapas sem responsável enviam null. Somente MASTER edita; controles ficam bloqueados durante carregamento e envio. Erros preservam o formulário e versões desatualizadas retornam conflito. A configuração é compartilhada pela empresa; a execução automática e o cálculo de datas aguardam integração com tarefas.

Financeiro também persiste no servidor: `FinanceiroApiService` consulta `/api/config/financeiro` e salva contas, categorias, centros, custos, URH e capacidade por endpoints separados. `FinanceiroConfig` bloqueia escrita durante carga/envio e para contas sem empresa ou MEMBRO, confirma somente após resposta e conserva formulários em erros. A versão compartilhada da configuração impede sobrescritas entre sessões. Salvar URH não envia capacidade, e adicionar/excluir custos não substitui os parâmetros em edição. Contas padrão, ativações e exclusões só mudam na tela após confirmação. A tela não importa nem grava financeiro no localStorage. Indicadores continuam como prévias; lançamentos, cobranças e calculadora aguardam seus módulos.

`TemaApiService` usa GET/PUT `/api/usuarios/me/tema`; `TemaService` carrega a cor no ambiente autenticado, cancela a carga/fila da conta anterior e envia alterações rápidas em sequência para manter a última escolha no banco. Alterar nome ou perfil sem trocar o usuário não reinicia o tema. A seção Aparência distingue carregamento, salvamento, confirmação e falha com botão Tentar novamente. Em falha de gravação, a prévia fica aplicada na sessão, mas não é apresentada como persistida. Temas antigos do localStorage não são importados e a tela não lê nem grava mais essa chave.

Senhas usa `CofreApiService` e `/api/config/cofre`. O frontend recebe metadados filtrados por empresa/visibilidade; somente MASTER edita. Membros com empresa também acessam essa aba, com edição bloqueada. A senha só chega por POST `/revelar` após clicar em Mostrar senha, fica em memória por 30 segundos e é removida ao ocultar a aba ou destruir o componente. Editar não revela a senha: um campo vazio envia null para manter o valor armazenado. A tela preserva formulários em falhas e confirma salvamento/exclusão após resposta. Configuração e backup da chave de criptografia estão documentados no README da raiz.
