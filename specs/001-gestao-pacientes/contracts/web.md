# Contrato HTTP e interface — v1

Contrato para Spring MVC + Thymeleaf, sem API JSON pública. Base `/`; idioma pt-BR. Todos os dados são administrativos e sujeitos à [política de segurança](security-audit.md). Campos e invariantes: [modelo](../data-model.md).

Revisão alinhada às três respostas em Clarifications 2026-10-24; as regras abaixo detalham os mesmos formulários/rotas, sem habilitar manutenção avulsa de inativo.

## Regras comuns

- Autenticação OIDC e sessão obrigatórias. GET só consulta; comandos usam POST, token CSRF e `operationId` único. Formulários de alteração incluem `expectedVersion`. Identificador UUID da URL não concede acesso.
- Sucesso de POST: `303 See Other` para página de resultado/ficha; GET subsequente também é autorizado e auditado. Nenhum POST produz mudança parcial. Resposta perdida permite repetir a mesma chave sem duplicar efeito.
- Validação: `422` com mesmo formulário, erros associados aos campos, resumo focável e entradas válidas preservadas, exceto arquivo que precisa ser selecionado novamente. Não incluir conteúdo de arquivo em resposta.
- Conflito de versão/reserva/prévia/idempotência: `409` e orientação para revisar. Mostrar dados atuais somente se autorizados, após auditoria; não sobrescrever ou reexecutar automaticamente. `403` acesso negado; `404` objeto inexistente sem exposição; `413` tamanho excessivo; `415` formato não permitido; `422` conteúdo inválido/inseguro; `503` dependência obrigatória indisponível, sem detalhes internos. Auditoria indisponível prevalece com `503` sem dados.
- Sessão ausente: navegação para login OIDC, sem guardar corpo de formulário sensível para replay automático. Ações recusadas não provocam transição. Não enviar informações de existência de paciente a usuário sem permissão.
- Páginas protegidas: `Cache-Control: no-store`, `Referrer-Policy: no-referrer`, CSP restritiva, escape Thymeleaf, `X-Content-Type-Options: nosniff`; não usar `th:utext` para entradas. URLs/logs/métricas não contêm CPF, nome ou texto bruto de busca.
- Cada leitura de ficha/resumo/documento/busca/auditoria confirma evento antes do primeiro byte protegido. Denegação, resultado vazio e validação de comando também geram evento minimizado. Auditoria da própria auditoria não é recursiva.

## Rotas e permissões

R = RECEPCAO; G = GERENTE; M = MEDICO. G acumula R. Não há acesso de paciente.

| Método / caminho | Papel | Entrada / resultado |
| --- | --- | --- |
| GET `/pacientes` | R/G/M | Formulário de busca sem listar toda a base |
| POST `/pacientes/buscas` | R/G/M | `kind=NOME/CPF/PRONTUARIO`, `criterion`, `includeInactive`; valida e cria filtro privado; 303 para resultados com token opaco |
| GET `/pacientes/resultados?filtro={token}&pagina={n}` | R/G/M | Token da própria sessão, 15 min; página de até 20, ordenada nome/prontuário; preserva filtros |
| GET `/pacientes/novo` | R/G | Formulário inicial e operationId |
| POST `/pacientes` | R/G | Cadastro completo, identidade, responsável se menor, revisão/justificativa de suspeita; cria prontuário |
| GET `/pacientes/{id}` | R/G/M | Ficha administrativa; origem mesclada indica destino e proveniência |
| GET `/pacientes/{id}/editar` | R/G | Somente ativo; formulário com versão |
| POST `/pacientes/{id}/dados` | R/G | Dados comuns; não permite alterar prontuário, estado ou documento existente por mass assignment |
| POST `/pacientes/{id}/identificacoes` | R/G | Incluir CPF posterior legítimo, preservando documentos anteriores |
| POST `/pacientes/{id}/identificacoes/{documentId}/corrigir` | R/G | Nova identificação, motivo e `evidenceAttachmentId` administrativo válido; somente ativo |
| POST `/pacientes/{id}/inativacao` | R/G | Ativo, versão, motivo e confirmação explícita |
| POST `/pacientes/{id}/reativacao` | R/G | Inativo, versão, motivo e confirmação; MESCLADO sempre bloqueado |
| POST `/pacientes/{id}/responsaveis` | R/G | Registrar/substituir/encerrar períodos; preservar ao menos um atual quando menor |
| POST `/pacientes/{id}/emergencia` | R/G | Definir/substituir/remover contato com confirmação; conjunto de campos completo |
| POST `/pacientes/{id}/convenios` | R/G | Registrar vínculo completo ou sucessor com supersedesCoverageId/período/validade; ao substituir preferido, exigir escolha explícita por sucessor/outro atual/PARTICULAR |
| POST `/pacientes/{id}/convenios/{coverageId}/encerramento` | R/G | Encerrar com período; manter histórico |
| POST `/pacientes/{id}/preferencia` | R/G | PARTICULAR ou vínculo atual do mesmo conjunto; mostrar vencimento |
| GET `/pacientes/{id}/consentimentos` | R/G/M | Histórico e estado por finalidade; sem aceite implícito |
| POST `/pacientes/{id}/consentimentos` | R/G | Finalidade, termo, decisão, datas, manifestante/responsável e evidência; sem pré-seleção |
| POST `/pacientes/{id}/consentimentos/revogacoes` | R/G | Multipart com manifestação/versão/operationId e exatamente um evidenceAttachmentId administrativo válido ou evidenceFile novo; ATIVO/INATIVO, prova nova+revogação no mesmo commit |
| GET `/pacientes/{id}/documentos` | R/G/M | Administrativos válidos e metadados clínicos neutros; sem inválidos na consulta comum |
| POST `/pacientes/{id}/documentos` | R/G | Somente ATIVO; multipart com arquivo/categoria/versão/CSRF/operationId, confirmado após scanner/armazenamento/commit. Em INATIVO, 409 mesmo com flag/categoria de revogação |
| GET `/pacientes/{id}/documentos/{attachmentId}/conteudo` | R/G/M | Somente administrativo válido e pertencente ao conjunto; clínico sempre 403 auditado |
| POST `/pacientes/{id}/documentos/{attachmentId}/invalidacao` | G | Motivo obrigatório; preserva conteúdo/origem/classificação; permite inativo |
| GET `/pacientes/{id}/historico` | R/G/M | Somente data, profissional e tipo; sem formulário de lançamento |
| GET `/mesclagens/nova` | G | Seleção de origem/destino por referências internas autorizadas |
| POST `/mesclagens/previas` | G | Raízes/versões, sourceEvidenceAttachmentId e destinationEvidenceAttachmentId válidos, conferência das provas, justificativa e escolhas; nome normalizado/nascimento iguais, prévia 200 sem mesclar e token de 15 min |
| POST `/mesclagens` | G | Token vinculado ao gerente/dados/provas da prévia e confirmação explícita; revalidar versões/identidade/provas, persistir referências das duas e motivo; 303 para destino |
| GET `/auditoria` | G | Filtros período/usuário/paciente/operação; sem retornar lista sem período válido |
| POST `/auditoria/buscas` | G | Valida filtros e gera token de sessão; consulta é auditada uma única vez por leitura |
| GET `/auditoria/resultados?filtro={token}&pagina={n}` | G | Até 20 eventos, instante desc/id; incluir origens se filtro por destino |

Sem rotas DELETE, PUT de auditoria, download clínico, prévia clínica ou atualização manual de resumos. GET de conteúdo revalida classificação/status, mesmo quando link não aparece na tela. IDs de filhos devem pertencer à identidade canônica solicitada. Comando para origem mesclada retorna 409 e exige revisar destino, sem transferência silenciosa da intenção.

## Contrato de busca

Nome: mínimo dois caracteres após trim, sem distinguir caixa/acentos; `%` e `_` são literais escapados, não curingas do usuário. CPF exato aceita pontuação e mantém zeros. Prontuário exige número completo, sem conversão que perca zeros. Identificadores exatos localizam inativos e origens mescladas; nome usa ativos por padrão e filtro explícito para incluir inativos. Mesclados são resolvidos ao destino com indicação de origem, sem surgir como identidade independente na busca padrão.

Linha: nome completo, nascimento, CPF mascarado (`***.***.***-NN` quando existente), prontuário e situação. Nunca mostrar CPF integral em resultados. Ordenar por nome normalizado e prontuário estável; paginação preserva critério/filtros no servidor. Token expirado exige repetir busca sem expor o termo em URL.

## Contrato de documentos

Envio clínico confirma somente categoria, data, autor e referência neutra; não renderizar preview, filename, texto extraído ou miniatura. Classificação incerta/exame/encaminhamento é sempre clínica. Administrativo válido pode ser baixado por controlador com nome neutro e `Content-Disposition: attachment`; nunca usar caminho fornecido pelo usuário, range parcial ou link público para escapar ao gate de auditoria. Tipos e tamanhos detectados no servidor prevalecem sobre declarações do navegador.

### Nova prova no formulário de revogação

Formulário de revogação oferece selecionar evidência administrativa válida existente ou anexar novo arquivo, exatamente uma opção. O limite da requisição deve comportar os metadados sem reduzir os 10 MiB permitidos por arquivo. Manter dados válidos em 422, orientando reselecionar arquivo quando necessário. O servidor fixa categoria EVIDENCIA_REVOGACAO para a prova nova; o navegador não escolhe uma capacidade de bypass ou envio clínico.

Em INATIVO, somente esse comando pode criar o novo documento vinculado à revogação. O uploader genérico segue bloqueado, inclusive por URL direta. Documento+decisão+eventos+recibo só recebem confirmação juntos; scanner/arquivo/evento/validação indisponíveis não deixam vínculo avulso bem-sucedido. Sucesso mantém paciente inativo e mostra finalidade revogada/referência neutra da prova; replay não duplica nenhum efeito. Cabeçalhos, leitura administrativa e restrições clínicas permanecem os mesmos.

### Mesclagem e conferência de identidade

Na seleção de provas, listar somente anexos ADMINISTRATIVO/VALIDO de cada conjunto canônico correspondente; cada documento é consultado pela rota administrativa autorizada/auditada. Exigir conferência de ambos, justificativa e igualdade de nome completo após apenas caixa/acentos/espaços repetidos e nascimento idêntico. Mostrar pendência objetiva quando falta prova ou igualdade; diferenças aproximadas de AS-003 e dia/mês invertidos não habilitam mesclagem. CPF válido divergente bloqueia mesmo com conferência/provas.

Prévia vincula os dois IDs de prova, versões, escolhas e gerente no servidor. A confirmação não aceita substituir uma prova em campo oculto: alteração exige nova prévia. Invalidação, troca de raiz, alteração de dados ou versão desde a prévia retorna 409 para revisar; prova inicialmente ausente/clínica/alheia ou confirmação sem justificativa retorna validação controlada, sem consolidar. Registro e evento guardam os dois IDs, sem copiar conteúdo/valores. Não habilitar anexação avulsa a inativo para obter nova prova de merge.

### Consulta histórica de convênios

Na ficha de convênios, permitir data de referência passada e mostrar separadamente vigência na data, vencimento naquela data e origem do segmento. Encerrado depois da data consultada não é excluído por seu estado atual. Renovação registra sucessor com novo período/validade; a consulta anterior usa a validade preservada do segmento anterior. Na edição, explicar endsOn como «Encerrado a partir de», primeiro dia fora da vigência; expiresOn permanece inclusivo.

Identificar preferência exibida como **preferência atual**, sem afirmar que era a preferência no passado. Consultar não reabre vínculo, altera preferência ou atendimentos. Sucessor de vínculo preferido exige escolha explícita por sucessor/outro vínculo atual/PARTICULAR. Não criar endpoint de agenda/check-in; a integração futura usa a porta CoverageStatus.

## Estados e acessibilidade

| Estado | Comportamento observável |
| --- | --- |
| Carregando | Indicador textual acessível; prevenção de duplo clique é auxílio, idempotência é do servidor |
| Vazio | Mensagem de ausência correspondente à consulta, sem erro genérico |
| Sucesso | Mensagem consistente após confirmação durável; foco e contexto preservados |
| Erro de campo | Label explícito, `aria-describedby`, `aria-invalid`, resumo de erros com links e campos válidos mantidos |
| Conflito | Mensagem compreensível, sem sobrescrever; ação para revisar versão atual |
| Histórico NÃO_CONFIGURADO | «Histórico ainda não disponível» |
| Histórico VAZIO | «Nenhum atendimento encontrado» somente após fonte integrada responder vazia |
| Histórico FALHA | «Não foi possível consultar o histórico. Tente novamente.» sem afirmar ausência |
| Histórico DISPONIVEL | Data/profissional/tipo em ordem decrescente, preservando origem |

Fragments compartilhados de navegação, alertas, campos e status; headings, landmarks, tabelas com cabeçalhos, idioma declarado, controles nativos, teclado, foco visível/ordenado e contraste WCAG 2.1 AA. Cor nunca é o único sinal. Diálogos de confirmação devolvem foco ao acionador; ao enviar com erro, foco no resumo. Testar mensagens de carregamento/sucesso com região de anúncio sem duplicar leitura.

## Evidências de contrato exigidas

MockMvc verifica métodos, papéis, status, CSRF, versionamento, paginação, filtros, campos permitidos e cabeçalhos. Incluir multipart de revogação de inativo, uploader genérico negado apesar de flag de revogação, duas provas obrigatórias na prévia/merge, invalidação entre prévia e confirmação e consulta histórica após encerramento/renovação. Verificar todas as combinações da matriz também por URL direta. E2E confirma mensagens/estados, ausência de dados clínicos, nenhum sucesso sem auditoria e comportamento acessível no navegador. [Quickstart](../quickstart.md) define os roteiros de aceite; [plan.md](../plan.md) separa nomes/tags de contratos, jornadas e desempenho.
