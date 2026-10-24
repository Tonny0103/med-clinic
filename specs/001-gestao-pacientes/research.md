# Phase 0 — Pesquisa e decisões

**Data**: 2026-10-24 | **Feature**: [spec.md](spec.md) | **Estado**: decisões técnicas resolvidas para revisão.

Pesquisa distribuída entre stack/persistência/testes e segurança/transações, conforme a skill. Fontes oficiais consultadas; decisões de produto permanecem as da spec. Versões transitivas serão herdadas do BOM.

**Replanejamento de 2026-10-24**: incorporadas as três respostas da sessão de esclarecimento e os ajustes de candidatos AS-003/suítes de teste. A stack e as versões R1/R2 permanecem as já pesquisadas; as decisões de produto em FR-015/018/020/022/026/029/032 e SC-006/007 são normativas para este design.

## R1 — Java, Spring Boot e páginas

**Decisão**: Java 21 LTS, Spring Boot 4.1.2, Spring MVC e starter Thymeleaf; Maven 3.9.x com wrapper. Thymeleaf 3.1.5.RELEASE é gerenciado pelo BOM consultado. Fixar versões exatas do wrapper, plugins não gerenciados e imagens no scaffold, sem `latest`, snapshots ou intervalos.

**Rationale**: atende à escolha explícita do usuário; SSR simplifica validação e acessibilidade para dez usuários. Java 21 está na faixa de suporte publicada para Boot. Maven oferece os comandos previstos no repositório.

**Alternativas consideradas**: Java 25 compatível, mas sem necessidade específica; SPA adicionaria build e estado; Gradle é válido, sem benefício exigido aqui.

**Fontes**: [requisitos Spring Boot](https://docs.spring.io/spring-boot/system-requirements.html), [BOM](https://docs.spring.io/spring-boot/appendix/dependency-versions/coordinates.html), [Maven Wrapper](https://maven.apache.org/tools/wrapper/).

## R2 — Persistência e migrações

**Decisão**: PostgreSQL 18.6; Spring Data JPA/Hibernate somente na infraestrutura, SQL parametrizado quando necessário. Flyway como mecanismo único de migração, incluindo suporte PostgreSQL; Hibernate `ddl-auto=validate`. Domínio sem anotações Spring/JPA.

**Rationale**: constraints, locks, índices e transações protegem identidade, auditoria e mesclagem. Não há schema anterior a migrar. A versão 18 consultada é suportada; ambientes usarão a mesma major.

**Alternativas consideradas**: H2 não reproduz concorrência/índices e não será evidência; NoSQL dificulta invariantes; Liquibase acrescenta ferramenta sem necessidade; entidades JPA no domínio violariam isolamento.

**Fontes**: [versionamento PostgreSQL](https://www.postgresql.org/support/versioning/), [SQL no Boot](https://docs.spring.io/spring-boot/reference/data/sql.html), [inicialização/Flyway](https://docs.spring.io/spring-boot/how-to/data-initialization.html).

## R3 — Busca e suspeita de duplicidade

**Decisão**: nome normalizado persistido, algoritmo versionado de caixa/acentos/espaços; substring literal parametrizada, páginas de 20 e ordenação nome normalizado/prontuário. `pg_trgm` auxilia termos com três ou mais caracteres; termos de duas letras terão carga própria, pois podem não aproveitar trigramas. CPF/prontuário usam B-tree, igualdade e zeros preservados.

**Rationale**: busca parcial difere da política AS-003. Suspeita usa candidatos por nascimento/nome e algoritmo Java puro para igualdade, uma inserção/remoção/substituição ou transposição adjacente, e dia/mês invertidos válidos. Consultar todos os candidatos relevantes, sem limite que esconda suspeitas. Resultado é alerta, nunca mesclagem automática.

**Alocação decidida**: `DuplicateCandidateRepository` e `PostgresDuplicateCandidateAdapter` pertencem à US1. Selecionar nascimento idêntico, ou nome normalizado idêntico com nascimento igual à inversão válida dia/mês; aplicar a regra exata em Java sobre todos os candidatos e retornar somente projeções administrativas autorizadas/versões. Índices B-tree por nascimento e nome normalizado/nascimento entram na migração inicial. `PatientSearchRepository`, paginação e GIN pg_trgm ficam na US2. Isso elimina dependência da US1 em uma história posterior; teste PostgreSQL da US1 deve comprovar a consulta, além do teste da política com candidatos fornecidos.

**Alternativas consideradas**: limiar de trigramas não equivale a AS-003; Levenshtein simples não cobre transposição como uma operação; fonética e aproximações adicionais alterariam escopo.

**Fontes**: [pg_trgm](https://www.postgresql.org/docs/18/pgtrgm.html), [fuzzystrmatch](https://www.postgresql.org/docs/18/fuzzystrmatch.html). O algoritmo exato é decisão derivada da spec.

## R4 — Identidade, concorrência e mesclagem

**Decisão**: prontuário textual gerado por sequência não cíclica e nunca reutilizado; UUID interno. Reserva documental única por identidade consolidada. Histórico comprovadamente corrigido não participa da reserva vigente; identificação válida anterior à mesclagem permanece reservada ao destino. Versão otimista nas alterações; mesclagem bloqueia raízes em ordem de UUID, confere versões e atualiza associação canônica/reservas em uma transação.

**Prova de identidade após esclarecimento**: prévia exige referência de evidência administrativa válida de cada raiz, nome completo igual após desconsiderar somente caixa/acentos/espaços repetidos e nascimento idêntico. Gerente confere os documentos e confirma com justificativa. Confirmar revalida estado, pertencimento e versões das provas/raízes; invalidação ou alteração desde a prévia exige revisão. Referências das duas provas ficam no MergeRecord e AuditEvent sem copiar valores ou conteúdo. AS-003 aproximado não comprova identidade; CPF válido divergente continua bloqueando. A origem inativa sem prova já vinculada não recebe upload avulso para mesclar: sua regularização segue reativação autorizada, quando aplicável.

**Rationale**: consulta prévia não impede corrida. Conflito de constraint vira erro de negócio. Origem imutável em cada vínculo e associação canônica preservam rastreabilidade sem mover arquivos ou reescrever eventos. Revalidar dentro da transação impede ciclos e prévia ultrapassada.

**Alternativas consideradas**: conferir apenas em Java falha sob concorrência; apagar origem perde números; transações por vínculo permitem consolidação parcial; reservar identificação comprovadamente errada perpetua associação falsa.

**Fonte**: [locks PostgreSQL](https://www.postgresql.org/docs/18/explicit-locking.html). Detalhes em [data-model.md](data-model.md).

## R5 — Autenticação e autorização

**Decisão**: Spring Security com OIDC Authorization Code e sessão de servidor. `(issuer, subject)` identifica operador; mapear somente grupos/claims permitidos para RECEPCAO, GERENTE e MEDICO. Sem perfil reconhecido, negar. Provedor é dependência externa configurável; testes usam issuer controlado. Sem administração de contas neste módulo.

**Rationale**: atende AS-004. Política de acesso pertence à aplicação; regras HTTP são defesa adicional. CSRF em todos os formulários, cookies seguros, saída escapada e respostas privadas sem cache.

**Alternativas consideradas**: botões ocultos não autorizam; qualquer claim de papel permitiria escalada; autenticação caseira amplia escopo; médico com leitura clínica contradiz FR-023.

**Fontes**: [OIDC/authorities](https://docs.spring.io/spring-security/reference/servlet/oauth2/login/advanced.html), [métodos](https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html), [CSRF](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html).

## R6 — Auditoria e repetição segura

**Decisão**: eventos append-only no mesmo PostgreSQL. Mutação, sucesso e resultado idempotente na mesma transação. Leitura materializa projeção limitada, confirma evento e só então renderiza/streama. Tentativas negadas/falhas usam transação curta independente após rollback, sem recursão. Falha de auditoria impede liberação.

**Rationale**: fila assíncrona isolada viola FR-034. Idempotência recupera commit com resposta perdida. Evento de leitura registra autorização/liberação, não prova recebimento integral pelo cliente. Download não usa URL pública ou acesso direto ao volume.

**Alternativas consideradas**: log técnico não substitui evento; auditar após streaming é tarde; sucesso em transação independente poderia sobreviver ao rollback do negócio.

**Fontes**: [transações Spring](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/tx-propagation.html), [privilégios PostgreSQL](https://www.postgresql.org/docs/18/ddl-priv.html).

## R7 — Documentos e quarentena

**Decisão**: volume privado e ClamAV, envio síncrono. Limitar streaming a 10 MiB; validar assinatura/formato/estrutura; rejeitar vazio, criptografado/não inspecionável e inseguro. Categoria incerta é clínica. Após inspeção limpa e persistência física, confirmar vínculo/auditoria no banco. Nunca retornar conteúdo clínico, por qualquer papel.

**Rationale**: uma instância e volume privado reduzem dependências. Arquivo é gravado antes da transação de vínculo; falha deixa órfão inacessível, sem vínculo parcial. Separar diretórios de quarentena/administrativos/clínicos; porta de leitura recebe somente ID de anexo administrativo válido, jamais caminho arbitrário. Scanner processa tecnicamente sem disponibilizar conteúdo ao usuário.

**Alternativas consideradas**: `static/` e URLs clínicas infringem restrição; MIME do navegador não basta; S3 é alternativa para múltiplas instâncias, sem requisito atual; 202 não será confirmação nem forma de esconder latência.

**Risco/gate**: scanner e 10 MiB podem exceder p95 <300 ms. Medir no perfil aprovado; falha bloqueia liberação e exige otimização ou emenda constitucional formal para relaxar a meta. Nenhuma meta foi reduzida.

**Fontes**: [OWASP File Upload](https://cheatsheetseries.owasp.org/cheatsheets/File_Upload_Cheat_Sheet.html), [ClamAV](https://docs.clamav.net/).

## R8 — Testes, qualidade e métricas

**Decisão**: JUnit Jupiter/Testcontainers via BOM, MockMvc, ArchUnit, Playwright Java/axe-core, JaCoCo LINE/BRANCH ≥80% no conjunto e por módulo lógico; Spotless/Checkstyle com quatro espaços, SpotBugs e scanner de segredos. Micrometer/Prometheus, k6 por operação, navegador para SC-001 e avaliação com usuárias para SC-004.

**Rationale**: domínio testa sem Spring; banco real verifica constraints/transações; axe não substitui teclado/leitor de tela. JaCoCo agrega unitários e integração; relatório ausente deve reprovar, sem gate ignorado.

**Alternativas consideradas**: cobertura global oculta módulos sem testes; UI isolada não cobre corridas; p95 agregado oculta rota lenta; acessibilidade automática isolada é insuficiente.

**Seleção de suítes decidida**: Surefire executa `*Test`; Failsafe tem execução `integration` sempre no `verify`, com tag `integration`, `*IT` e exclusão de `*JourneyIT`/`*PerformanceIT`. Perfil `e2e` acrescenta execução exclusiva `*JourneyIT`/tag `e2e`; perfil `performance` acrescenta `*PerformanceIT`/tag `performance` e orquestra k6. Cada execução possui relatórios, summaryFile e verificação próprios; perfis não substituem uma propriedade global que desligue integração. Com perfis combinados, cada suíte roda uma vez. `failIfNoTests` e relatórios obrigatórios impedem sucesso silencioso de suíte esperada ausente; tags e nomes devem concordar, conferidos por teste de convenção. Utilitário `ValidationDataSeeder` fica fora de `*Test`/`*IT`, vinculado exclusivamente a `validation-data` em pre-integration-test, guardado por ambiente/alvo isolado. Seu teste de recusa de produção é unitário normal.

**Alternativas de seleção consideradas**: somente `*IT` mistura navegador/carga com integração; trocar groups global por perfil pode suprimir a base; compartilhar summaryFile pode sobrescrever uma suíte; executar seed como teste descoberto tornaria o build padrão capaz de alterar a massa de validação.

**Fontes**: [testes Boot](https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html), [Testcontainers](https://docs.spring.io/spring-boot/reference/testing/testcontainers.html), [Playwright Java](https://playwright.dev/java/docs/intro), [JaCoCo](https://www.jacoco.org/jacoco/trunk/doc/check-mojo.html), [thresholds k6](https://grafana.com/docs/k6/latest/using-k6/thresholds/).

**Fontes da seleção de suítes**: [tags JUnit](https://docs.junit.org/6.0.3/writing-tests/tagging-and-filtering.html), [JUnit Platform no Failsafe](https://maven.apache.org/surefire/maven-failsafe-plugin/examples/junit-platform.html), [parâmetros de execução](https://maven.apache.org/surefire/maven-failsafe-plugin/integration-test-mojo.html), [verificação e summaryFile](https://maven.apache.org/surefire/maven-failsafe-plugin/verify-mojo.html). A distribuição entre perfis é decisão deste projeto apoiada nessas capacidades.

## R9 — Integrações ausentes e governança

**Decisão**: porta de resumos com NÃO_CONFIGURADO, DISPONIVEL, VAZIO e FALHA; produção inicial retorna NÃO_CONFIGURADO. Fonte controlada testa demais estados e proveniência na mesclagem. Vigência de convênio/resolução de identidade são contratos internos para módulos futuros, sem novas APIs públicas. `Clock` e fuso injetados.

**Rationale**: não inventar atendimento nem confundir ausência de módulo com ausência de histórico. Retenção, termos e OIDC são pré-condições de produção, sem aprovação presumida.

**Alternativas consideradas**: fixtures em produção/lançamento manual alteram escopo; fuso implícito do host torna datas inconsistentes.

**Fonte normativa interna**: FR-013, AS-004/005/006/008 e constituição 1.0.0. Não restam esclarecimentos técnicos abertos; gates pendentes estão no plano.

## R10 — Evidência nova na revogação de inativo

**Decisão**: POST de revogação aceita referência administrativa existente válida ou novo arquivo administrativo no próprio formulário multipart, exatamente uma fonte de evidência. `RevokeConsent` usa preparação privada de arquivo compartilhada com o uploader, mas confirma novo Attachment, ConsentDecision REVOGADO, incremento de versão, eventos de anexação/revogação e um recibo de operação na mesma transação. A inspeção e gravação física ocorrem antes dos locks; erro deixa no máximo órfão privado, nunca anexo avulso bem-sucedido. O paciente permanece INATIVO. POST genérico de documentos continua negado para esse estado.

**Rationale**: realiza a exceção escolhida pelo usuário sem abrir manutenção cadastral ou outro tipo de anexação. A política interna distingue `RevokeConsent` de `UploadAttachment`; não existe parâmetro de formulário capaz de ligar um bypass de status. Categoria da nova prova é EVIDENCIA_REVOGACAO, administrativa, fixada pelo servidor; qualquer formato/segurança inválido ou conteúdo classificado clínico é recusado nesse fluxo.

**Alternativas consideradas**: obrigar reativação contradiz FR-020; liberar uploader genérico de inativo contraria a opção escolhida; endpoint separado de staging/token acrescentaria ciclo de vida e risco de vínculo avulso sem necessidade; gravar documento e decisão em commits independentes permite sucesso parcial. Reutilizar apenas comprovante antigo não cobre a nova evidência comunicada depois da inativação.

**Fontes**: decisão de produto em Clarifications 2026-10-24 e FR-015/020/022; padrão de transação/auditoria de R6 e arquivo privado de R7.

## R11 — Vigência e validade históricas de convênio

**Decisão**: considerar intervalo efetivo `[startsOn, endsOn)` e validade do segmento em D, sem filtrar pelo estado atual ATUAL/ENCERRADO. Vínculo encerrado depois de D participa da consulta histórica; no dia expiresOn continua válido. Renovação/alteração de validade ou de dados da cobertura cria sucessor com sua própria validade/período e referência ao anterior, preservando o segmento antigo. Não sobrescrever expiresOn anterior. `CoverageStatus` retorna referenceDate, inForceOnDate, expiredOnDate e proveniência, separando `currentPreference` de dados históricos; não inventa preferência passada nem reabre cobertura.

**Rationale**: apenas remover `state=ATUAL` não basta se a validade antiga for sobrescrita. Segmentos relacionais já compatíveis com vínculos/períodos do modelo atendem FR-016/018 e evitam introduzir reconstrução por logs ou event sourcing. Os períodos de uma cadeia de sucessores não se sobrepõem; múltiplos convênios diferentes continuam permitidos.

**Alternativas consideradas**: usar situação atual elimina coberturas históricas válidas; sobrescrever validade projeta renovação sobre o passado; reconstruir snapshots de auditoria violaria minimização e não é necessário; restringir consulta a hoje/futuro foi rejeitado pelo usuário. Não se acrescenta histórico de preferência, que não foi pedido.

**Fonte normativa**: terceira resposta em Clarifications 2026-10-24, FR-016–018 e SC-007. Intervalos e sucessores são decisões de implementação, preservando a regra de produto confirmada.
