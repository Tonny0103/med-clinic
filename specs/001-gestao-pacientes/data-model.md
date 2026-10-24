# Phase 1 — Modelo de dados

Referências: [spec.md](spec.md), [plan.md](plan.md) e [contratos](contracts/application.md). Modelo lógico para migrações futuras; este documento não cria tabelas.

**Revisão**: incorpora Clarifications 2026-10-24 — prova nova na revogação de inativo, provas de ambas as raízes na mesclagem e reconstrução histórica de convênio.

## Convenções

- UUID para referências internas; prontuário é string numérica de 12 posições, gerada por sequência positiva não cíclica, com índice único e sem reutilização. Lacunas por rollback são aceitáveis. Esgotamento bloqueia geração, nunca trunca ou recicla. Campo é imutável no domínio e protegido no banco.
- Instantes em UTC (`timestamptz`); nascimento, validade e vigência são datas locais (`date`). `Clock` + fuso IANA obrigatório da clínica determina hoje/maioridade. Não impor idade máxima artificial.
- `version` inteiro crescente no agregado Patient. Qualquer mutação de filhos coordena com a versão do paciente, inclusive anexação, consentimento e invalidação. Origem de vínculo nunca é reescrita.
- Campos livres são limitados e escapados na saída: nome 200, motivo 500, parentesco/emissor/plano 120, número documental 80, endereço por linha 200, e-mail 254 caracteres. Limites são parâmetros técnicos iniciais; rejeitar com mensagem, sem truncar. Endereço permite caracteres Unicode e formatos estrangeiros.
- FK, NOT NULL, CHECK e unicidade no banco complementam regras do domínio. Dados sensíveis ficam somente nas tabelas de negócio protegidas; auditoria não guarda cópias de valores.

## Entidades e relacionamentos

### Patient — Paciente (FR-001–015, FR-028)

| Campo | Tipo / regra |
| --- | --- |
| id / medicalRecordNumber | UUID / string única, imutável e nunca reutilizada |
| canonicalPatientId | FK Patient; si próprio antes de mesclagem; raiz final após consolidação |
| fullName / normalizedName | Nome obrigatório / projeção de busca com normalização versionada |
| birthDate | Data obrigatória, não futura |
| nationalityCountry | ISO de país; necessário para avaliar exceção de estrangeiro, distinto do país do endereço |
| informedSex | FEMININO, MASCULINO, OUTRO ou NAO_INFORMADO; seleção explícita |
| phone / email | Telefone obrigatório, e-mail opcional validado quando presente |
| address | Valor composto: país, linha principal, complemento opcional, localidade, região e código postal conforme país |
| status | ATIVO, INATIVO ou MESCLADO |
| carePreference / preferredCoverageId | PARTICULAR ou CONVENIO; FK de vínculo do conjunto canônico quando escolhido |
| createdAt / updatedAt / version | Instantes e controle de concorrência |

Telefone aceita formato internacional, normalizado com país explícito e validação de formato/comprimento; para Brasil, DDD e número aplicáveis. Não comprova titularidade nem exige exclusividade. Endereço exige país, linha principal e localidade; região/código postal seguem formato aplicável, sem CEP brasileiro para país estrangeiro. Compartilhamento de endereço/telefone é permitido.

Cadastro e edição comum exigem nome, nascimento, sexo informado, identificação, telefone e endereço completos, inclusive quando e-mail está ausente. Menor é idade estritamente menor que 18 na data local; exige responsável atual. Revalidar obrigatórios em atualizações, preservando entradas válidas nos erros.

### IdentityDocument e IdentityReservation — Identificação (FR-003–004, FR-014, FR-028)

`IdentityDocument`: id, originalPatientId, kind (CPF/ALTERNATIVO), documentType, normalizedNumber, country, issuer, substitutionReason, validFrom, endedAt, state (VIGENTE/SUBSTITUIDO/CORRIGIDO), correctionReason, correctionEvidenceAttachmentId e recordedBy. CPF mantém exatamente 11 dígitos como texto e passa dígitos verificadores; sequências repetidas são inválidas. Alternativo exige tipo, número, país, emissor e motivo de substituição do CPF.

`IdentityReservation`: chave documental canônica globalmente única, canonicalPatientId e identityDocumentId. CPF usa chave por dígitos; alternativo usa tupla tipo/número/país/emissor normalizados, com regra documentada por tipo (sem remover caracteres significativos arbitrariamente). A chave é única independentemente da situação do paciente. Índices e reserva são criados/alterados na mesma transação do cadastro.

- Sem CPF: somente menor ou estrangeiro pode usar documento alternativo; menor também exige responsável. Adulto brasileiro precisa de CPF.
- Acrescentar CPF posterior preserva o documento alternativo legítimo e sua reserva, sem criar paciente. Histórico não é apagado.
- Correção de identificação já registrada só em ATIVO, por recepção/gerente, com motivo e anexo administrativo válido do mesmo conjunto de identidade. Nova chave é validada/reservada atomicamente; registro incorreto vira CORRIGIDO e deixa de ser identificação vigente/localizável. Reserva comprovadamente errada é liberada, preservando histórico e auditoria; não manter associação falsa.
- Identificador legítimo vigente em origem antes de mesclar segue reservado ao destino. Cadastro inativo ou mesclado nunca libera documento por causa da situação. Uma reserva tem um proprietário canônico; referências históricas não são novas identidades.

### LegalGuardian — Responsável legal (FR-006)

id, originalPatientId, name, relationship, documentType/number/country/issuer, phone, startsOn, endsOn, recordedBy e recordedAt. Períodos usam início inclusivo e fim exclusivo. Menor precisa de pelo menos um período atual; encerrar o último sem substituto é bloqueado. Documento do responsável não satisfaz identificação do menor. Completar 18 anos não apaga históricos e não cria consentimento.

### EmergencyContact — Contato de emergência (FR-007)

id, originalPatientId, name, relationship, phone, startsOn e endsOn. No máximo um contato atual no conjunto canônico; se informado, exigir os três campos em conjunto. Não tem semântica de responsabilidade legal; pode compartilhar telefone com paciente/responsável.

### InsuranceCoverage — Vínculo de convênio (FR-016–018)

id, originalPatientId, insurer, plan, membershipNumber (texto), expiresOn obrigatório, startsOn, endsOn opcional, supersedesCoverageId opcional (FK para segmento anterior) e state (ATUAL/ENCERRADO). Operadora, plano e carteirinha são obrigatórios. Não há unicidade global de carteirinha. Cada segmento preserva os dados e a validade de seu período; alterações de número/operadora/plano ou renovação/alteração de expiresOn encerram o segmento anterior e criam sucessor, sem sobrescrever seus valores. Supersedes referencia a mesma cadeia/origem, sem ciclos; períodos sucessivos não se sobrepõem. Vínculos diferentes podem coexistir.

O período é `[startsOn, endsOn)`: início inclusivo e endsOn como primeira data fora da vigência, quando presente; exigir endsOn ≥ startsOn. Igualdade preserva segmento sem dia vigente, por exemplo correção/substituição no mesmo dia; não o apresentar como vigente em nenhuma data. `inForceOnDate` é verdadeiro quando startsOn ≤ D e endsOn está ausente ou D < endsOn, independentemente do state atual. `expiredOnDate` é verdadeiro quando `expiresOn < D`; no próprio dia, válido. Vigência no período e carteirinha vencida são informações separadas. Um segmento ENCERRADO hoje deve aparecer como vigente na consulta de D anterior ao encerramento se o período contiver D. A validade usada é a daquele segmento, nunca a de um sucessor posterior.

Preferência CONVENIO só aponta para vínculo atual; se todos encerrados, usar PARTICULAR. Substituição de vínculo preferido exige escolha explícita por sucessor/outro vínculo atual ou PARTICULAR na mesma alteração autorizada, preservando o registro anterior; vencimento isolado não escolhe outro plano. A consulta retorna `currentPreference`, data de referência, segmentos/proveniência, inForceOnDate e expiredOnDate; não infere preferência passada, reabre cobertura ou reescreve atendimentos.

### Attachment — Documento anexado (FR-022–025)

id (referência neutra), originalPatientId, category, classification (ADMINISTRATIVO/CLINICO_RESTRITO), storageKey aleatória, detectedMediaType, byteSize, integrityDigest, uploadedBy, uploadedAt, securityScanReference, state (VALIDO/INVALIDADO), invalidatedBy/At e invalidationReason.

Não persistir nome original revelador para apresentação/logs. Exames, encaminhamentos e categorias incertas são CLINICO_RESTRITO. Só PDF/JPEG/PNG não vazios ≤10.485.760 bytes; estrutura compatível e inspeção limpa antes da confirmação. Um documento invalidado mantém arquivo/classificação e sai de consultas comuns. Não reclassificar para reduzir restrição, não transferir para outro paciente, não excluir via operação de usuário.

Arquivo e banco têm fronteira de consistência: conteúdo durável privado primeiro, vínculo+evento depois. Falha deixa apenas órfão inacessível, nunca vínculo confirmado para arquivo ainda não gravado. Reconciliação respeita operações em andamento e distingue temporário sem referência de original vinculado/invalidado, cuja retenção depende de política aprovada.

Categoria EVIDENCIA_REVOGACAO é administrativa e fixada pelo servidor no envio composto de revogação. Em INATIVO, novo Attachment dessa categoria só é criado junto da ConsentDecision REVOGADO correspondente no mesmo commit; o caso genérico de anexação continua negado. A preparação privada do arquivo não cria vínculo consultável e não tem endpoint público próprio.

### ConsentDecision — Manifestação (FR-019–021)

Registro imutável: id, originalPatientId, purpose (PROTECAO_DADOS/TERMOS_ATENDIMENTO/IMAGEM), termVersion, decision (CONCEDIDO/RECUSADO/REVOGADO), manifestedAt, recordedAt, declarantName/reference, declarantType (TITULAR/RESPONSAVEL), guardianId quando aplicável, operatorId e evidenceAttachmentId administrativo válido. Revogação referencia decisão anterior quando identificável. A evidência precisa pertencer ao mesmo conjunto canônico e manter proveniência.

Não atualizar manifestação para alterar decisão: inserir novo evento documentado. Registro tardio não altera a data da manifestação; ambas são preservadas. Antes de liberar um novo aceite, confrontar histórico conhecido, termos e responsável aplicáveis. Mudança de cadastro/termo/responsável não gera CONCEDIDO. Invalidar evidência não apaga manifestação e torna sua autorização não utilizável até revisão documental.

RevokeConsent exige exatamente uma fonte de evidência: ID administrativo válido existente ou novo arquivo administrativo no próprio comando multipart. Para prova nova, criar Attachment+ConsentDecision, atualizar uma vez a versão da raiz e gravar eventos de anexação/revogação+recibo em transação única. O recibo aponta a decisão, pela qual a referência do anexo pode ser recuperada no replay. Falha de qualquer gravação reverte todos os vínculos; conteúdo já gravado permanece órfão privado. Paciente INATIVO permanece assim e não recebe anexação avulsa. Invalidar depois a prova de uma revogação não restaura autorização anterior; manter a projeção conservadora sem apagar o evento.

Estado efetivo derivado por finalidade: SEM_MANIFESTACAO, CONCEDIDO, RECUSADO, REVOGADO ou CONFLITANTE. Recusa/revogação permanece histórica; novo aceite posterior exige manifestação explícita documentada que resolva os eventos anteriores. Conflito temporal ou entre origens não é resolvido apenas pelo maior timestamp. Na mesclagem, qualquer conflito/revogação mantém finalidade sem autorização até nova manifestação no conjunto consolidado. Recusa de imagem não bloqueia cadastro nem altera outra finalidade.

### EncounterSummary — Projeção externa (FR-013)

sourceSystem, externalEncounterId, originalPatientReference, occurredAt/date, professionalDisplayName e encounterType. Não contém diagnóstico/evolução/prescrição. Não criar tabela de atendimento nem formulário de manutenção nesta entrega. Chave composta origem+ID externo identifica registro; não deduplicar pelo conteúdo. Porta recebe todos os membros canônicos para preservar associações após mesclagem, ordena data desc e identidade da fonte como desempate. Estados NÃO_CONFIGURADO, VAZIO, DISPONIVEL e FALHA permanecem distintos.

### MergeRecord — Mesclagem (FR-026–029)

id, sourcePatientId, destinationPatientId, memberIdsAtMerge, sourceVersion, destinationVersion, sourceEvidenceAttachmentId obrigatório, destinationEvidenceAttachmentId obrigatório, administrativeResolution (nomes dos campos e referências de escolhas, sem cópia clínica), reason, managerId, mergedAt e operationId. Registro imutável. Origem preserva prontuário, atributos históricos e vínculos originais; destino fornece ficha administrativa resolvida.

Antes da consolidação, cada prova deve ser Attachment VALIDO/ADMINISTRATIVO pertencente ao conjunto canônico da raiz correspondente; referência do outro lado, invalidada ou clínica não satisfaz a condição. Exigir nome completo igual após apenas caixa/acentos/espaços repetidos, nascimento idêntico e confirmação justificada do gerente de que conferiu ambas. Aproximações AS-003, inversão dia/mês e confirmação gerencial isolada não bastam. CPF válido divergente continua bloqueando. Guardar somente referências das provas no registro/evento; o gerente consulta conteúdo administrativo pelo fluxo autorizado/auditado, sem OCR ou leitura clínica.

`canonicalPatientId` materializa associação direta à raiz, sem ciclos. Se uma raiz já possui origens e depois é mesclada, todos os seus membros passam atomicamente à nova raiz. Só origem/destino ainda canônicos podem iniciar nova consolidação; origens MESCLADO nunca são reativadas. A situação ativa/inativa do destino é preservada; conflitos de responsáveis, emergência e preferência são resolvidos na prévia sem apagar históricos.

### AuditEvent — Auditoria (FR-031–034)

id, actorId (`issuer`/`subject` ou referência interna estável), activeRole, occurredAt UTC, operation, objectType, objectId opcional, originalPatientId opcional, result, changedFieldNames, reason quando obrigatório, evidenceAttachmentId opcional, mergeSourceId/destinationId, mergeSourceEvidenceAttachmentId e mergeDestinationEvidenceAttachmentId obrigatórios para mesclagem, e correlationId aleatório. Busca guarda somente tipo de critério, contagem/estado e paginação; nunca termo ou CPF. Não guardar snapshots, conteúdo documental, contatos, tokens nem valores anteriores/novos. A revogação com prova nova produz eventos distintos de anexação e revogação no mesmo commit e correlationId; replay não os duplica.

Append-only: runtime sem UPDATE/DELETE/ownership; migração usa identidade separada. Consulta por paciente consolidado inclui membros sem reescrever eventos. Filtro por período usa intervalo UTC [início,fim), com UI em fuso local, e ordenação instante desc/id, páginas de 20. Consulta da própria auditoria gera um único evento fora do mecanismo recursivo.

### OperationReceipt e MergePreview — Suporte de aplicação

`OperationReceipt`: operationId aleatório, actorId, operationType, requestFingerprint protegido por HMAC com chave externa, status de confirmação, resultObjectId/resultCode e committedAt; chave única actor+tipo+operationId. Sem corpo/HTML ou dados pessoais em claro. Negócio e recibo confirmam juntos; mesma chave/payload retorna referência do resultado, payload divergente gera conflito. Retenção técnica mínima de 30 dias, a aprovar antes de produção; antes de remover recibo, preservar tombstone da chave usada para impedir repetição tardia.

`MergePreview`: token aleatório de uso único vinculado ao gerente, origem/destino, versões, sourceEvidenceAttachmentId e destinationEvidenceAttachmentId obrigatórios, confirmação de conferência de ambas/justificativa, IDs dos conflitos/escolhas e expiração de 15 minutos. Guardar no servidor; não confiar em valores ocultos de conflito. Confirmar reconsulta validade/classificação/pertencimento das duas provas e dados das raízes. Toda invalidação/alteração de filho incrementa a versão da raiz, dispensando versão separada de anexo para esse controle; qualquer mudança invalida confirmação. Filtros de busca também ficam em sessão privada com token opaco de 15 minutos, sem termo em URL.

## Transições e atomicidade

| Objeto | Transição | Condições / efeito |
| --- | --- | --- |
| Patient | criação → ATIVO | obrigatórios, reservas, responsável se menor, recibo e auditoria juntos |
| Patient | ATIVO → INATIVO | recepção/gerente, motivo e confirmação; preserva vínculos/reservas |
| Patient | INATIVO → ATIVO | recepção/gerente, motivo; mesmo ID/prontuário, volta à busca padrão |
| Patient | ATIVO ou INATIVO → MESCLADO | gerente; prévia/identidade confirmadas; destino canônico distinto |
| Patient | MESCLADO → qualquer | proibido; somente resolução para destino |
| Attachment | quarentena → VALIDO | inspeção limpa + conteúdo durável + tx de vínculo/evento |
| Attachment + ConsentDecision | prova nova → VALIDO + REVOGADO | comando de revogação ATIVO/INATIVO; anexo+decisão+eventos+recibo atômicos, sem anexação avulsa ao inativo |
| Attachment | VALIDO → INVALIDADO | gerente e motivo; preserva original, sem redução de restrição |
| ConsentDecision | novo evento → projeção atualizada | manifestação documentada; não editar/apagar evento anterior |

Em INATIVO bloquear manutenção comum, novos vínculos, novas concessões e anexação avulsa; permitir leitura, reativação, revogação e invalidação autorizada. A única exceção de novo anexo é a evidência administrativa criada dentro da revogação no mesmo commit, mantendo o paciente inativo. Mesclagem é operação gerencial própria sobre raízes, inclusive inativas, desde que suas provas já estejam vinculadas; nova prova para mesclar não habilita upload avulso de inativo. Em MESCLADO, telas leem destino e indicam origem; mutações com ID antigo não são redirecionadas silenciosamente.

Todo escritor adquire coordenação com a raiz e confere versão/status novamente. Mesclagem bloqueia raízes em ordem determinística, reconsulta membros/reservas, duas provas e dados de identidade e confirma versões da prévia. Impede auto-mesclagem, ciclos, origem já mesclada, ausência/prova inválida ou alheia, nome/nascimento não coincidentes, conferência gerencial sem justificativa e CPFs vigentes válidos divergentes, sem exceção gerencial. Atualiza membros/reservas, estado, resolução administrativa, referências das duas provas, recibo e evento na mesma transação. Auditoria e filhos mantêm origem; consultas agregam membros. Falha reverte tudo. Não chamar scanner/fonte externa durante locks.

## Índices e verificações de integração

- Únicos: prontuário, chave de reserva documental e chave de operação; FK canônica e verificação de consistência de raiz/estado.
- Candidatos da US1: B-tree nascimento e nome normalizado/nascimento na migração inicial; DuplicateCandidateRepository consulta todos os candidatos relevantes e a política Java aplica AS-003. Não depende da busca pública ou de pg_trgm.
- Busca pública da US2: GIN `pg_trgm` no nome normalizado e B-tree nome/prontuário; CPF/prontuário exatos por reserva/índice. Medir também nomes de dois caracteres.
- Filhos por originalPatientId e períodos; anexos por paciente/estado/classificação; auditoria por instante/id, paciente, ator e operação.
- Tests com duas transações reais: mesmo CPF/alternativo, edição simultânea, merge versus invalidação de prova/alteração de filho e replay após commit. Verificar revogação multipart de inativo sem anexo avulso, ausência de sucesso parcial, reserva de inativo/mesclado, liberação de identificação comprovadamente corrigida e preservação de origens/referências das duas provas. Testar consulta anterior ao encerramento e renovação posterior preservando expiresOn antigo.
