# Contratos internos de aplicação — v1

Contratos Java internos; não são endpoints públicos. Tipos de domínio e aplicação não dependem de Spring, JPA, servlet, SDK de scanner ou sistema de arquivos. Adaptação/wiring são externos. Referências: [modelo](../data-model.md), [web](web.md), [segurança](security-audit.md).

Revisão alinhada às respostas de Clarifications 2026-10-24. Estes contratos substituem os detalhes anteriores de revogação de inativo, critérios de merge e situação histórica de convênio.

## Entradas e resultados comuns

`ActorContext`: identidade estável issuer/subject, perfis permitidos, perfil atuante e correlationId; originado da sessão validada, nunca do formulário. `CommandContext`: operationId e expectedVersion quando há agregado existente. Instantes por `Clock`; configuração fornece `ClinicZone`.

Comandos recebem DTOs com allowlist e retornam referências administrativas, versão e código de resultado. Falhas tipadas: ValidationFailure (campos/códigos), PermissionDenied, IdentityConflict, DuplicateReviewRequired, StaleVersion, InvalidState, PreviewExpired e DependencyUnavailable. Detalhes de banco/arquivo/token não cruzam a apresentação. Motivos nunca contêm conteúdo clínico.

## Casos de uso e portas

| Contrato / responsabilidade | Entrada → saída / invariantes |
| --- | --- |
| RegisterPatient / UpdatePatient | Dados/ator/versão → PatientReference; validam completude, documentos e responsáveis, auditam campos |
| AddCpf / CorrectIdentification | Documento e contexto → mesma identidade/prontuário; correção exige motivo/evidência administrativa válida |
| ReviewDuplicates | Identificação candidata → projeções autorizadas e token de revisão ligado a candidato/versões; AS-003 exato e candidatos PostgreSQL completos já na US1 |
| ChangePatientStatus | Alvo/motivo/confirmação → versão; nenhuma exclusão ou reativação de mesclado |
| MaintainGuardians / EmergencyContact | Dados/períodos → versão; checa responsáveis de menor e contato atual único |
| MaintainCoverage / SetPreference | Vínculo/sucessor/preferência → versão; preserva períodos e validade antiga, exige escolha explícita quando substitui preferido |
| RecordConsent / RevokeConsent | Finalidade/termo/manifestação e evidência → decisão/estado; RevokeConsent aceita ID válido ou arquivo novo, exatamente um, e confirma prova nova+revogação atomicamente em ATIVO/INATIVO |
| UploadAttachment / InvalidateAttachment | Stream limitado/categoria ou motivo → referência neutra/versão; uploader genérico somente ATIVO, sem retorno clínico; invalidação gerencial também permite INATIVO |
| SearchPatients / ReadPatient | Critério limitado ou ID → página/projeção administrativa; autorização e auditoria antes de retorno externo |
| ReadAdministrativeAttachment | ID de paciente/anexo → handle administrativo validado; autorização/evento durável antes de consumir bytes |
| PreviewMerge / ConfirmMerge | Duas raízes/versões, duas provas administrativas válidas, nome/nascimento coincidentes, escolhas, justificativa e confirmação gerencial → prévia ou destino; revalidar tudo na mesma transação de consolidação |
| QueryAudit | Filtros e contexto gerencial → página de 20; audita consulta sem recursão |

### Portas de persistência e coordenação

- `PatientRepository` carrega raiz, membros e versão; persiste mapeamento explícito sem expor entidade JPA.
- `IdentityReservationRepository` reserva/libera por correção comprovada/transfere propriedade canônica; conflitos de unicidade são tipados. Não permite liberar por inativação/mesclagem.
- `DuplicateCandidateRepository` pertence à US1: retorna todos os candidatos administrativos relevantes e versões para AS-003, sem paginação/truncamento. `PostgresDuplicateCandidateAdapter` consulta nascimento idêntico ou nome normalizado idêntico com nascimento igual à inversão válida de dia/mês. Índices B-tree por nascimento e nome normalizado/nascimento nascem com o cadastro; a política Java aplica a aproximação exata depois. Consulta/autorização são parte do cadastro/revisão auditados; qualquer projeção exibida confirma evento antes da resposta. Testes PostgreSQL comprovam obtenção real dos candidatos.
- `PatientSearchRepository` pertence à US2 e retorna somente a projeção paginada da busca pública. SQL é parametrizado; sort e tamanho são fixados/validados. Candidatos da US1 não dependem desse contrato, de paginação ou de pg_trgm.
- `UnitOfWork` delimita transação de banco e coordenação por raiz. `MergeCoordinator` bloqueia raízes em ordem determinística e revalida versões/membros. Qualquer escritor de filho participa da mesma coordenação.
- `AuditWriter` persiste evento minimizado; falha é fatal para acesso protegido. `AuditQuery` só expõe dados ao caso de uso gerencial. `OperationReceiptRepository` arbitra mesma chave/payload de forma durável e atômica com efeito.
- Repositórios específicos preservam consentimentos, períodos, anexos e origens; leitura de conjunto canônico não altera dados históricos.

### Porta de arquivos

`QuarantineStore` recebe stream limitado e gera referência privada; `FileInspector` verifica assinatura, parser seguro e scanner, retornando CLEAN/REJECTED/UNAVAILABLE sem conteúdo; `PrivateAttachmentStore` publica bytes duráveis em namespace administrativo/clínico e retorna chave interna. `AdministrativeContentReader` aceita somente referência administrativa válida autorizada, sem aceitar caminho ou chave arbitrária da apresentação. Não existe `ClinicalContentReader` no módulo.

Ordem: autorizar → receber/inspecionar em quarentena → persistir arquivo privado → abrir transação curta, revalidar raiz/versão/estado → vínculo+recibo+evento → commit → confirmar. Se houver conflito/falha, nenhum vínculo de sucesso; arquivo órfão segue reconciliação segura. Nenhuma chamada de scanner durante lock de mesclagem. Credenciais, caminhos e resultado bruto do scanner ficam fora dos DTOs.

### Revogação com evidência nova

`RevokeConsent` recebe exatamente uma fonte: evidenceAttachmentId existente válido ou newAdministrativeEvidence limitado. Evidência existente deve ser ADMINISTRATIVO/VALIDO do mesmo conjunto; evidência nova usa categoria EVIDENCIA_REVOGACAO fixada pelo servidor e o mesmo pipeline privado. A referência de preparação do novo arquivo é interna ao comando, vinculada ao ator/paciente/operationId e não é um Attachment de negócio, token público ou rota de upload separada.

Depois da preparação, revalidar permissão, raiz, versão e estado ATIVO/INATIVO. Criar o Attachment administrativo, inserir a decisão REVOGADO, atualizar uma vez a versão, inserir os dois eventos de anexação/revogação e confirmar recibo na mesma UnitOfWork. Falha reverte todos; conteúdo físico órfão fica privado para reconciliação. A nova evidência não fica avulsa nem disponível se a manifestação falhar. O retorno aponta a decisão e sua referência de prova; mesma chave/payload não duplica anexo, decisão ou eventos. Replay reautoriza e resolve recibo já confirmado antes de tentar novamente a mutação por versão antiga.

`UploadAttachment` genérico continua negado em INATIVO, mesmo se o cliente enviar categoria ou flag de revogação. A exceção pertence à capacidade de RevokeConsent, sem bypass fornecido pelo navegador. Mantém paciente inativo, bloqueia concessão, alterações cadastrais e envio clínico nesse fluxo. Invalidar uma prova de revogação posterior não restaura autorização anterior. Em MESCLADO, exigir revisar o destino antes de comando, sem redirecionamento silencioso.

### Identidade e convênio para módulos futuros

`ResolvePatientIdentity(originalReference)` → canonicalPatientId, medicalRecordNumber do destino, originalReference, merged flag e membros de proveniência. Não renumera nem permite acesso implícito; consumidor fornece contexto autorizado.

`CoverageStatus(patientReference, referenceDate)` → referenceDate, `currentPreference` explicitamente atual e segmentos de cobertura com origem, startsOn/endsOn/expiresOn, inForceOnDate e expiredOnDate. Data deve ser válida e finita. Vigência: startsOn ≤ D e endsOn ausente ou D < endsOn. Validade: vencido se expiresOn < D, válido no próprio dia. Não filtrar histórico pelo estado ATUAL/ENCERRADO atual. Cobertura encerrada depois de D participa da consulta de D; renovação posterior não muda a validade do segmento anterior.

`MaintainCoverage` fecha o segmento anterior e cria sucessor ao renovar/alterar validade ou dados materiais, com supersedesCoverageId, períodos sem sobreposição na cadeia e origem preservada. Substituição do vínculo preferido exige escolha explícita por sucessor/outro vínculo atual ou PARTICULAR; sem vínculo atual, PARTICULAR. A consulta histórica é somente leitura: não infere preferência passada, não reabre vínculo, não altera preferência atual ou atendimentos. Agenda/check-in futuros decidem seu próprio fluxo.

### Resumos de atendimento

`EncounterSummaryProvider.fetch(canonicalIdentityMembers, pageRequest)` → resultado discriminado:

- `NotConfigured`: nenhuma fonte implementada; não significa ausência de atendimentos.
- `Available(items, nextPage)`: itens somente data, profissional, tipo, sourceSystem e externalEncounterId; ordenar por data desc e desempate estável.
- `Empty`: fonte integrada consultada com sucesso e sem registros.
- `Unavailable`: timeout/falha de fonte existente; mensagem controlada.

Adaptador inicial `UnconfiguredEncounterSummaryProvider` retorna sempre NotConfigured. Testes substituem por fonte controlada que preserva associações por originalReference. Futuro adaptador valida resposta, limites/paginação, tamanho dos campos e timeout; nunca aceita campos clínicos em sua projeção. Não persistir fixtures em produção. Nenhum caso de uso escreve resumos.

## Concorrência, repetição e consentimento

Prévia de duplicidade é vinculada ao payload normalizado e candidatos/versões; mudança de identificação exige nova revisão. Identificador idêntico bloqueia mesmo com justificativa. AS-003 sem identidade documental idêntica permite continuar com motivo auditado; telefone compartilhado nunca bloqueia.

Prévia de mesclagem recebe sourceEvidenceAttachmentId e destinationEvidenceAttachmentId, um documento ADMINISTRATIVO/VALIDO de cada conjunto canônico correspondente. Exigir nome completo igual após somente caixa/acentos/espaços repetidos, nascimento idêntico, conferência de ambas pelo gerente e justificativa obrigatória. AS-003 aproximado e dia/mês invertidos não satisfazem igualdade para mesclar; CPFs válidos divergentes continuam bloqueando. O gerente consulta documentos pela porta administrativa autorizada/auditada; não usar OCR nem conteúdo clínico para decisão automática.

Confirmação é vinculada ao mesmo gerente e prévia de uso único. Dentro da transação, bloquear raízes em ordem, revalidar versões/membros/reservas, igualdade dos dados e pertencimento/classificação/validade das duas provas. Qualquer alteração/invalidação de filho coordena com a raiz e incrementa sua versão; prova alterada desde a prévia exige nova revisão e não autoriza consolidar. Persistir referências das duas provas em MergeRecord e evento, sem copiar conteúdo/valores, consumir prévia e confirmar recibo no mesmo commit. Resolução administrativa inclui contato de emergência único, responsáveis atuais e preferência; históricos permanecem. Não abrir exceção gerencial nem permitir upload avulso em raiz inativa para obter prova de merge.

Projeção de consentimento agrega eventos de todos os membros. Conflitos e revogações não são anulados por aceite no destino ou timestamp mais recente de outra origem. Nova autorização precisa de nova manifestação documentada que resolva o conflito. Consulta dessa projeção não declara que todos os tratamentos dependem de consentimento.

Replay de comando verifica autenticação/autorização novamente; mesma chave e fingerprint confirmado retorna o mesmo resultado, sem repetir efeito ou evento de mutação. Cada nova leitura do resultado recebe seu evento. Chave igual com payload diferente gera conflito; rollback permite tentativa segura. Um recibo nunca armazena HTML sensível como resposta em cache.
