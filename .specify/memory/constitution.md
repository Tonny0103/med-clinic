# Constituição do Med Clinic

## I. MVP de demonstração
O projeto é um monólito Java com Spring Boot e Thymeleaf para apresentar a uma pessoa conhecida. Manter as oito histórias de gestão de pacientes, com dezenas de registros fictícios e poucos usuários. Priorizar uma implantação pequena em uma VM. Dados reais, operação clínica e certificações não fazem parte desta entrega.

## II. Código simples e organizado
Separar domínio, aplicação, infraestrutura e apresentação. Regras de negócio não dependem de Spring ou persistência; contratos internos isolam adaptadores. Usar nomes claros, quatro espaços no Java e fragmentos compartilhados na interface. Evitar serviços externos e abstrações sem necessidade demonstrada.

## III. Verificação proporcional
Manter poucos testes essenciais de identidade, permissões, preservação dos vínculos e transações. Ao alterar regras críticas, escrever o teste antes da implementação e observar a falha. Corrigir testes que falharem. Não há percentual mínimo de cobertura, obrigação de suítes extensas, homologação separada ou teste de carga. Complementar com roteiro manual curto para as oito histórias.

## IV. Interface e segurança
Usar HTML semântico, labels, mensagens de erro compreensíveis, foco visível e navegação por teclado. Conferir os fluxos essenciais; não exigir laudo formal de acessibilidade para a apresentação.
Autenticar usuários, autorizar ações no servidor, manter CSRF e validar entradas. Arquivos ficam privados e são verificados antes do vínculo. Nenhum perfil pode ler conteúdo clínico neste módulo. Não versionar credenciais, dados de pacientes reais ou chaves privadas; não registrar dados sensíveis nos logs ou na auditoria.

## V. Operação pequena
Executar aplicação, PostgreSQL e verificação de arquivos na mesma VM quando hospedado. Dimensionar para dezenas de pacientes; confirmar usabilidade por navegação manual, sem SLO p95 ou monitoramento dedicado obrigatório. Segredos vêm do ambiente. Manter instruções de execução, cópia de segurança e recuperação simples.

## Entrega e governança
As instruções do usuário nesta conversa definem o escopo e autorizam a implementação. Não exigir aprovações separadas de produto/técnica, assinaturas, hashes de documentos ou aprovação de parâmetros de carga. Atualizar spec, plano e tarefas quando o escopo mudar. Marcar tarefas apenas quando concluídas e fazer um commit por tarefa, conforme solicitado.
Mudanças nesta constituição seguem MAJOR.MINOR.PATCH; mudanças incompatíveis de governança incrementam MAJOR. Esta versão substitui os gates corporativos de 1.0.0 por verificação proporcional ao MVP, por instrução explícita do usuário em 2026-10-24. Os artefatos da funcionalidade e AGENTS.md são adequados na mesma tarefa; templates são orientações e não reinstalam os gates removidos.

**Version**: 2.0.0 | **Ratified**: 2026-10-23 | **Last Amended**: 2026-10-24

