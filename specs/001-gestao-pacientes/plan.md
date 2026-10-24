# Implementation Plan: Cadastro e Gestão de Pacientes

**Branch Git atual**: `dev` | **Feature retornada pelo setup**: `001-gestao-pacientes` | **Date**: 2026-10-24 | **Spec**: [spec.md](spec.md)

**Input**: `specs/001-gestao-pacientes/spec.md`; instrução do usuário: Java com Spring Boot e Thymeleaf.

**Status**: design concluído para revisão; aprovações documentais de implementação ainda ausentes. O setup resolveu a feature persistida sem trocar a branch. O cabeçalho `main` da spec é histórico; nenhuma branch foi criada neste fluxo.

**Revisão do design**: sincronizada com as três respostas em `spec.md` → Clarifications → Session 2026-10-24. `tasks.md` já existe, mas representa o design anterior e deve ser regenerado por `$speckit-tasks` antes da implementação. Esta execução termina na fase 1 e preserva o backlog existente.

## Summary

Entregar o cadastro administrativo de uma clínica: identidade e prontuário permanente, busca, manutenção, responsáveis, convênios, consentimentos, documentos, mesclagem e auditoria. A solução será um monólito Spring Boot com Spring MVC e Thymeleaf, domínio Java puro, PostgreSQL transacional e arquivos privados. Agenda, prontuário clínico, gestão de contas e leitura clínica permanecem fora do escopo.

CPF/documento será protegido por reserva única no banco, incluindo origens mescladas; versões evitarão sobrescrita concorrente. Escrita e auditoria confirmarão na mesma transação; leitura protegida só será enviada após persistir seu evento. A primeira entrega exibirá «Histórico ainda não disponível» por um adaptador explícito, sem dependência de agenda.

Revogação de paciente inativo poderá incluir nova evidência administrativa no próprio formulário, com anexo e manifestação confirmados juntos, sem liberar upload avulso. Mesclagem exigirá prova administrativa válida de cada raiz, nome normalizado exatamente igual, nascimento idêntico e confirmação justificada do gerente, revalidados na confirmação. Consulta de convênio passado usará os períodos e a validade preservada de cada segmento, incluindo vínculos encerrados posteriormente.

## Technical Context

**Language/Version**: Java 21 LTS; Spring Boot 4.1.2 estável, verificado nas fontes oficiais em [research.md](research.md).

**Primary Dependencies**: Spring MVC, Thymeleaf 3.1 gerenciado pelo BOM, Spring Security/OAuth2 Client OIDC, Bean Validation, Spring Data JPA/Hibernate; SQL parametrizado explícito nos adaptadores para reservas, busca e locks. Flyway, driver PostgreSQL e bibliotecas de teste via BOM. Maven 3.9.x com wrapper versionado e versão exata fixada no scaffold.

**Storage**: PostgreSQL 18.6, Flyway como única autoridade de migração e Hibernate `ddl-auto=validate`. Arquivos em volume privado criptografado, fora de `static/`, sob chaves aleatórias e acesso por adaptador; ClamAV em serviço privado para inspeção. Banco e backups criptografados; TLS em transporte. Produção inicial com uma instância e volume persistente; distribuição horizontal exigirá rever o adaptador de arquivos.

**Testing**: JUnit Jupiter gerenciado pelo Boot, AssertJ/Mockito, ArchUnit, MockMvc, Testcontainers com PostgreSQL real, Playwright Java + axe-core, JaCoCo. Surefire `*Test`; execuções Failsafe separadas por nome/tag para integração, E2E e desempenho, conforme tabela abaixo. k6 para carga e Playwright para tempo percebido. Spotless com formatação Java de quatro espaços, Checkstyle e SpotBugs no `verify`; scanner de segredos na CI.

**Target Platform**: servidor Linux, JDK 21, PostgreSQL 18, volume privado e scanner; desenvolvimento Windows/PowerShell com Docker. Navegadores atuais Chromium, Firefox e WebKit. Fuso obrigatório por configuração IANA, inicialmente `America/Cuiaba`, sujeito à confirmação na implantação.

**Project Type**: aplicação web monolítica, HTML renderizado no servidor, CSS e JavaScript progressivo; contratos HTTP de formulários, sem API REST pública nesta entrega.

**Performance Goals**: p95 estritamente menor que 300 ms por operação HTTP, da entrada no serviço ao término da resposta, incluindo dependências. SC-001 exige todas as 1.000 buscas em até 2 s até exibição. Upload, scanner, download, auditoria e renderização não recebem dispensa.

**Constraints**: WCAG 2.1 A/AA, cobertura LINE e BRANCH ≥80% total e por módulo lógico alterado, test-first, autorização por operação e objeto, auditoria obrigatória, arquivos PDF/JPEG/PNG ≤10 MiB, nenhuma leitura clínica por este módulo e nenhum dado real nos testes.

**Scale/Scope**: uma clínica, 10.000 pacientes fictícios e dez usuários simultâneos na validação; oito jornadas da spec. Identidade OIDC e perfis são dependência de liberação. Políticas de tratamento, retenção e termos são condições anteriores a dados reais.

Todas as escolhas técnicas foram resolvidas na fase 0. Aprovações e provisionamento são gates de execução, não decisões técnicas implicitamente aprovadas.

## Constitution Check

Constituição aplicável: [1.0.0](../../.specify/memory/constitution.md). A permissão para planejar não é aprovação de spec/plano para implementar.

| Gate | Antes da fase 0 | Após a fase 1 / evidência projetada |
| --- | --- | --- |
| I — domínio independente e camadas | PASS: monólito com contratos internos | PASS reavaliado: [contratos internos](contracts/application.md), porta de candidatos própria da US1, modelos JPA externos e ArchUnit |
| II — test-first, cobertura e contratos | PASS: estratégia obrigatória definida | PASS reavaliado: ciclo vermelho/verde; JaCoCo total/por camada; nomes/tags/relatórios isolam suítes sem suprimir integração |
| III — WCAG 2.1 A/AA | PASS: critérios aplicáveis incorporados | PASS no design: [contrato web](contracts/web.md) e validação automatizada/manual em [quickstart.md](quickstart.md) |
| IV — desempenho e observabilidade | PASS para planejar; parâmetros propostos abaixo | PASS no design; aprovação dos parâmetros é gate pré-implementação. Cumprimento depende de medição |
| V — segurança nas fronteiras | PASS: validação e acesso mínimo | PASS reavaliado: [segurança e auditoria](contracts/security-audit.md); revogação composta de inativo, duas provas revalidadas no merge, quarentena e testes negativos |
| Aprovação explícita de spec e plano | PENDENTE; não impede produzir design | BLOQUEIA implementação: produto aprova spec; técnico aprova plano e carga, com nome, data e revisão |
| Políticas e dependências de produção | PENDENTE | BLOQUEIA dados reais/liberação: OIDC, perfis, armazenamento, scanner, termos e retenção aprovados |

Não há violação arquitetural proposta nem dispensa constitucional. Falta de aprovação ou teste reprovado impede a respectiva etapa; não se declara conformidade de software ainda inexistente.

### Parâmetros de desempenho propostos para aprovação

- Homologação dedicada equivalente à implantação inicial: aplicação Linux 4 vCPU/8 GiB, heap 2 GiB; PostgreSQL 4 vCPU/8 GiB e SSD; scanner 2 vCPU/4 GiB, assinaturas atualizadas; volume SSD criptografado. Rede privada de 1 Gbit/s e RTT ≤2 ms entre dependências; cliente de navegador com RTT de 50 ms e 100 Mbit/s. Registrar imagens/digests, configuração e hardware; se produção diferir, ajustar e aprovar antes de implementar.
- Massa determinística: 10.000 pacientes, 80% ativos, 15% inativos e 5% origens mescladas; homônimos, acentos, nomes de dois caracteres, datas invertidas e exceções de identificação. Dois convênios por paciente, responsável quando aplicável, três decisões e três anexos por paciente; arquivos de 100 KiB, 1 MiB e 10 MiB. Evidências inteiramente sintéticas.
- Dez sessões simultâneas; mistura: 40% buscas (60% nome, 25% CPF, 15% prontuário), 25% ficha/resumos, 10% manutenção/identificação/responsáveis, 5% convênios, 5% consentimentos, 5% upload/download, 5% transições/mesclagem e 5% auditoria/invalidação. Sete secretárias, dois médicos e um gerente. Cenários negativos e falhas deliberadas em rodada separada.
- Aquecimento de 5 minutos e medição de pelo menos 20 minutos, com pausas de 0,2–1 s; estender até obter ≥1.000 respostas completas de sucesso por operação e variante relevante, inclusive upload de 10 MiB e download integral. Operações raras recebem rodada dedicada nas mesmas condições. Não esconder uma operação em média de grupo.
- SC-001: rodada separada de exatamente 1.000 buscas completas no navegador e dez sessões: 600 nomes (incluindo duas letras), 250 CPFs, 150 prontuários; 20% páginas posteriores, 10% sem resultado e inclusão de inativos/mesclados. Cada busca apresenta resultado correto ou vazio em ≤2 s; erro ou timeout reprova.
- Aceite: p95 <300 ms por operação e variante, zero erros inesperados, respostas completas e asserções funcionais. Medir no servidor do início ao término da resposta, inclusive corpo e dependências; k6 complementa essa métrica. Upload inclui scanner e persistência, sem confirmação assíncrona para contornar a meta. Risco principal: 10 MiB com inspeção; medir cedo após aprovações e bloquear liberação se não cumprir.
- Incluir como variantes próprias a revogação multipart de inativo com prova nova de até 10 MiB, consulta histórica após encerramento/renovação e mesclagem com duas provas. A revogação com arquivo também inclui scanner, armazenamento e todos os eventos no p95; não agrupá-la com revogação por evidência já existente para ocultar custo.
- Produção: histogramas Micrometer por método e rota parametrizada, nunca identificadores, query ou nomes. Prometheus calcula p95 em janela de 5 minutos por operação; p95 ≥300 ms gera alerta, incidente com responsável operacional e ação corretiva. Erros e contagem ficam visíveis; janela sem amostra não significa sucesso. Auditoria/scanner indisponíveis têm alertas próprios sem dados pessoais.

### Estratégia de entrega e verificação

1. Registrar aprovações versionadas ou revisão vinculada com aprovador, papel, data e commit/hash dos documentos; uma pessoa pode acumular papéis se registrado. Aprovar também a carga. Não produzir código/testes antes disso.
2. Regenerar tarefas a partir desta revisão antes de iniciar scaffold, CI, contratos e testes de arquitetura, mantendo falha observada → implementação mínima → refatoração por comportamento. Não interpretar as 145 tarefas antigas como sincronizadas automaticamente.
3. Implementar jornadas P1 e depois P2 com auditoria e autorização desde a primeira operação. A US1 inclui consulta PostgreSQL real de candidatos AS-003; US3 antecipa anexo administrativo seguro; US5 implementa revogação composta com arquivo, sem esperar a extensão clínica da US6.
4. `test` executará unitários; `verify` executará formatação/análise, unitários, integração, contratos e cobertura. Perfis `e2e` e `performance` terão evidências obrigatórias na CI de homologação antes de liberar.
5. Módulos lógicos de cobertura: `patients.domain`, `patients.application`, `patients.infrastructure`, `patients.presentation`, `shared.application` e `shared.infrastructure`. Agregar subpacotes por módulo e bloquear LINE/BRANCH <80% em qualquer módulo alterado e no bundle. Exclusões apenas geradas/terceiros, explícitas e revisadas; não excluir regras ou adaptadores.
6. Unidade: CPF, idade/data local, telefone/endereço, AS-003 exato, segmentos históricos de convênio, consentimento e critérios documentais de mesclagem. Integração real: candidatos da US1, reservas concorrentes, locks/versões, prova invalidada após prévia, rollback/replay do par anexo+revogação, validade antiga preservada após renovação e auditoria indisponível. HTTP: matriz completa, upload genérico de inativo negado, CSRF, erros e nenhum byte clínico. E2E: oito jornadas, teclado/leitor de tela e SC-004 com dez secretárias treinadas.

### Seleção de suítes e perfis Maven

| Suíte | Seleção obrigatória | Ativação |
| --- | --- | --- |
| Unitários | Surefire `**/*Test.java`; excluir tags integration/e2e/performance | `test` e qualquer `verify` |
| Integração/contratos | Failsafe execução `integration`, `**/*IT.java`, tag `integration`; excluir `**/*JourneyIT.java` e `**/*PerformanceIT.java` | Sempre em `verify` |
| Jornadas E2E | Failsafe execução `e2e`, `**/*JourneyIT.java`, tag `e2e` | Perfil `e2e` acrescenta à base |
| Carga/navegador SC-001 | Failsafe execução `performance`, `**/*PerformanceIT.java`, tag `performance`, orquestrando k6 e navegador | Perfil `performance` acrescenta à base |
| Massa sintética | Utilitário `ValidationDataSeeder`, sem sufixo Test/IT, binding em pre-integration-test | Somente perfil explícito `validation-data`, ambiente test/validation e alvo isolado |

Execuções têm IDs, reportsDirectory e summaryFile próprios, com goals integration-test/verify associados ao mesmo summaryFile. Não substituir seleção global por perfil: `verify -Pe2e` mantém integração; `verify -Pperformance` mantém integração; perfis combinados executam cada suíte uma vez. Teste de convenção reprova nome/tag incompatível ou teste IT sem categoria, impedindo omissão silenciosa. Suíte esperada sem testes/relatório reprova (`failIfNoTests=true`); o gate de desempenho também exige amostras/thresholds, e o de acessibilidade exige relatório/manual. JaCoCo separa arquivos de execução por suíte e agrega a cobertura sem sobrescrever resultados.

`verify` comum não inicia navegador, carga ou seed externo. O teste unitário que demonstra recusa do seed em produção continua na base. Testes de integração usam seus próprios containers/fixtures, independentemente do alvo E2E. Com perfis combinados de seed e jornada/carga, semear explicitamente antes das suítes externas; não semear em startup ou produção.

### Alocação da consulta de candidatos e confirmação documental

US1 entrega `DuplicateCandidateRepository` + `PostgresDuplicateCandidateAdapter`, índices B-tree por nascimento e nome normalizado/nascimento e testes reais de obtenção de candidatos. A consulta usa nascimento igual ou nome normalizado igual com dia/mês validamente invertidos, sem paginação/truncamento que omita candidatos; a política Java aplica AS-003. US2 entrega a porta separada `PatientSearchRepository` e a busca pública paginada com pg_trgm. A leitura interna de candidatos integra a operação de cadastro autorizada/auditada; qualquer projeção exibida na revisão passa pelo gate de leitura.

AS-003 é alerta de suspeita; identidade para merge é a igualdade estrita de nome normalizado e nascimento mais as provas das duas raízes e conferência gerencial. `sourceEvidenceAttachmentId` e `destinationEvidenceAttachmentId` ficam na prévia, registro e evento. Escritores de filhos incrementam a versão da raiz: invalidar uma prova entre prévia e confirmação impede uso da prévia. O merge revalida pertencimento/classificação/validade das provas e não concede leitura clínica.

## Project Structure

### Documentation (this feature)

```text
specs/001-gestao-pacientes/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── web.md
│   ├── application.md
│   └── security-audit.md
└── tasks.md                 # existente; regenerar com speckit-tasks após esta revisão
```

### Source Code (repository root — estrutura futura)

```text
pom.xml
mvnw
mvnw.cmd
.mvn/wrapper/
compose.yaml
src/main/java/br/com/medclinic/
├── MedClinicApplication.java
├── patients/
│   ├── domain/             # valores e políticas Java sem Spring/JPA
│   ├── application/        # casos de uso, DTOs, portas e políticas de acesso
│   ├── infrastructure/     # JPA, SQL, arquivos, scanner e adaptador de resumos
│   └── presentation/       # MVC, formulários, mapeamento de erros
└── shared/
    ├── application/        # Clock, operador, auditoria e unidade de trabalho
    └── infrastructure/     # wiring, OIDC, transações, métricas e eventos
src/main/resources/
├── application.yml
├── db/migration/
├── templates/{fragments,patients,audit,errors}/
└── static/{css,js,images}/
src/test/java/br/com/medclinic/  # espelha camadas; *Test e *IT
src/test/resources/             # fixtures sintéticas e configuração de teste
src/test/performance/           # k6 e manifestos de carga
docs/validation/                # evidências sem dados reais e aprovações
.github/workflows/             # gates CI
```

**Structure Decision**: um projeto Maven, pacotes por contexto e camada, sem frontend separado. Contratos internos e mapeadores preservam o domínio; composição e transação são adaptadores externos. Thymeleaf usa fragments para layout, erros, navegação e status. Esta execução cria somente documentos.

## Complexity Tracking

Nenhuma exceção constitucional. Separar modelos de domínio/JPA atende à independência exigida. Volume privado, scanner e OIDC respondem a fronteiras obrigatórias; não se introduzem microsserviços, event bus ou cache de dados de pacientes.

O envio composto reutiliza preparação privada de arquivo, sem tabela/endpoint público de staging. Segmentos sucessores preservam validade histórica em relações existentes, sem event sourcing. Essas escolhas e a seleção de suítes estão resolvidas em [research.md](research.md); aprovações documentais e demonstração dos gates continuam pendentes.
