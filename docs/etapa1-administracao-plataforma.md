# Etapa 1 — Administração da plataforma

A área `/administracao` exige administrador da plataforma, separado do vínculo com empresas. O backend reconsulta o privilégio em cada requisição. Usuários comuns não podem criar empresas nem concluir cadastro público. Empresas, funcionalidades e usuários aparecem como próximas entregas, sem ações disponíveis nesta etapa.

## Provisionar o primeiro administrador

No `docker/.env` existente, configurar apenas estas variáveis, preservando os demais valores:

```dotenv
PLATAFORMA_ADMIN_EMAIL=seu-email@example.com
PLATAFORMA_ADMIN_NOME=Seu Nome
```

Recriar a API a partir da raiz:

```sh
docker compose -f docker/compose.yaml up -d --force-recreate api
```

O provisionamento cria ou utiliza uma conta ativa, concede o privilégio e registra auditoria em uma transação. Não cria vínculo com empresa. Email vazio não concede acesso; email inválido ou conta inativa impede o provisionamento. A operação acontece uma única vez: trocar a configuração não cria outro administrador nem restaura um privilégio revogado.

## Validar na tela

1. Acessar http://localhost:4200/login e entrar com o email provisionado, usando código ou Google configurado.
2. Conferir o redirecionamento para `/administracao`, os três cartões em preparação e o botão de sair.
3. Entrar com uma conta comum já cadastrada. Conferir que não existe opção de criar empresa e que `/administracao` redireciona para `/escritorios`.
4. Conferir desktop e celular. Os cartões serão implementados nas próximas etapas; a próxima é cadastro de empresas.

## Evidências em 06/10/2026

- Backend: 28 testes aprovados, incluindo provisionamento único, revogação e acessos negados.
- Frontend: 13 testes aprovados e build de produção aprovado.
- PostgreSQL 17: Flyway aplicou V6 com sucesso; contêineres atualizados.
- Chrome: navegação administrativa, acesso comum e acesso anônimo verificados com autenticação simulada. Capturas: `etapa1-admin-preview.png`, `etapa1-admin-mobile-preview.png`, `etapa1-usuario-preview.png`.
- Conta `rodrigocb2012@gmail.com` provisionada no ambiente local; consulta ao PostgreSQL confirmou usuário ATIVO e privilégio administrativo ativo. Pendente executar o login real. A tarefa permanece em revisão até essa validação.

## Outros administradores

O modelo permite vários administradores da plataforma. A configuração inicial serve somente para o primeiro; mudar o email não concede novos privilégios. Ainda não existe tela ou API para gerenciar os demais. A evolução prevista é permitir que um administrador autorizado cadastre ou selecione uma conta, conceda ou revogue esse acesso e registre a ação em auditoria. Deve impedir a remoção do último administrador ativo. Esse privilégio é distinto das permissões de usuários de empresas.

Branch: `feature/adv-002-perfis-permissoes`. Alterações locais, sem commit ou publicação remota.
