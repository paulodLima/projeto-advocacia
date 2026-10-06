# Etapa 2 — Cadastro de empresas

Acesse http://localhost:4200/administracao/empresas com um administrador da plataforma. O painel também oferece o botão Gerenciar empresas. Esta etapa entrega cadastro, lista paginada (20 por página) e ficha com edição.

## Regras

- Nome obrigatório, até 150 caracteres.
- Razão social obrigatória, até 150 caracteres. Nome representa nome fantasia/de exibição.
- CNPJ obrigatório e único, numérico ou alfanumérico, normalizado sem pontuação e validado pelos dígitos verificadores. A validação não consulta situação cadastral na Receita Federal.
- Quantidade de funcionários obrigatória, inteira e não negativa; não representa limite de usuários ou licenças.
- Email, telefone, endereço (logradouro, número, bairro e complemento), cidade, UF e CEP opcionais.
- Código interno obrigatório e único: 2 a 80 letras minúsculas, números ou hífens. Exemplo: empresa-a. É separado do CNPJ.
- Situação ativa/inativa. Empresa inativa não pode ser selecionada nem usada como contexto por seus membros.
- Administração global é exclusiva dos administradores da plataforma. Cadastro não cria vínculo de proprietário para o administrador, usuários ou liberações de módulos.
- Cadastro e edição registram autor, empresa e ação na auditoria, na mesma transação.
- V7 preserva escritórios existentes com código empresa-UUID e situação ativa. A API pública antiga permanece bloqueada.
- V8 acrescenta dados cadastrais sem inventar CNPJ ou número de funcionários para empresas anteriores. Completar esses campos ao editar suas fichas.

## Roteiro para validar com sua conta

1. Entrar com o email administrativo e abrir Empresas → Nova empresa.
2. Cadastrar Empresa A com razão social, nome fantasia, CNPJ válido, quantidade de funcionários, código empresa-a e situação Ativa. Conferir na lista.
3. Cadastrar Empresa B com código empresa-b. Conferir ambas.
4. Abrir a ficha de A, editar o nome, salvar e recarregar o navegador. Conferir persistência.
5. Tentar cadastrar outra empresa com o mesmo CNPJ ou código empresa-a. Conferir erro de duplicidade, sem criação parcial.
6. Deixar campos obrigatórios vazios; testar CNPJ inválido e funcionários negativos ou fracionados. Conferir erro e ausência de gravação.
7. Abrir em celular e conferir formulário, lista e navegação.

## Evidências em 06/10/2026

Backend: 32 testes passaram, incluindo quatro novos testes de empresas (cadastro/edição/auditoria sem vínculo, acessos negados/CSRF, validação/duplicidade, contexto de empresa inativa). Frontend: 13 testes existentes passaram e build de produção aprovado. Docker atualizado; PostgreSQL 17 aplicou V7 com sucesso.

Chrome com API simulada: duas empresas, duplicidade, formulário vazio, edição e recarga, layout desktop/celular e bloqueio de usuário comum passaram. Essa recarga comprova a leitura da API simulada; persistência real foi verificada nos testes de backend, não em login real no navegador. Nenhuma empresa fictícia foi gravada no banco local pelo roteiro visual. Capturas: etapa2-empresas-preview.png e etapa2-empresa-mobile-preview.png.

Pendente: validação real pelo responsável com sua conta no ambiente local. PLAT-T02 permanece Em revisão. Próxima etapa: módulos liberados por empresa. Branch feature/adv-002-perfis-permissoes; sem commit/PR.

## Ampliação dos dados empresariais

Após esclarecimento do responsável, incluídos os dados fiscais, funcionários, contato e endereço nas regras acima. A migração V8 está aplicada no PostgreSQL. A suíte ampliada passou com 35 testes backend e 13 frontend; build aprovado. Capturas e roteiro visual com API simulada atualizados para o novo formulário. CNPJ alfanumérico segue o [manual oficial de dígitos verificadores](https://www.gov.br/receitafederal/pt-br/centrais-de-conteudo/publicacoes/documentos-tecnicos/cnpj/manual-dv-cnpj.pdf). Permanecem pendentes login real e validação do responsável.
