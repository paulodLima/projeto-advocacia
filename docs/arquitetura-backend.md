# Arquitetura do backend

O projeto mantém módulos por funcionalidade. A organização interna adotada nos módulos de empresas e administração é:

```text
funcionalidade/
  api/
    Controller.java
    request/       # DTOs HTTP de entrada, com validação
    response/      # DTOs HTTP de saída e conversão de domínio
  application/
    Service.java   # Casos de uso e limites de transação
    command/       # Dados de entrada dos casos de uso
  domain/
    Modelo.java    # Modelos e regras sem Spring/JPA
    repository/    # Contratos de persistência
  infrastructure/
    persistence/
      entity/      # Entidades JPA e conversão entidade/domínio
      repository/  # Spring Data e implementação dos contratos
```

O controller recebe e valida o HTTP, converte o request para um comando, chama o serviço e converte o resultado para response. Não executa SQL, não utiliza entidade JPA e não define transações. Records são arquivos próprios, mantendo requests separados de responses.

Os serviços de `escritorio` e `plataforma` utilizam contratos de domínio. Os adaptadores traduzem esses contratos para Spring Data ou JDBC. A transação do serviço mantém a gravação da empresa e a auditoria atômicas. O bloqueio do provisionamento inicial continua no banco, encapsulado pelo adaptador JDBC.

`Empresa` e `VinculoEscritorio` pertencem ao domínio de `escritorio`, responsável pelo contexto e pelos vínculos. A administração global é um caso de uso de `plataforma`, que usa esse domínio. Autenticação HTTP e sessões permanecem no módulo `auth`; consultas de contas passam por serviço e pelo contrato de usuário. Componentes técnicos de sessão e limite de solicitações são adaptadores de autenticação.

A separação de entidades e repositories foi aplicada também à persistência existente de `auth` e `usuario`. O contrato já existente `usuario.domain.UsuarioRepository` foi preservado e ganhou consulta por email. A autenticação anterior por código/Google ainda utiliza os adaptadores próprios de códigos e identidades; esta reorganização não redesenha esse mecanismo.

## Verificação

`ArchitectureTests` impede DTOs internos nos controllers e imports de persistência/SQL nas APIs. Também verifica que aplicação e domínio de empresas/administração respeitam a separação e que entidades e repositories JPA estão nos pacotes próprios. A suíte funcional verifica cadastro, edição, CNPJ, contexto, auditoria, provisionamento e autorização.

Após mover classes, executar `mvn clean test`: uma compilação incremental pode deixar classes dos pacotes antigos em `target` e gerar conflito de beans. Migrations e esquema do banco não foram alterados pela refatoração.

Validação da refatoração em 06/10/2026: 39 testes aprovados; API reconstruída e iniciada com PostgreSQL no esquema V8; endpoint de provedores respondeu 200 e administração sem sessão respondeu 401. Tarefa técnica PLAT-ARQ01 e histórico registrados no Notion. Alterações locais na branch `feature/adv-002-perfis-permissoes`, sem commit/PR.
