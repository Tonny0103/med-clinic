# Tarefas — MVP de gestão de pacientes

Escopo decidido pelo usuário em 2026-10-24: manter as oito histórias, implantação pequena e poucos testes essenciais. Constituição 2.0.0. Este backlog substitui as 145 tarefas anteriores, preservadas no histórico Git. Executar em ordem; um commit por tarefa concluída. Testes de regras críticas precedem sua implementação. Não marcar execução externa/manual não realizada.

- [X] T001 Alinhar constituição, AGENTS.md, spec, plano e contratos ao MVP; substituir aprovações formais pelo registro de escopo em docs/validation/001-gestao-pacientes/approvals.md.
- [X] T002 Criar projeto Java/Spring Boot/Thymeleaf, Maven Wrapper, configuração local/teste, formatação e README com comandos.
- [X] T003 Escrever testes essenciais e implementar regras puras de identidade, suspeita de duplicidade e vigência de convênios em domain.
- [X] T004 Implementar portas, persistência transacional, versões, idempotência, autenticação por três perfis e auditoria interna.
- [X] T005 [US1] Implementar formulário e cadastro com prontuário imutável, CPF/documento único, responsável de menor e justificativa de suspeita.
- [X] T006 [US2] Implementar busca paginada, ficha administrativa, aliases e histórico não disponível, com leitura auditada.
- [X] T007 [US3] Implementar edição com conflito, correção documental, inativação e reativação com motivo.
- [ ] T008 [US4] Implementar responsáveis históricos, emergência, múltiplos convênios, preferência atual e consulta histórica por data.
- [ ] T009 [US5] Implementar manifestações imutáveis, finalidades, revogação e evidência nova exclusivamente nesse fluxo para inativos.
- [ ] T010 [US6] Implementar upload privado inspecionado, metadados neutros, leitura administrativa e invalidação gerencial; negar conteúdo clínico.
- [ ] T011 [US7] Implementar prévia e confirmação transacional de mesclagem com duas provas, versões e proveniência.
- [ ] T012 [US8] Implementar consulta gerencial paginada de eventos com filtros e auditoria da própria consulta.
- [ ] T013 Preparar Compose/Oracle, roteiro de demonstração e cópia de segurança; executar testes essenciais e registrar limites da validação.

