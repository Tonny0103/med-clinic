# Segurança, arquivos e auditoria — v1

Normas de produto: FR-015/020/022–034, matriz de acesso e QR-003/004 em [spec.md](../spec.md), incluindo Clarifications 2026-10-24. Este contrato descreve controles técnicos, sem declarar aprovação jurídica ou política de retenção.

## Fronteiras de confiança

1. **Navegador → MVC**: sessão OIDC, CSRF, allowlist de campos, validação de tipo/tamanho/formato e semântica no servidor. IDs ocultos/versões/tokens são dados não confiáveis. Autorizar ação e pertencimento do objeto antes de materializar dados.
2. **OIDC → aplicação**: issuer configurado, validação de assinatura/claims pelo Spring Security, mapeamento explícito de perfis permitidos e usuário desconhecido sem acesso. Segredos externos; nenhum cadastro/senha local em produção. Renovação/expiração de sessão deve reavaliar autorização conforme configuração do provedor.
3. **Aplicação → PostgreSQL**: consultas parametrizadas, contas separadas de migração/runtime, constraints, locks e TLS. Runtime não possui tabelas nem altera/apaga eventos de auditoria. Domínio não recebe conexão/entidade JPA.
4. **Aplicação → volume/scanner**: volume fora da raiz web, sem URL pública, listagem ou execução. Chaves aleatórias não derivadas de filename; nomes originais não são exibidos. Scanner privado com atualização de assinaturas e timeout; nunca enviar documentos a serviço público. Namespace clínico não é servido por qualquer rota ou perfil.
5. **Fonte futura → resumos**: resposta validada e projetada em somente data/profissional/tipo; falha controlada distinta de vazio. Sem fonte inicial, usar estado explícito, nunca dados inventados.

Segredos, DSNs com credenciais, tokens, corpos de formulários, termos de busca, CPF, contato e conteúdo de documentos são proibidos em logs/APM. Desativar log SQL de parâmetros e request bodies, mascarar headers sensíveis e usar templates de rota em métricas. Desabilitar cache HTTP de páginas/documentos; não armazenar respostas pessoais em service worker ou armazenamento persistente do navegador.

## Política de acesso

| Operação | RECEPCAO | GERENTE | MEDICO |
| --- | --- | --- | --- |
| Busca, ficha, resumo e conteúdo administrativo válido | Sim | Sim | Sim |
| Cadastro/manutenção de ativo, responsáveis, convênios, inativação/reativação | Sim | Sim | Não |
| Correção de identificação com comprovação/motivo | Sim | Sim | Não |
| Manifestação/revogação e upload | Sim | Sim | Não |
| Invalidar documento, mesclar, consultar auditoria | Não | Sim | Não |
| Conteúdo clínico, exclusão definitiva, alterar auditoria | Não | Não | Não |

Negar por padrão, tanto na entrada HTTP quanto na política de aplicação. Não combinar papéis para conceder leitura clínica. Paciente não acessa o sistema. INATIVO permite leitura, revogação, invalidação e reativação autorizadas; novo anexo administrativo só é permitido dentro da revogação correspondente no mesmo commit, sem reativar. Anexação avulsa, concessão e outras alterações comuns continuam bloqueadas. MESCLADO só resolve ao destino; reativação/edição independente é proibida. Escopos futuros não herdam leitura clínica deste módulo.

## Protocolo de arquivos

1. Autorizar operador/capacidade: paciente ATIVO para anexação genérica; ATIVO ou INATIVO exclusivamente para prova administrativa nova dentro de RevokeConsent. Parâmetro/flag de revogação no uploader genérico não cria exceção. Aplicar limites de requisição/multipart e stream real ≤10 MiB por arquivo, comportando metadados adicionais. CSRF deve ser validado sem disponibilizar bytes recebidos.
2. Gravar quarentena com nome aleatório, permissões privadas, sem execução e sem preview. Normalizar categoria por allowlist; incerta, exame e encaminhamento são clínicos. Detectar tipo por assinatura e parser seguro; MIME/extensão isolados não bastam.
3. Validar PDF/JPEG/PNG, não vazio, estrutura íntegra, limites de recursos e scanner limpo. Rejeitar arquivo protegido por senha/não inspecionável e conteúdo inseguro. Timeout/indisponibilidade não equivale a CLEAN e não publica vínculo.
4. Persistir bytes em diretório definitivo privado; garantir durabilidade antes do commit de vínculo. Revalidar raiz/versão/status e confirmar vínculo+recibo+evento na mesma transação. Na revogação com prova nova, confirmar também ConsentDecision REVOGADO e os dois eventos de anexação/revogação no mesmo commit, sem publicação avulsa. A referência de preparação é interna ao mesmo comando/ator/paciente, sem endpoint/token público de staging. Só então mostrar sucesso; resposta nunca contém filename revelador para clínicos.
5. Falha pré-commit não cria vínculo; reconciliação remove apenas temporários/órfãos comprovados sem referência no banco primário e sem operação ativa. Janela técnica proposta: temporários/órfãos após 24 h, a aprovar; limpar nunca é retenção de documento de negócio. Original válido/invalidado não entra nessa rotina e depende da política aprovada.
6. Download administrativo verifica autorização, vínculo canônico, estado e classificação; abre conteúdo para leitura técnica, confirma auditoria e só então envia bytes. Sem auditoria, não transmitir. Conteúdo clínico ou invalidado jamais é entregue. Sem thumbnails, OCR, extração, URL assinada, link de filesystem ou endpoint alternativo.
7. Invalidação só por gerente e motivo; preserva classificação, arquivo, autoria e origem. Não reclassificar para baixar restrição. Corrigir paciente/categoria exige invalidar e reenviar. Suspeita de documento clínico marcado administrativo requer invalidação/restrição imediata e investigação auditada.

Usar credenciais/permissões separadas por adaptador quando suportado pelo volume: scanner lê quarentena, leitor web lê somente administrativos; namespace clínico aceita ingestão, sem função de leitura na aplicação web. Capacidade técnica de scanner/operador de infraestrutura não cria permissão de leitura para usuário. Criptografia de volume e backups utiliza chaves externas; restauração deve preservar correspondência entre banco/arquivos e validar integridade sem expor conteúdo.

## Provas da mesclagem

Exigir sourceEvidenceAttachmentId e destinationEvidenceAttachmentId administrativos válidos de seus respectivos conjuntos canônicos, nomes iguais após apenas caixa/acentos/espaços repetidos, nascimento idêntico, conferência de ambas e justificativa gerencial. Prova clínica, inválida, alheia ou ausente bloqueia; CPF válido divergente e aproximação AS-003 não são superados pela confirmação. A consulta das provas usa o controlador administrativo autorizado/auditado, sem extração clínica.

Prévia associa IDs das duas provas, versões e gerente. Confirmar revalida estado/classificação/pertencimento das provas e dados sob coordenação das raízes; invalidação/alteração de filho incrementa a versão e exige nova revisão. Registro/evento mantêm IDs originais sem copiar conteúdo documental. Não abrir upload avulso em paciente inativo para obter prova de mesclagem; reativação/regularização autorizada continua um fluxo próprio, quando aplicável.

## Protocolo de auditoria e indisponibilidade

- **Escrita bem-sucedida**: negócio, evento e recibo de idempotência na mesma transação. Erro de auditoria faz rollback e retorna 503; nenhum sucesso parcial.
- **Revogação com prova nova**: Attachment+ConsentDecision, incremento de versão, eventos de anexação/revogação e recibo compartilham o commit. Falha reverte todos os registros; bytes órfãos não são consultáveis. Replay recupera o resultado confirmado sem novo anexo/manifestação/evento, mantendo INATIVO. Invalidar posteriormente uma prova de revogação não restaura autorização anterior.
- **Leitura/busca/vazio**: materializar DTO limitado, persistir evento e confirmar transação antes de renderizar/streamar. Sem lazy loading protegido posterior ao gate. Não abrir transação read-only que impeça inserir evento.
- **Acesso negado/validação/conflito/falha**: após liberar locks e rollback, tentar evento minimizado em transação curta. Se evento também falhar, retornar 503 sem dados; registrar apenas métrica técnica de indisponibilidade, sem PII. A impossibilidade física de gravar durante queda não autoriza leitura/alteração e não é apresentada como auditoria completa.
- **Consulta de auditoria**: somente gerente; carregar página e gravar um evento AUDIT_READ antes de responder, sem auditar internamente a chamada do writer. Próxima página é nova consulta. Eventos são somente leitura/append na aplicação; acesso privilegiado operacional é controlado fora da interface.
- **Rejeições antes do controlador**: filtros de segurança, tratamento de CSRF, parsing multipart e limite de upload integram o mesmo gate de auditoria. Configurar limites no proxy para encaminhar rejeições auditáveis ao serviço/gateway, sem publicar arquivo; não deixar erro 413/403 sair silenciosamente por caminho alternativo. Registrar somente ator conhecido da sessão ou tentativa não autenticada, operação/código e correlationId, sem confiar no ID de paciente fornecido nem copiar corpo inválido. Verificar esses caminhos por contrato.
- **Repetição**: mesma operação confirmada não cria segundo efeito. Replay reautoriza e audita eventual leitura; nova tentativa após rollback pode confirmar uma única operação. Resultado incerto deve ser resolvido pelo recibo, não por novo operationId automático.

Schema de evento: ID, ator/perfil, instante UTC, operação, objeto/paciente interno se existente, resultado e nomes dos campos alterados. Correção inclui motivo e referência da comprovação, sem CPF antigo/novo. Mesclagem inclui origem/destino/motivo e referências internas das duas provas administrativas, sem copiar valores ou conteúdo. Busca inclui tipo de critério e resultado/contagem, sem texto bruto; tentativas negadas não ecoam dados recebidos.

Motivos têm limite de 500 caracteres e orientação visível para não registrar clínica/segredos; não aceitar anexação de dumps. Mensagens usam códigos e correlationId aleatório. Integridade é garantida por permissões/controles operacionais e backups; append-only não é alegação de inviolabilidade contra administrador do banco.

## Gates de liberação e evidências

- Testar todos os papéis com acesso direto a ações/arquivos; nenhum byte, nome original ou conteúdo clínico em HTML, headers, erros ou URLs. Testar troca de ID de filho/paciente e anexo invalidado.
- Derrubar auditoria em busca, ficha, resumos, download, mutação e consulta de eventos; nenhuma resposta protegida/sucesso é liberado. Injetar falha entre operação e evento e após commit antes da resposta.
- Concorrência: CPF/alternativo, versão, anexação versus merge, duas mesclagens e replay. Validar atomicidade e origem de cada vínculo.
- Verificar revogação multipart de inativo com arquivo novo: upload genérico segue bloqueado, scanner/evento/decisão falhos não deixam anexo avulso e replay não duplica. Verificar provas ausentes/trocadas/clínicas/inválidas e invalidação entre prévia e confirmação de merge; duas referências ficam preservadas em registro/evento sem expor conteúdo.
- Segredos externos e verificação automática de repositório/artefatos. Somente fixtures sintéticas; evidências/screenshots sem dados reais. Métricas nunca rotuladas por paciente/operador.
- Antes de dados reais: clínica aprova finalidades/fundamentos, termos/evidências e retenção, incluindo originais inválidos, auditoria, recibos e backups; provisiona OIDC/perfis e procedimento externo de direitos dos titulares. Inativação/revogação não executa eliminação automática. Falta de política bloqueia dados reais, sem inventar prazo legal.
