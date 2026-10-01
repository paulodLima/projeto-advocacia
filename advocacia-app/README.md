# Frontend de Advocacia

Angular com componentes standalone e suporte a SSR, organizado por funcionalidade.

## Identidade visual por empresa

A identidade padrão do produto é **Gestão Advocacia**, com monograma GA em `public/images/logo-ga.svg`. Nome, logo e imagem do login são consumidos pelo `IdentidadeEmpresaService`, em `core/branding`, e têm um contrato próprio `IdentidadeEmpresa`. A foto atual permanece somente como imagem padrão.

Quando houver cadastro de empresas no backend, a identidade retornada para a empresa identificada deverá ser aplicada com `IdentidadeEmpresaService.aplicar(...)`. Dados ausentes retornam aos padrões do produto; ao trocar de empresa, dados da marca anterior não são reaproveitados. Imagens que não carregarem também usam o padrão.

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

Pastas reservadas usam `.gitkeep` para serem preservadas no Git. A página `/login` usa o visual de `docs/index.html`, com acesso principal por código enviado ao email. Após validar, abre `/inicio`, protegida por guard que verifica a sessão no backend. A raiz redireciona para `/login`. O Google aparece somente como ícone secundário, aguardando integração OAuth. As demais funcionalidades ainda estão reservadas.

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
