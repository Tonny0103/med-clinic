<!--
Sync Impact Report — material temporário de revisão; remover antes do commit.
Versão: scaffold sem versão → 1.0.0 (adoção inicial).
Princípios modificados: cinco slots genéricos substituídos por Qualidade e camadas;
Test-first e cobertura; Experiência e acessibilidade; Desempenho; Segurança.
Seções adicionadas: Restrições de entrega; Fluxo de desenvolvimento e revisão;
Governance preenchida com emendas, versionamento e conformidade.
Seções removidas: nenhuma seção normativa; apenas instruções do scaffold.
TODOs: nenhum. Templates e comandos dependentes não foram alterados.
-->
# Med Clinic Constitution

## Core Principles

### I. Qualidade de código e arquitetura em camadas

O sistema DEVE separar domínio, aplicação, infraestrutura e apresentação. As dependências
DEVEM apontar para as camadas internas. Regras de negócio NÃO DEVEM depender de frameworks,
interfaces, persistência ou serviços externos. Esses recursos DEVEM ser acessados por contratos
internos e adaptadores externos. O domínio DEVE ser testável sem inicializar infraestrutura.

O código DEVE ter responsabilidades delimitadas, nomes claros e contratos explícitos.
Regras de negócio NÃO DEVEM ser duplicadas. Formatação, análise estática e revisão de código
DEVEM ser obrigatórias para integração. Complexidade adicional DEVE ter justificativa no plano
ou na revisão. Esses limites preservam a manutenibilidade e a independência tecnológica.

### II. Test-first obrigatório e cobertura mínima

Toda alteração de comportamento DEVE seguir o ciclo: escrever o teste, executá-lo e observar
falha pelo comportamento ausente, implementar o mínimo para passar e refatorar com testes
aprovados. Correções DEVEM começar com teste que reproduza o defeito. A revisão DEVE registrar
evidências da falha inicial e da execução aprovada.

A cobertura automatizada DEVE atingir no mínimo 80% de linhas e de ramificações do código de
produção, tanto no total quanto em cada módulo alterado. Exclusões de código gerado ou de
terceiros DEVEM ser explícitas e revisadas. Código de negócio NÃO DEVE ser excluído para elevar
a métrica. A integração contínua DEVE bloquear resultados abaixo desses limites.

Regras de negócio DEVEM ter testes unitários; adaptadores e persistência DEVEM ter testes de
integração; contratos de API novos ou alterados DEVEM ter testes de contrato. Jornadas críticas
DEVEM ter testes de ponta a ponta. Testes DEVEM ser determinísticos e isolados. Cobertura
numérica NÃO DEVE substituir asserções sobre comportamento e cenários de erro.

### III. Experiência consistente e acessibilidade

Toda interface DEVE atender aos critérios aplicáveis dos níveis A e AA da WCAG 2.1.
Componentes e jornadas DEVEM oferecer navegação por teclado, foco visível e ordenado,
semântica e nomes acessíveis, contraste adequado e identificação compreensível de erros.
Informação NÃO DEVE depender exclusivamente de cor. Mensagens de validação DEVEM estar
associadas aos campos e orientar a correção sem perder os dados válidos já informados.

Interfaces DEVEM reutilizar componentes, terminologia, navegação e estilos compartilhados.
Estados de carregamento, vazio, sucesso e erro DEVEM ter tratamento consistente.
Toda interface nova ou alterada DEVE passar por verificação automatizada de acessibilidade
 e avaliação manual com teclado e leitor de tela nas jornadas afetadas. As evidências DEVEM
ser registradas na revisão. Violações dos níveis A ou AA NÃO DEVEM ser liberadas.

### IV. Desempenho verificável

Cada operação da API DEVE apresentar latência p95 estritamente inferior a 300 ms, medida
entre o recebimento da requisição na entrada do serviço e o término da resposta, incluindo
processamento e dependências chamadas nesse intervalo. A avaliação DEVE ocorrer por operação;
agregações NÃO DEVEM ocultar operações lentas.

O plano DEVE definir ambiente equivalente ao de produção, volume de dados, concorrência,
mistura de requisições, aquecimento, duração e tamanho de amostra representativos da carga
esperada. Esses parâmetros DEVEM ser aprovados antes da implementação. Mudanças que afetem
a API DEVEM apresentar teste de carga reproduzível nessas condições, com p95 e taxa de erros.
Falhas e respostas incompletas NÃO DEVEM ser usadas para simular atendimento à meta.

A produção DEVE ter métricas por operação e alertas para p95 igual ou superior a 300 ms.
A janela de observação DEVE ser definida no plano. Violações DEVEM gerar registro, responsável
e ação corretiva. Entregas que violem a meta nos testes NÃO DEVEM ser liberadas.

### V. Segurança em todas as fronteiras

Segredos, credenciais, tokens e chaves privadas NÃO DEVEM constar no repositório, em fixtures,
exemplos ou logs. Segredos DEVEM ser fornecidos em tempo de execução por gestão externa de
segredos. Exemplos DEVEM usar valores fictícios sem validade. A integração contínua DEVE
verificar segredos e bloquear ocorrências. Segredos expostos DEVEM ser revogados ou rotacionados
imediatamente e removidos do histórico pelo procedimento de incidente; apagar somente o arquivo
atual NÃO DEVE ser considerado correção suficiente.

Toda fronteira de confiança DEVE validar entradas antes do uso: APIs, formulários, mensagens,
arquivos, configuração e respostas de integrações externas. A validação DEVE verificar esquema,
tipo, tamanho, formato, limites e invariantes aplicáveis. Entradas inválidas DEVEM ser rejeitadas
com erro controlado. Validação no cliente NÃO DEVE substituir validação no servidor. Acesso a
recursos DEVE verificar autenticação e autorização aplicáveis; consultas DEVEM usar parametrização.
Logs NÃO DEVEM expor segredos ou dados sensíveis.

## Restrições de entrega

Toda funcionalidade DEVE ter `spec.md` e `plan.md` aprovados antes de qualquer implementação,
incluindo código de produção e testes. A aprovação DEVE ser explícita e registrada em artefato
versionado ou revisão vinculada, com aprovador, data e revisão dos documentos aprovados.
O responsável pelo produto DEVE aprovar a spec; o responsável técnico DEVE aprovar o plano.
Uma pessoa DEVE poder exercer ambos os papéis quando isso estiver registrado no projeto.

A spec DEVE definir comportamento, critérios de aceitação e jornadas afetadas. O plano DEVE
definir camadas, contratos e estratégias de testes, cobertura, acessibilidade, segurança e
desempenho. Requisitos não aplicáveis DEVEM ter justificativa explícita revisada; essa classificação
NÃO DEVE dispensar requisito aplicável desta constituição. Mudanças materiais de escopo ou solução
DEVEM atualizar os documentos e obter nova aprovação antes da implementação do trecho afetado.
Urgência NÃO DEVE eliminar esses requisitos.

## Fluxo de desenvolvimento e revisão

1. A equipe DEVE escrever e aprovar spec e plano, registrando a verificação constitucional.
2. As tarefas DEVEM derivar dos documentos aprovados e identificar testes e critérios de aceite.
3. A implementação DEVE seguir test-first e preservar os limites entre camadas.
4. Cada revisão DEVE vincular spec, plano, aprovações e evidências dos requisitos aplicáveis.
5. A integração contínua DEVE executar formatação, análise estática, testes, cobertura,
   verificação de segredos e verificações automatizadas de acessibilidade aplicáveis.
6. A revisão DEVE conferir arquitetura, validações nas fronteiras, experiência, acessibilidade
   manual e resultados de testes de desempenho aplicáveis.
7. Integração e liberação DEVEM ser bloqueadas por requisito obrigatório não atendido, aprovação
   ausente ou verificação obrigatória com falha. Correções DEVEM ser verificadas antes de prosseguir.

## Governance

Esta constituição DEVE prevalecer sobre práticas locais, templates, planos e decisões conflitantes.
Toda spec, plano e revisão DEVE registrar conformidade com a versão vigente. Os responsáveis pelo
produto e pela técnica DEVEM assegurar a aplicação das regras. Dispensas informais NÃO DEVEM
substituir o processo de emenda.

Toda emenda DEVE apresentar motivação, texto anterior e proposto, impacto, classificação de versão
e plano de migração dos artefatos afetados. A emenda DEVE obter aprovação explícita dos responsáveis
pelo produto e pela técnica antes de entrar em vigor. Aprovação e data de vigência DEVEM ser
registradas na revisão vinculada. Até a aprovação, a versão vigente DEVE continuar aplicável.

O versionamento DEVE seguir MAJOR.MINOR.PATCH: MAJOR para remoções ou redefinições incompatíveis
de princípios ou governança; MINOR para novos princípios, seções ou ampliação material compatível
das obrigações; PATCH para esclarecimentos e correções sem mudança normativa. Cada emenda DEVE
atualizar versão e data de última alteração, preservando a data de ratificação inicial.
Esta adoção inicial DEVE ser identificada como 1.0.0.

Cada emenda DEVE avaliar o impacto sobre specs, planos, tarefas, templates e comandos existentes.
Adequações necessárias DEVEM ser registradas com responsável e prazo e NÃO DEVEM autorizar
descumprimento das regras vigentes. A conformidade DEVE ser revista em toda aprovação de spec e
plano e em toda revisão de entrega. Alterações motivadas por exceções DEVEM seguir o mesmo
processo de emenda.

**Version**: 1.0.0 | **Ratified**: 2026-10-23 | **Last Amended**: 2026-10-23
