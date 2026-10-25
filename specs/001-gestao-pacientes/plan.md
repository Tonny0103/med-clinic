# Plano — Gestão de pacientes: MVP demonstrativo

**Escopo**: oito histórias de spec.md; dezenas de pacientes fictícios, poucos usuários e uma clínica. Constituição 2.0.0. Implementação autorizada pela instrução do usuário de 2026-10-24; sem gates de aprovação ou carga.

## Stack e organização
Java 21, Spring Boot, Maven Wrapper, Spring MVC, Thymeleaf, Spring Security com login de formulário, JDBC e PostgreSQL. H2 somente para testes e execução local opcional. Domínio Java puro; aplicação orquestra portas; infraestrutura implementa banco, arquivos e scanner; apresentação contém controllers e templates. JDBC substitui JPA para reduzir configuração e manter transações explícitas.

Pacote base br.com.medclinic. src/main/java, src/main/resources/templates, static e db/migration; testes em src/test/java. CSS próprio, HTML semântico e sem SPA.

## Persistência e transações
Guardar a ficha como agregado serializado em texto, com colunas relacionais para pesquisa, prontuário, documentos únicos, versão e situação. Filhos conservam IDs e origem. Atualizações verificam versão; banco protege identificadores e chave de submissão. Transações incluem alteração e auditoria; leituras protegidas exigem evento antes da resposta. Mesclagem bloqueia raízes em ordem, valida versões/provas e mantém alias da origem. Consultas pequenas podem filtrar agregados em memória: dezenas de pacientes não justificam motor de busca.

## Segurança
Três contas configuradas externamente, perfis RECEPCAO/GERENTE/MEDICO, sem módulo de usuários ou OIDC. CSRF, cookies HttpOnly/SameSite, no-store e autorização no serviço. Médico só lê administração. Arquivos privados, PDF/JPEG/PNG até 10 MiB, inspeção ClamAV antes de gravar vínculo. Nenhuma rota libera conteúdo clínico. Inativos só recebem nova evidência pelo fluxo de revogação; demais anexos exigem ativo.

## Verificação
Poucos testes essenciais: regras de CPF/menor/suspeita/convênio e teste de integração dos fluxos críticos/permissões/rollback. Testes críticos precedem comportamento. Sem cobertura mínima, browser automatizado obrigatório, teste de carga ou pesquisa com dez secretárias. Roteiro manual curto em quickstart.md; evidência real em docs/validation. Não afirmar conformidade clínica ou jurídica.

## Operação
Uma VM Oracle A1 candidata, com aplicação, PostgreSQL e ClamAV via Compose; arquivos e banco em volumes privados. Imagens compatíveis com ARM64; build local também em amd64. Desenvolvimento sem Docker pode usar H2 e scanner real externo; scanner indisponível bloqueia envio. Termos e dados são fictícios. Não provisionar conta Oracle automaticamente.

## Sequência
T001 alinhamento → T002 scaffold → T003 regras → T004 persistência/segurança → T005 cadastro → T006 busca → T007 manutenção → T008 vínculos → T010 arquivos → T009 consentimentos → T011 mesclagem → T012 auditoria → T013 implantação e validação final. Arquivos antecedem consentimentos para fornecer a preparação privada de evidências. Cada tarefa produz um commit.

