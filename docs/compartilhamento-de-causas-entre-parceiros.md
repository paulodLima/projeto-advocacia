# Compartilhamento de causas entre empresas parceiras

## Objetivo da proposta

Permitir que duas empresas de advocacia que utilizam o Gestão Advocacia trabalhem juntas em uma causa, com acesso autorizado aos dados necessários e mantendo separados os demais cadastros de cada escritório.

Essa funcionalidade pode transformar o módulo de Parceiros em uma ferramenta de colaboração: além de registrar quem é o parceiro, o sistema passa a organizar o trabalho realizado com ele.

**Status:** proposta para uma etapa futura. O cadastro de parceiros e sua relação com a carteira de contatos já existem. Vinculação entre empresas, convites e compartilhamento de causas ainda não estão implementados.

## Por que isso seria interessante

Uma causa pode precisar da participação de profissionais de diferentes especialidades ou localidades. Um escritório pode receber a demanda, outro contribuir com conhecimento específico e um terceiro realizar uma atividade local.

Sem um espaço compartilhado, os envolvidos precisam trocar arquivos, combinar tarefas e atualizar o andamento por canais separados. Isso pode gerar cópias desatualizadas, mensagens difíceis de encontrar e dúvidas sobre quem está responsável por cada atividade.

O compartilhamento permite reunir essas informações no sistema que os escritórios já usam. A empresa parceira encontra a causa autorizada, acompanha as atualizações permitidas e participa das atividades atribuídas a ela.

## Exemplo prático

O Escritório A atende uma cliente em uma demanda de Família que também exige atuação especializada em Direito Empresarial. O Escritório B, cadastrado como parceiro, possui essa especialidade e também utiliza o Gestão Advocacia.

O Escritório A seleciona a causa, convida o Escritório B e permite consultar determinados documentos e colaborar em tarefas. Após aceitar o convite, o Escritório B passa a acessar essa causa em sua própria conta.

O Escritório A continua responsável pelo cadastro original. O Escritório B não recebe acesso às outras causas, à carteira completa de clientes ou às informações internas do Escritório A.

## Vantagens para os escritórios

| Vantagem | Benefício esperado |
| --- | --- |
| Informação atualizada | Os participantes consultam os mesmos dados autorizados, reduzindo a circulação de versões diferentes. |
| Responsabilidades claras | Tarefas podem indicar quem fará cada atividade e a qual escritório esse responsável pertence. |
| Colaboração por especialidade | O escritório pode trabalhar com parceiros de áreas que complementam sua atuação. |
| Atuação em outras localidades | Parceiros locais podem colaborar em atividades específicas, dentro das permissões concedidas. |
| Continuidade do trabalho | Documentos e registros compartilhados ficam associados à causa, facilitando a retomada do contexto. |
| Rastreabilidade | Um histórico identifica quem alterou uma informação, de qual empresa e quando. |
| Controle do acesso | O escritório escolhe o que compartilhar e pode encerrar a participação do parceiro. |

Esses benefícios dependem de permissões bem definidas e do uso consistente do sistema pelos participantes.

## Vantagens para o produto

O Gestão Advocacia pode oferecer uma experiência que acompanha o trabalho entre escritórios, além da organização interna de cada empresa.

Uma empresa que já usa o sistema poderá convidar um parceiro para colaborar em uma causa. Isso pode incentivar a adoção por novos escritórios, pois o convite terá uma finalidade concreta: participar de um trabalho em andamento. Esse efeito é uma oportunidade de crescimento, não uma garantia de aquisição de usuários.

Também cria uma conexão útil entre os módulos:

- **Parceiros:** identifica os profissionais e escritórios com quem a empresa colabora.
- **Casos:** organiza a demanda e as atividades anteriores à criação de um processo.
- **Gestão Processual:** acompanha os processos e a participação de cada parceiro.
- **Documentos e Agenda:** apoiam a troca de documentos e a execução de tarefas autorizadas.
- **Financeiro:** poderá registrar acordos de participação, honorários e repasses em uma etapa posterior.

## Como poderia funcionar

1. **Identificar a empresa parceira.** O cadastro do parceiro poderá ser vinculado a uma empresa usuária do sistema. Cadastrar um nome ou informar um e-mail, por si só, não concede acesso.
2. **Escolher uma causa.** Um usuário autorizado da empresa proprietária selecionará o caso ou processo que deseja compartilhar.
3. **Definir o acesso.** Antes de enviar o convite, ele escolherá os dados e as ações permitidas.
4. **Enviar o convite.** A empresa convidada poderá consultar o convite e aceitar ou recusar a parceria. Os dados da causa só serão liberados após o aceite.
5. **Escolher os participantes.** A empresa convidada indicará os membros que poderão atuar, dentro dos limites concedidos pela proprietária.
6. **Colaborar e acompanhar.** Os participantes acessarão os conteúdos autorizados, com identificação do escritório e registro das ações.
7. **Encerrar ou revogar.** Um usuário autorizado poderá encerrar a parceria ou retirar o acesso, mantendo o histórico necessário para compreender o trabalho realizado.

## Permissões sugeridas

O compartilhamento deve ser concedido por causa, com permissões explícitas. Participar de uma parceria não deve liberar acesso geral à empresa proprietária.

| Conteúdo ou ação | Possibilidade de autorização |
| --- | --- |
| Dados da causa | Consultar os dados necessários ao trabalho conjunto. |
| Documentos | Consultar documentos selecionados; envio de novos documentos pode ser uma permissão separada. |
| Tarefas | Consultar e atualizar tarefas atribuídas à parceria. |
| Movimentações | Consultar registros compartilhados e, quando autorizado, adicionar novos registros. |
| Anotações internas | Permanecer privadas, salvo compartilhamento explícito. |
| Honorários e repasses | Compartilhar apenas os dados do acordo da parceria, quando essa integração existir. |
| Convites e encerramento | Reservar a usuários autorizados; acesso à causa não implica poder convidar outras empresas. |

A autorização da empresa convidada também deve considerar as permissões de cada membro. O sistema deve verificar tanto a concessão entre empresas quanto o acesso individual do usuário.

## Separação entre empresas

A causa deve manter uma empresa proprietária. O convite cria uma relação de acesso; não transfere a propriedade nem exige copiar a causa para o banco de outra empresa.

Essa abordagem evita que duas cópias da mesma causa evoluam de forma diferente. Ainda assim, cada empresa deve poder manter suas anotações e atividades internas sem expô-las automaticamente ao parceiro.

O backend precisa verificar a autorização em cada consulta e alteração, inclusive no acesso direto a documentos. Esconder opções no frontend não é suficiente para proteger dados entre empresas.

Ao revogar o compartilhamento, novas consultas, alterações e downloads devem ser bloqueados. Arquivos já baixados pelo parceiro não podem ser recolhidos automaticamente; esse limite precisa ficar claro no fluxo do produto.

## Cuidados para a funcionalidade ser útil

- Identificar claramente quem é a empresa proprietária e quais empresas participam.
- Diferenciar informações internas de informações compartilhadas.
- Mostrar ao usuário o alcance do acesso antes de confirmar um convite.
- Evitar convites duplicados e impedir que um parceiro amplie o acesso recebido por conta própria.
- Registrar convites, aceites, alterações de permissão e revogações no histórico.
- Definir o que acontece quando um participante sai da equipe ou tem sua conta desativada.
- Validar as regras de confidencialidade e tratamento dos dados antes de disponibilizar o recurso em produção.

## Primeira entrega recomendada

Começar com compartilhamento de **Casos**, depois que esse módulo possuir cadastro, ficha e persistência próprios. A primeira entrega pode incluir:

1. Vinculação do parceiro a uma empresa do sistema.
2. Convite, aceite ou recusa e revogação por caso.
3. Consulta dos dados autorizados e de documentos selecionados.
4. Definição dos membros participantes e histórico das ações de compartilhamento.

Depois, ampliar para tarefas, colaboração na edição, processos e acordos financeiros. Assim, cada etapa pode ser validada com um fluxo completo e permissões compreensíveis.

## Relação com o layout atual

As telas de Parceiros, Casos e Gestão Processual devem continuar seguindo o layout de `docs/index.html`. O compartilhamento pode ser incorporado à ficha da causa como uma ação e uma seção de participantes, preservando a linguagem visual existente.

O layout dessa extensão ainda precisará ser definido, pois o cadastro de um parceiro não equivale ao fluxo de convite entre empresas.
