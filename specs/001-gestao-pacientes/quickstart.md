# Phase 1 — Guia de execução e validação

**Estado atual**: apenas especificação/design existem. `pom.xml`, wrapper, Compose, perfis e testes abaixo são contratos de entrega para a implementação; os comandos ainda não são executáveis neste repositório. Este fluxo não criou código nem executou testes da aplicação.

Referências: [plano](plan.md), [modelo](data-model.md), [HTTP/UI](contracts/web.md), [portas](contracts/application.md), [segurança](contracts/security-audit.md).

**Revisão**: este guia incorpora Clarifications 2026-10-24. Regenerar `tasks.md` a partir do plano revisado antes da implementação; o backlog existente ainda descreve o design anterior.

## Pré-condições

1. Produto aprova `spec.md`; responsável técnico aprova `plan.md` e parâmetros de carga. Registrar nome, papel, data e revisão exata em artefato versionado ou revisão vinculada, conforme constituição. A mesma pessoa pode exercer ambos os papéis se isso for registrado. Este guia não representa aprovação.
2. Após essas aprovações e a atualização do backlog por `$speckit-tasks`, executar a implementação derivada dos documentos revisados. Cada mudança registra teste que falhou pelo comportamento ausente e execução aprovada; não basta teste escrito depois.
3. Instalar JDK 21, Docker com Compose e navegadores/dependências do Playwright. Maven será obtido pelo wrapper; homologação de desempenho requer k6 e os recursos descritos no plano, sem disputar CPU com o gerador de carga.
4. Disponibilizar somente ambiente isolado e dados sintéticos. OIDC local/controlado oferece identidades de recepção, gerente, médico e usuário sem papel, sem construir gestão de contas no módulo. Configurar secrets no ambiente/gestor externo, sem valores no repositório, terminal ou evidências.

## Contrato de configuração a implementar

O scaffold deve mapear as variáveis abaixo em configuração validada no startup; todas as obrigatórias ausentes/invalidas impedem inicialização em ambiente real. `application-local.*` e `.env` permanecem ignorados; exemplos só têm descrições/valores fictícios sem validade, nunca segredos.

| Variável | Conteúdo / condição |
| --- | --- |
| `MEDCLINIC_DB_URL` | JDBC PostgreSQL do ambiente, sem senha embutida |
| `MEDCLINIC_DB_USER`, `MEDCLINIC_DB_PASSWORD` | Identidade runtime de privilégio mínimo; segredo externo |
| `MEDCLINIC_MIGRATION_USER`, `MEDCLINIC_MIGRATION_PASSWORD` | Identidade separada para Flyway, sem permissão equivalente no runtime |
| `MEDCLINIC_OIDC_ISSUER`, `MEDCLINIC_OIDC_CLIENT_ID`, `MEDCLINIC_OIDC_CLIENT_SECRET` | Provedor/cliente provisionados fora do módulo; redirect permitido conforme ambiente |
| `MEDCLINIC_CLINIC_ZONE` | IANA, inicialmente `America/Cuiaba`, validado com a clínica |
| `MEDCLINIC_FILES_ROOT` | Diretório privado persistente fora de `static/`, com permissões e criptografia do ambiente |
| `MEDCLINIC_SCANNER_HOST`, `MEDCLINIC_SCANNER_PORT` | ClamAV privado saudável, com assinaturas atualizadas |
| `MEDCLINIC_IDEMPOTENCY_HMAC_KEY` | Chave externa; jamais imprimir fingerprint/payload em logs |
| `MEDCLINIC_VALIDATION_BASE_URL` | Alvo isolado dos perfis E2E/performance, jamais produção |

Compose futuro terá serviços `db`, `scanner` e `oidc` com healthchecks e imagens fixadas; portas do banco/scanner limitadas ao ambiente privado/local. Credenciais e usuários de teste são provisionados em runtime fora de arquivos versionados. Volume de arquivos não é servido diretamente pelo proxy. Produção inicial tem uma instância de aplicação e volume persistente.

## Inicialização após implementação

Em PowerShell, na raiz do repositório e com variáveis fornecidas externamente:

```powershell
java -version
docker compose up -d db scanner oidc
docker compose ps
.\mvnw.cmd test
.\mvnw.cmd verify
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=local'
```

Em Unix, substituir `.\mvnw.cmd` por `./mvnw`. Esperado: serviços saudáveis, Flyway migra com conta própria, Hibernate valida schema e aplicação local atende em `http://localhost:8080`. Abrir `/pacientes` deve encaminhar para OIDC; usuário sem papel recebe negação. Nenhuma página de paciente é pública. Local usa dados sintéticos; TLS/cookies Secure são obrigatórios na homologação de segurança e produção.

Fixtures de validação serão fornecidas por perfil Maven `validation-data`, restrito a ambientes `test`/`validation`, com recusa explícita em produção e exigência de banco isolado. O utilitário ValidationDataSeeder não tem sufixo Test/IT e só é executado por binding desse perfil em pre-integration-test; não deve rodar no `verify` comum ou startup. Seu teste de recusa de produção é unitário normal. Esse perfil deve criar massa determinística sem apagar bancos existentes e sem precisar de endpoint de seed público. Usuários vêm do OIDC controlado; não há senha de teste no código.

```powershell
.\mvnw.cmd verify -Pvalidation-data
.\mvnw.cmd verify -Pe2e
```

Executar esses comandos em outro terminal com a aplicação/alvo isolado disponível. `validation-data` valida/cria o conjunto sintético com seed fixo; repetir não duplica registros. `e2e` instala/usa navegadores por configuração Maven versionada e executa os cenários automatizados, incluindo axe-core. As duas execuções dependem do alvo isolado e dados disponíveis. Testes unitários/integração usam recursos próprios, sem depender de paciente pré-existente.

### Conferência da seleção de suítes após o scaffold

| Comando | Execuções esperadas |
| --- | --- |
| `.\mvnw.cmd test` | Surefire `*Test`; sem navegador/carga/seed |
| `.\mvnw.cmd verify` | Unitários + Failsafe `*IT`/tag integration, excluindo JourneyIT e PerformanceIT; sem seed externo |
| `.\mvnw.cmd verify -Pe2e` | Base anterior + `*JourneyIT`/tag e2e; sem desempenho/seed implícitos |
| `.\mvnw.cmd verify -Pperformance` | Base anterior + `*PerformanceIT`/tag performance e k6; sem jornadas E2E não relacionadas à carga ou seed implícito |
| `.\mvnw.cmd verify -Pvalidation-data` | Seed explicitamente guardado + unitários/integração; sem ativar navegador/carga |

Suítes Failsafe têm relatórios e failsafe-summary.xml separados em `target/failsafe-reports/integration/`, `target/failsafe-reports/e2e/` e `target/failsafe-reports/performance/`; unitários usam `target/surefire-reports/`. Verificar que cada execução valida seu próprio resumo e que integração permanece presente com perfis adicionais. Nome/tag incompatível, IT sem categoria ou suíte esperada sem testes/relatório reprova. Os perfis são cumulativos, cada suíte uma vez, sem substituir a base; medições de aceite ocorrem em rodadas isoladas com massa/ambiente registrados, sem disputa com outras avaliações.

## Cenários de aceite

Executar pela interface e também por requisições diretas nos testes de contrato. Gerar CPF com dígitos verificadores para personas sintéticas em ambiente isolado, sem associar pessoas reais. Valores gerados não podem vazar em logs/reports públicos.

| Roteiro | Passos / resultado esperado | Rastreabilidade |
| --- | --- | --- |
| 1. Identidade e completude | Como recepção, cadastrar adulto com CPF, menor sem CPF com documento próprio/responsável, estrangeiro sem CPF. Exigir nome/nascimento/sexo/telefone/endereço; e-mail ausente permite salvar. Ausência de telefone ou endereço, CPF inválido, nascimento futuro e adulto brasileiro sem CPF bloqueiam preservando campos válidos. Prontuários distintos e permanentes | US1; FR-001–008; SC-002/007/009 |
| 2. Concorrência e suspeita | Duas submissões simultâneas com mesmo CPF/alternativo: somente uma confirma. Repetir com documento de inativo/mesclado. Repetir mesmo operationId após simular resposta perdida: um efeito. Ainda na US1, comprovar que DuplicateCandidateRepository obtém candidatos reais do PostgreSQL, sem depender da busca pública da US2 ou fornecer lista apenas em memória. Testar AS-003 com acentos/espaços, uma edição/transposição, dia/mês invertidos e negativos; justificativa permite homônimo distinto. Telefone familiar não bloqueia | FR-003–005/008/034; SC-002/003 |
| 3. Busca e ficha | Buscar nome parcial/duas letras/acentos, CPF pontuado e prontuário com zeros. Verificar máscaras, ordenação estável, páginas de 20 e filtros preservados. Vazio é distinto de falha; critério insuficiente não lista a base. Nome padrão omite inativos; CPF/prontuário exatos encontram inativos e origem mesclada com destino | US2; FR-009–012; SC-001/007 |
| 4. Histórico | Sem fonte, todas as consultas mostram «Histórico ainda não disponível». Com adaptador apenas de teste: disponível apresenta somente data/profissional/tipo em ordem; vazio e falha têm mensagens distintas. Não existe formulário/manual de resumo, dado fictício em produção ou texto clínico | FR-013; AS-005; SC-007 |
| 5. Atualização e estados | Abrir duas sessões/versões, salvar em uma e tentar a antiga: conflito sem sobrescrever. Corrigir identificação exige motivo/evidência administrativa válida e preserva prontuário. Inativar com motivo remove da busca padrão; reativar por recepção/gerente com motivo restaura mesmo cadastro/vínculos. Sem motivo bloqueia; origem mesclada nunca reativa. Inativo bloqueia edição comum e permite revogação/invalidação autorizadas | US3; FR-014–015; SC-002/007 |
| 6. Responsáveis/convênios | Menor exige responsável completo; emergência não o substitui. Contato opcional exige nome/vínculo/telefone juntos. Registrar dois planos e preferir um ou particular; encerrar preserva histórico. Consultar data anterior a encerramento posterior: cobertura aparece vigente conforme período, com validade daquela data. Renovar validade por sucessor e confirmar que consulta anterior usa expiresOn antigo; testar início inclusivo/fim exclusivo, ontem/hoje/amanhã e validade inclusiva. Consulta não reabre vínculo nem muda preferência atual/atendimentos; troca de preferido exige escolha explícita. Maioridade usa fuso local e preserva responsáveis históricos | US4; FR-006–007/016–018; SC-007 |
| 7. Consentimento | Registrar concessão/recusa por finalidades distintas com termo/manifestante/prova; recusar imagem não bloqueia. Inativar e revogar com novo arquivo administrativo no formulário da própria revogação: mantém INATIVO, cria uma prova e uma decisão e audita ambos no mesmo commit. Enviar esse arquivo pelo uploader genérico, inclusive com flag de revogação, continua bloqueado. Falha de scanner/auditoria/decisão não deixa anexo avulso; replay pós-commit não duplica efeito. Invalidar prova da revogação não restaura autorização antiga. Mudança de termo/responsável/cadastro não concede e nenhum checkbox vem aceito | US5; FR-015/019–022; SC-003/007 |
| 8. Anexos e conteúdo | Enviar PDF/JPEG/PNG administrativos, exame, encaminhamento e categoria incerta. Testar vazio, 10 MiB, 10 MiB+1, MIME falso, conteúdo inseguro e scanner indisponível; recusados não têm vínculo/acesso. Para TODOS os papéis, URL direta de clínico nega sem byte/preview/filename revelador. Médico lê administrativo válido, não envia. Somente gerente invalida com motivo; arquivo original fica preservado e indisponível na consulta comum | US6; FR-022–025/030; SC-005 |
| 9. Mesclagem | Exigir de G duas provas ADMINISTRATIVO/VALIDO, uma de cada conjunto, nome igual após somente caixa/acentos/espaços repetidos, nascimento idêntico, conferência e justificativa. Prova ausente/clínica/inválida/trocada, nome aproximado, dia/mês invertidos ou CPF divergente bloqueiam. Invalidar prova após prévia torna confirmação conflitante e exige revisão. Confirmar preserva referências das duas provas no registro/evento sem conteúdo, destino, ambos os prontuários, reservas legítimas e todos os vínculos/origens, sem manter identificação comprovadamente corrigida. Auto-merge/ciclo/mesclado/token vencido/versões alteradas bloqueiam; falha reverte tudo e revogação não vira autorização | US7; FR-026–029/032; SC-006 |
| 10. Auditoria/indisponibilidade | Como gerente, filtrar período/usuário/paciente/operação, 20 por página, incluindo origens. Confirmar eventos para todas as operações, vazio, negativas e consulta da auditoria sem recursão. Recepção/médico não consultam. Nenhum perfil altera/apaga evento. Simular auditoria indisponível em leitura/download/busca/mutação: sem dados/sucesso; retomar e repetir sem duplicação. Eventos contêm campos/referências, sem valores sensíveis | US8; FR-031–034; SC-003/005 |

## Qualidade, cobertura e acessibilidade

`verify` deve falhar com formatação/análise/segredos inválidos ou cobertura insuficiente. Relatório JaCoCo combina unitários e integração, exige LINE e BRANCH ≥80% no total e em cada módulo lógico alterado do plano. Relatório ausente ou teste ignorado não equivale a sucesso. Arquitetura deve reprovar domínio importando Spring/JPA e apresentação acessando persistência diretamente.

As seleções de nomes/tags e perfis são as do [plano](plan.md#seleção-de-suítes-e-perfis-maven). Relatórios de suítes/cobertura não se sobrescrevem; ativar perfil não desliga integração. O recebimento de prova na revogação e a comparação documental no merge também passam por E2E/axe e avaliação manual, com mensagens de erro e foco acessíveis.

Nos oito fluxos, axe-core não pode deixar violações aplicáveis A/AA. Avaliação manual: navegar sem mouse, verificar foco em erros/confirmações, preenchimento de datas, páginas, upload e prévia de merge; usar leitor de tela (NVDA no Windows e combinação suportada documentada), verificar nomes/ordem/anúncios e contraste. Registrar critérios avaliados, navegador/leitor/versões, evidências e correções; screenshot isolada não prova acessibilidade.

SC-004 exige dez secretárias treinadas, dados/manifestações prontos e cronômetro da entrada até cadastro completo, anexo de identidade e decisões. Nove ou mais devem concluir corretamente em ≤4 minutos, sem ajuda e sem e-mail obrigatório. Não substituir avaliação humana por cronômetro automatizado.

## Desempenho e falhas

Após aprovação dos parâmetros e implantação equivalente, preparar massa de 10.000 pacientes e executar:

```powershell
.\mvnw.cmd verify -Pperformance
```

Perfil futuro deve orquestrar k6 e rodada Playwright de SC-001, guardar manifesto de ambiente, duração/amostras, mix, p95 por rota/operação/tamanho, taxa de erros e verificação de conteúdo. Aplicar exatamente [os parâmetros do plano](plan.md#parâmetros-de-desempenho-propostos-para-aprovação), inclusive scanner, auditoria, volume criptografado e 10 MiB. Não usar resposta parcial/erro como resultado rápido. Rodada insuficiente em amostra deve reprovar, não apresentar percentil conclusivo.

Medir separadamente revogação por prova existente e revogação multipart com prova nova de inativo, inclusive 10 MiB, scanner e ambos os eventos no tempo do serviço. Incluir consulta histórica após encerramento/renovação e merge com duas provas. Não contar upload privado preparatório ou 202 como operação concluída. Um piloto antecipado do orçamento de arquivos não substitui a rodada completa na massa aprovada.

Esperado: cada operação p95 <300 ms no serviço, zero falhas inesperadas e todas as 1.000 buscas corretamente visíveis em ≤2 s. Nenhuma aprovação é inferida de teste não executado. Se upload máximo ou outra rota falhar, bloquear liberação e corrigir; relaxamento depende de processo constitucional formal.

Executar falhas de auditoria/scanner/fonte externa e interrupção pós-commit em rodada isolada. Registrar recuperação e idempotência. Não incluir eventos de falha induzida no grupo de sucessos para baixar latência; reportar ambos separadamente. Confirmar métricas por operação e alerta p95 ≥300 ms em janela de 5 minutos, sem rótulos pessoais.

## Evidências e encerramento da validação

Revisão deve vincular versões de spec/plano/aprovações, evidência vermelho/verde, resultados de `test`/`verify`/E2E/performance, cobertura por módulo, matriz negativa, relatório manual de acessibilidade, screenshots sintéticas e avaliação SC-004. Guardar referências em `docs/validation/` sem dados reais ou segredos. Testar restauração do banco/volume e integridade das referências antes de dados reais.

Políticas de tratamento, termos, retenção de originais/auditoria/recibos/backups e atendimento a titulares precisam de aprovação da clínica antes de dados reais; não há exclusão definitiva na interface. Não executar limpeza destrutiva de volumes como parte deste guia. Falha em gate obrigatório impede integração/liberação conforme constituição.
