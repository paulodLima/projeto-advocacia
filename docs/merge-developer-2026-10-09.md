# Merge da developer — 09/10/2026

- Branch: `feature/adv-002-perfis-permissoes`.
- Origem atualizada: `origin/developer`, commit `d5984c3`.
- Merge: `5d3934b`, usando `-X theirs` para priorizar a developer nos trechos conflitantes.
- Alterações locais anteriores preservadas, incluindo arquivos não rastreados, em `stash@{0}`: `backup-local-antes-merge-developer-2026-10-09`. Não reaplicadas para evitar sobrepor o código recebido.
- Nenhum push realizado.

## Leitura do código recebido

Configurações do escritório, equipe e convites, identidade visual, temas, perfil, cadastros auxiliares, documentos, workflows, financeiro e cofre; gestão de contatos e parceiros com filtros, permissões e carteira de clientes. Parceiros compartilham a identidade do contato e usam controle de versão nas edições. O contexto das novas funcionalidades vem do vínculo do usuário no backend.

Consultados `docs/index.html`, o planejamento Projeto Advocacia no Notion e os serviços de cadastro, empresa, contatos, parceiros e autenticação.

## Validações e pendências

- Confirmado que `origin/developer` é ancestral de HEAD; merge encerrado sem conflitos Git pendentes.
- `git diff --check` sem erros.
- Restaurado import de `adminPlataformaGuard`, perdido pela resolução automática de trechos.
- `mvn clean test` falhou na compilação: referências ao pacote anterior de `JpaUsuarioRepository`, além de símbolos e junção inconsistente no `AuthController`. Os testes não chegaram a executar.
- Build Angular, executado fora do sandbox após `spawn EPERM`, falhou: template do layout usa `plataforma` e `escritorios`, ausentes na classe recebida.
- Migrations têm versões V5, V6, V7 e V8 duplicadas entre as branches. Há modelos paralelos de empresa/escritório, vínculos e permissões; integração funcional ainda não validada.
- O serviço de empresa recebido permite criação por usuário sem vínculo, divergindo da regra de administração central do planejamento. A rota de seleção de escritório também foi retirada pela developer.
- Não alteradas migrations nem conciliadas regras de negócio nesta sessão de merge.

## Próximo passo

Receber a alteração de cadastro desejada pelo usuário. Conciliar os modelos, rotas, migrations e erros de compilação antes da validação integrada. O trabalho anterior de módulos e usuários está preservado no stash e exige conciliação para ser reaplicado.
