# Feature Specification: Cadastro e Gestão de Pacientes

**Feature Branch**: `dev` — implementação na branch existente; nenhum hook configurado.

**Feature Directory**: `specs/001-gestao-pacientes`

**Created**: 2026-10-23

**Status**: Implementado e verificado como MVP demonstrativo; escopo simplificado pelo usuário em 2026-10-24. Evidências em docs/validation/001-gestao-pacientes/verification.md.

**Input**: Cadastro administrativo único para recepção, com identificação, contatos, responsáveis, convênios, busca, prevenção e mesclagem de duplicidades, documentos, resumo de atendimentos, consentimentos, inativação, reativação e auditoria. Proteção de dados pessoais e sensíveis e acesso por perfil.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Cadastrar uma identidade confiável (Priority: P1)

Como secretária, quero cadastrar pacientes, inclusive menores e estrangeiros sem CPF, sem fragmentar sua identidade.

**Why this priority**: Identificação correta sustenta todos os processos administrativos.

**Independent Test**: Cadastrar adulto, menor e estrangeiro fictícios, sem agenda ou prontuário clínico.

**Acceptance Scenarios**:

1. **Given** dados válidos e secretária autenticada, **When** cadastra, **Then** gera prontuário único e imutável e audita a criação.
2. **Given** CPF inválido ou pertencente a outra identidade ativa, inativa ou mesclada, **When** cria ou edita paciente, **Then** bloqueia, inclusive em tentativas simultâneas.
3. **Given** menor sem CPF, **When** informa documento alternativo e responsável legal, **Then** permite cadastrar; sem um deles, bloqueia.
4. **Given** estrangeiro sem CPF, **When** apresenta documento alternativo completo, **Then** permite cadastrar; adulto brasileiro sem CPF não se enquadra nessa exceção.
5. **Given** nomes e nascimentos semelhantes, **When** cria ou altera identificação, **Then** alerta e exige revisão; documentos distintos permitem prosseguir com justificativa. Telefone familiar não bloqueia cadastro.
6. **Given** campos obrigatórios ausentes, nascimento futuro ou contato preenchido inválido, **When** tenta salvar, **Then** identifica erros nos campos e preserva os demais valores válidos.
7. **Given** identificação válida, **When** tenta salvar sem telefone ou sem endereço, **Then** bloqueia e indica os dados faltantes; com ambos válidos e demais obrigatórios atendidos, permite salvar sem e-mail.

### User Story 2 - Buscar e consultar a ficha (Priority: P1)

Como secretária ou médico, quero localizar o paciente correto e consultar dados administrativos e resumo de atendimentos.

**Why this priority**: Consulta rápida e identificação inequívoca são essenciais à recepção.

**Independent Test**: Buscar em base fictícia com homônimos e inativos; verificar a primeira entrega sem fonte de atendimentos e, com fonte controlada de teste, os estados com registros, vazio e falha.

**Acceptance Scenarios**:

1. **Given** variações de caixa e acentos, **When** busca nome parcial, **Then** encontra correspondências distinguíveis por nome, nascimento, CPF mascarado, prontuário e situação.
2. **Given** CPF com ou sem pontuação ou prontuário completo, **When** busca, **Then** encontra a identidade; número de origem mesclada conduz ao destino com indicação da mesclagem.
3. **Given** nenhum ou muitos resultados, **When** busca, **Then** mostra estado vazio ou páginas de até 20 pacientes com refinamento e navegação preservando filtros.
4. **Given** atendimentos disponíveis, **When** consulta histórico, **Then** vê somente data, profissional e tipo, do mais recente ao mais antigo, sem diagnóstico, evolução ou conteúdo clínico.
5. **Given** fonte de atendimentos disponível que retorna nenhum registro ou falha ao consultar uma fonte já integrada, **When** abre o histórico, **Then** distingue ausência de atendimentos de falha de consulta.
6. **Given** médico autenticado, **When** lê ou tenta alterar a ficha, **Then** permite leitura administrativa auditada e nega qualquer escrita.
7. **Given** primeira entrega sem módulo que forneça atendimentos, **When** abre o histórico, **Then** mostra «Histórico ainda não disponível», sem afirmar que o paciente não tem atendimentos, sem dados fictícios em produção e sem permitir lançamento ou correção manual de resumos neste módulo.

### User Story 3 - Atualizar, inativar e reativar (Priority: P1)

Como secretária, quero corrigir dados, inativar pacientes e reativar seus cadastros quando retornarem, preservando seus vínculos.

**Why this priority**: Mantém a confiabilidade sem apagar a rastreabilidade.

**Independent Test**: Editar, inativar e reativar pacientes fictícios com vínculos existentes, verificando preservação do prontuário e auditoria de cada transição.

**Acceptance Scenarios**:

1. **Given** contato desatualizado, **When** o corrige, **Then** atualiza o contato, mantém o prontuário e audita os campos alterados.
2. **Given** edição concorrente, **When** salva uma versão ultrapassada, **Then** exige revisão dos dados atuais sem sobrescrever silenciosamente.
3. **Given** paciente ativo, **When** confirma inativação com motivo, **Then** preserva dados e vínculos, remove da busca padrão e permite localizar por filtro explícito ou identificador exato.
4. **Given** inativo, **When** consulta, **Then** mostra a situação e mantém CPF reservado; não oferece exclusão definitiva.
5. **Given** paciente inativo que retorna, **When** secretária ou gerente confirma reativação com motivo, **Then** o mesmo cadastro volta a ativo e à busca padrão, preservando prontuário, documentos e histórico, com auditoria de autor, instante e motivo; sem motivo, bloqueia.
6. **Given** origem de cadastro mesclado, **When** alguém tenta reativá-la, **Then** bloqueia e indica o destino consolidado, sem recriar identidade independente.
7. **Given** CPF ou documento alternativo registrado incorretamente em um cadastro ativo, **When** secretária ou gerente corrige mediante documento comprobatório vinculado e motivo obrigatório, **Then** valida a nova identificação, preserva o mesmo prontuário e vínculos e audita autor, instante, campos, motivo e referência da comprovação, sem copiar dados do documento para o evento; sem comprovação ou motivo, bloqueia.

### User Story 4 - Manter responsáveis, emergência e convênios (Priority: P1)

Como secretária, quero manter responsáveis e coberturas atuais e anteriores para apoiar a recepção.

**Why this priority**: Evita lacunas de responsabilidade e erros administrativos.

**Independent Test**: Registrar responsável, emergência, dois convênios e troca de cobertura, sem faturamento.

**Acceptance Scenarios**:

1. **Given** menor, **When** informa responsável, **Then** exige nome, vínculo, documento e telefone; contato de emergência não o substitui automaticamente.
2. **Given** contato de emergência informado, **When** salva, **Then** exige nome, parentesco ou vínculo e telefone em conjunto, permitindo contato compartilhado.
3. **Given** dois convênios completos, **When** registra preferência, **Then** preserva ambos e permite preferir um deles ou atendimento particular.
4. **Given** troca ou encerramento de cobertura, **When** atualiza vínculos, **Then** preserva o histórico sem usá-lo como cobertura atual.
5. **Given** validade anterior à data consultada, **When** consulta situação, **Then** alerta vencimento; no próprio dia da validade, considera válida. A situação fica disponível para futura agenda e check-in.
6. **Given** convênio vigente em uma data passada e encerrado depois dela, **When** consulta a situação naquela data, **Then** reconstrói a vigência e a validade conforme o histórico e a data consultada, sem descartá-lo por estar encerrado hoje, sem reabrir o vínculo e sem alterar a preferência atual.

### User Story 5 - Registrar consentimentos e revogações (Priority: P1)

Como secretária, quero registrar manifestações por finalidade e preservar evidências de mudanças.

**Why this priority**: Evita autorizações presumidas, genéricas ou desatualizadas.

**Independent Test**: Registrar aceite, recusa e revogação em finalidades distintas, sem acesso do paciente ao sistema.

**Acceptance Scenarios**:

1. **Given** manifestação documentada, **When** registra, **Then** vincula finalidade, versão do termo, decisão, datas, manifestante, responsável legal quando aplicável, operador e evidência.
2. **Given** recusa de imagem, **When** salva, **Then** mantém a recusa sem bloquear cadastro ou modificar outras finalidades.
3. **Given** aceite anterior, **When** registra revogação comunicada pelo titular ou responsável, **Then** mostra revogado imediatamente sem apagar o aceite histórico.
4. **Given** mudança de termo, responsável ou cadastro, **When** atualiza, **Then** não presume novo aceite; nova autorização exige manifestação documentada.
5. **Given** paciente inativo e nova evidência administrativa de revogação, **When** secretária ou gerente registra a revogação, **Then** permite anexar a evidência exclusivamente nesse fluxo e vinculá-la à manifestação, mantendo o paciente inativo e bloqueadas a anexação avulsa e as demais alterações cadastrais comuns; aplicar as mesmas regras de segurança, acesso e auditoria dos documentos administrativos.

### User Story 6 - Anexar documentos com acesso restrito (Priority: P2)

Como secretária, quero associar documentos administrativos e enviar arquivos clínicos sem poder consultar posteriormente o conteúdo clínico.

**Why this priority**: Centraliza vínculos preservando a fronteira administrativa.

**Independent Test**: Enviar identidade, exame e encaminhamento fictícios e testar acesso para todos os perfis.

**Acceptance Scenarios**:

1. **Given** documento administrativo válido, **When** anexa, **Then** associa categoria, data, autor e paciente e permite leitura administrativa autorizada.
2. **Given** exame ou encaminhamento, **When** envia, **Then** confirma sem prévia; nenhum perfil deste módulo pode ler, baixar ou extrair conteúdo. Exibe só categoria, data, autor e referência neutra, sem nome original revelador.
3. **Given** referência direta a arquivo clínico, **When** secretária, gerente ou médico tenta obtê-lo, **Then** nega e audita; perfil médico não concede acesso clínico aqui.
4. **Given** arquivo vazio, inseguro, proibido ou acima do limite, **When** envia, **Then** rejeita sem disponibilizar arquivo ou vínculo parcial.
5. **Given** vínculo errado, **When** gerente registra motivo, **Then** pode invalidá-lo preservando original e auditoria; correção exige novo envio, sem abrir conteúdo clínico.

### User Story 7 - Mesclar duplicidades (Priority: P2)

Como gerente, quero consolidar cadastros da mesma pessoa após revisar divergências, preservando suas referências.

**Why this priority**: Corrige fragmentação sem destruir evidências ou confundir pessoas distintas.

**Independent Test**: Consolidar dois cadastros fictícios com divergências, documentos e históricos; consultar ambos os números originais.

**Acceptance Scenarios**:

1. **Given** evidência administrativa válida vinculada a cada cadastro e nome completo e nascimento coincidentes após eventuais correções autorizadas, **When** gerente confere as evidências, confirma justificadamente a identidade, escolhe destino, resolve conflitos administrativos e confirma a prévia, **Then** consolida vínculos, preserva as referências das duas evidências e marca origem como mesclada, sem exclusão.
2. **Given** prontuários diferentes, **When** mescla, **Then** não altera nem reutiliza números; o da origem continua resolvendo para o destino com proveniência.
3. **Given** CPFs válidos divergentes, ausência de evidência administrativa válida de qualquer dos cadastros, nomes completos ou nascimentos divergentes após normalização permitida, ou falta de confirmação justificada do gerente, **When** tenta confirmar, **Then** considera a identidade não comprovada e bloqueia até sanar as pendências ou realizar correção documental autorizada, sem exceção gerencial; semelhança aproximada de nomes não satisfaz a coincidência exigida.
4. **Given** históricos e consentimentos conflitantes, **When** mescla, **Then** mantém registros e origens, não deduplica eventos clínicos pelo conteúdo nem restaura consentimento revogado.
5. **Given** falha, alteração concorrente, auto-mesclagem ou origem já mesclada, **When** confirma, **Then** impede consolidação parcial e orienta revisão.
6. **Given** secretária ou médico, **When** tenta mesclar, **Then** nega e audita.

### User Story 8 - Consultar auditoria (Priority: P2)

Como gerente, quero saber quem criou, alterou e consultou cadastros sem expor conteúdo sensível nos eventos.

**Why this priority**: Permite investigar uso indevido e demonstrar rastreabilidade.

**Independent Test**: Executar operações fictícias e revisar eventos e permissões como gerente.

**Acceptance Scenarios**:

1. **Given** operações realizadas, **When** filtra por período, usuário, paciente ou operação, **Then** encontra autor, instante, ação, objeto, resultado e campos alterados quando aplicável.
2. **Given** busca vazia ou tentativa negada, **When** ocorre, **Then** audita o resultado sem texto bruto de pesquisa ou conteúdo sensível.
3. **Given** evento existente, **When** qualquer perfil tenta editá-lo ou apagá-lo, **Then** impede; secretária e médico também não podem consultá-lo.
4. **Given** auditoria indisponível, **When** tenta ler ou modificar dados, **Then** não libera leitura nem confirma alteração sem evento e informa indisponibilidade; nova tentativa não duplica operação.

### Edge Cases

- CPF com pontuação ou zeros iniciais: comparar dígitos sem perder zeros e rejeitar sequência inválida.
- Paciente completa 18 anos: usar data local da clínica, preservar responsáveis históricos e não exigir novo responsável para adulto.
- Menor ou estrangeiro obtém CPF: acrescentar CPF único à mesma identidade, preservando documento anterior e prontuário.
- Gêmeos, homônimos e familiares: sem documento idêntico, suspeita não comprova duplicidade.
- Telefone ou endereço ausente: impedir salvar e orientar o preenchimento com dados reais, preservando os demais valores; e-mail ausente não bloqueia nem torna o cadastro incompleto.
- CPF em inativo ou mesclado: impedir novo cadastro; permitir reativação autorizada do inativo com motivo e auditoria, mas nunca reativar origem mesclada como identidade independente.
- Convênio incompleto: exigir validade; múltiplos vínculos não justificam selecionar cobertura vencida automaticamente.
- Convênio atualmente encerrado: na consulta a uma data passada, reconstruir vigência e validade naquela data pelos períodos históricos; encerramento posterior à data consultada não elimina a cobertura histórica nem a torna cobertura atual.
- Busca vazia ou nome com menos de dois caracteres: pedir critério suficiente sem listar toda a base.
- Reenvio ou gravação simultânea: impedir números repetidos, duplicação pela mesma submissão e alterações parciais.
- Mesclagem com consentimentos divergentes: manter finalidade sem autorização até nova manifestação; destino não anula revogação.
- Mesclagem sem CPF em um ou ambos os cadastros: exigir evidência administrativa válida vinculada a cada um, nome completo e nascimento coincidentes após eventuais correções autorizadas e confirmação justificada do gerente. Para o nome, desconsiderar somente caixa, acentos e espaços repetidos; diferenças aproximadas previstas em AS-003 não comprovam identidade. CPFs válidos divergentes continuam bloqueando a mesclagem.
- Revogação de paciente inativo com nova evidência: permitir anexá-la exclusivamente no fluxo de revogação, sem reativar o cadastro nem liberar anexação avulsa ou outras alterações cadastrais comuns.
- Suspeita de anexo clínico classificado como administrativo: permitir invalidar vínculo e restringir exposição, com auditoria.
- Fonte de atendimentos ainda inexistente: mostrar «Histórico ainda não disponível», sem lançamento manual; distinguir essa condição de fonte consultada sem registros e de falha em fonte já integrada.
- Fonte de atendimentos ou arquivos temporariamente indisponível: distinguir falha de ausência de registros.

## Requirements *(mandatory)*

### Functional Requirements

#### Identificação e cadastro

- **FR-001**: Secretária e gerente DEVEM poder criar e manter nome completo, nascimento, sexo informado, CPF ou documento alternativo, telefone, e-mail e endereço. Endereço deve comportar país e formatos estrangeiros, sem exigir CEP brasileiro de estrangeiros.
- **FR-002**: Nome, nascimento válido, identificação, sexo informado, incluindo opção «não informado», telefone válido e endereço preenchido DEVEM ser obrigatórios para salvar ou atualizar um cadastro. Telefone ou endereço ausente deve bloquear a gravação, com erro associado ao campo e preservação dos demais valores válidos. E-mail é opcional e deve ter formato validado quando preenchido; sua ausência não bloqueia nem torna o cadastro incompleto. Telefone familiar ou do responsável legal pode ser usado quando for o contato do paciente, sem exigir número exclusivo. Não deve haver limite artificial de idade que impeça paciente idoso.
- **FR-003**: CPF DEVE ter formato e dígitos verificadores válidos e ser único entre identidades, inclusive inativas ou mescladas, na criação, edição, inclusão posterior e concorrência, sem exceção gerencial. Essa validação não implica consulta a órgão externo.
- **FR-004**: Somente menor de 18 anos ou estrangeiro sem CPF DEVE poder substituí-lo por documento alternativo com motivo, tipo, número, país e emissor. A combinação documental deve ser única entre identidades; documento do responsável não substitui o do menor.
- **FR-005**: Cada paciente DEVE receber automaticamente prontuário único, imutável e nunca reutilizado. Edição e mesclagem não podem renumerá-lo.
- **FR-006**: Menor DEVE ter ao menos um responsável legal atual, com nome, vínculo, documento e telefone. Mudanças devem preservar proveniência e período; contato de emergência não confere responsabilidade legal automaticamente.
- **FR-007**: Deve ser permitido um contato de emergência atual por paciente, com nome, vínculo e telefone exigidos em conjunto quando informado. Contatos e endereços compartilhados NÃO DEVEM ser identificadores exclusivos.
- **FR-008**: Criação ou alteração de identificação DEVE alertar suspeita conforme AS-003, exibindo apenas dados administrativos autorizados. Suspeita sem documento idêntico permite prosseguir com justificativa auditada; documento idêntico bloqueia.

#### Busca, consulta e manutenção

- **FR-009**: Buscar por nome parcial sem distinguir caixa e acentos, CPF exato com ou sem pontuação e prontuário exato, preservando zeros iniciais. Exigir critério não vazio e pelo menos dois caracteres para nome.
- **FR-010**: Resultados DEVEM mostrar nome, nascimento, CPF mascarado, prontuário e situação, com até 20 registros por página, refinamento e navegação preservando filtros. Ordenar por nome e depois prontuário, de forma estável sem alteração dos dados, e distinguir carregando, vazio e falha.
- **FR-011**: Nome DEVE buscar ativos por padrão, permitindo incluir inativos. CPF e prontuário exatos também devem localizar inativos e origens mescladas, indicando situação e destino.
- **FR-012**: Ficha e resumos DEVEM apresentar apenas dados administrativos e pendências, sem diagnóstico, evolução, prescrições ou conteúdo clínico em campos ou mensagens.
- **FR-013**: Histórico DEVE ser somente leitura. Na primeira entrega, enquanto não existir módulo fornecedor de atendimentos, mostrar «Histórico ainda não disponível», sem dados fictícios em produção nem lançamento ou correção manual de resumos. Quando uma fonte estiver disponível, apresentar somente data, profissional e tipo, em ordem decrescente de data. Distinguir fonte ainda inexistente, consulta válida sem registros e falha de consulta de fonte já integrada; criar ou corrigir atendimentos está fora do escopo.
- **FR-014**: Edição DEVE validar os invariantes do cadastro, preservar prontuário, auditar campos alterados e impedir sobrescrita silenciosa de versão desatualizada, exigindo revisão da versão atual. Secretária e gerente podem corrigir CPF ou documento alternativo já registrado em cadastro ativo somente com documento comprobatório administrativo vinculado e motivo obrigatório auditado. Validar a nova identificação e sua unicidade, preservando a mesma identidade, prontuário e vínculos; não transferir históricos entre pessoas por correção documental. Ausência de comprovação ou motivo deve bloquear a correção.
- **FR-015**: Secretária e gerente DEVEM poder inativar e reativar com confirmação e motivo obrigatório, preservando o mesmo prontuário, dados e vínculos e auditando autor, instante, motivo e transição de situação. Reativação devolve o cadastro à situação ativa e à busca padrão. Bloquear edição cadastral comum enquanto inativo, mas permitir revogação de consentimento, o envio de evidência administrativa exclusivamente nesse fluxo e invalidação documental autorizada. Essa exceção não reativa o paciente nem habilita anexação avulsa ou outras alterações cadastrais comuns. Origem mesclada não pode ser reativada como identidade independente. Não disponibilizar exclusão definitiva.

#### Convênios

- **FR-016**: Registrar múltiplos vínculos com operadora, plano, carteirinha, validade, situação e período de vigência, preservando substituições e encerramentos. A carteirinha não deve ser exclusiva globalmente entre pacientes ou operadoras.
- **FR-017**: Manter preferência explícita por particular ou um vínculo atual, permitindo mudar sem apagar histórico ou alterar atendimentos anteriores. Sem convênio atual, a preferência deve ser particular.
- **FR-018**: Carteirinha vencida DEVE gerar alerta na ficha e na consulta de situação para data de referência, inclusive se for a preferência atual. Para uma data passada, reconstruir a vigência e a validade naquela data a partir dos períodos históricos e da validade registrada, incluindo vínculos encerrados posteriormente; a situação atual de encerrado não pode, isoladamente, excluir uma cobertura que estava vigente na data consultada. A consulta histórica não reabre o vínculo nem altera a preferência atual ou atendimentos anteriores. Futura agenda e check-in deverão consumir essa situação; suas telas, regras de bloqueio e implantação não integram esta entrega.

#### Consentimentos

- **FR-019**: Registrar decisões separadas por finalidade de proteção de dados, termos de atendimento e imagem, sem aceite pré-selecionado. Incluir finalidade, versão do termo, concedido/recusado/revogado, datas da manifestação e registro, manifestante, responsável legal quando aplicável, operador e referência de evidência administrativa.
- **FR-020**: Secretária e gerente DEVEM registrar revogação comunicada pelo titular ou responsável, com efeito imediato no estado da finalidade e preservação do histórico. Para paciente inativo, permitir anexar nova evidência administrativa exclusivamente no fluxo de revogação e vinculá-la à manifestação, sem exigir reativação; aplicar as mesmas regras de segurança, acesso e auditoria dos demais documentos administrativos. Recusa de imagem não deve impedir cadastro; não presumir que todo tratamento depende de consentimento.
- **FR-021**: Mudança de termo, responsável, cadastro ou mesclagem NÃO DEVE gerar aceite. Nova autorização após recusa ou revogação exige manifestação documentada; decisões conflitantes mantêm a finalidade sem autorização até resolução explícita.

#### Documentos

- **FR-022**: Secretária e gerente DEVEM poder anexar identidade, evidências administrativas, exames e encaminhamentos com paciente, categoria obrigatória, data e autor. Em paciente inativo, o envio de novos documentos fica restrito à evidência administrativa vinculada à manifestação no fluxo de revogação de consentimento; bloquear anexação avulsa. Exames, encaminhamentos e categoria incerta devem receber classificação clínica restrita.
- **FR-023**: Anexos administrativos válidos DEVEM ser legíveis somente aos perfis com leitura administrativa. Anexos clínicos devem permitir envio sem prévia e impedir leitura, download, prévia e extração de conteúdo por TODOS os perfis deste módulo, inclusive médico e gerente, também por referência direta. Mostrar somente categoria, data, autor e referência neutra, sem nome original revelador.
- **FR-024**: Aceitar apenas PDF, JPEG e PNG, não vazios, até 10 MiB por arquivo, com tipo e conteúdo compatíveis e verificação de segurança antes de confirmar o vínculo. Arquivo recusado ou envio incompleto não deve gerar vínculo bem-sucedido nem acesso ao arquivo; arquivo clínico aprovado continua sem leitura neste módulo.
- **FR-025**: Apenas gerente DEVE invalidar vínculo documental com motivo, preservando original, classificação e auditoria. Não oferecer exclusão, redução de restrição por reclassificação ou transferência silenciosa entre pacientes; correção exige invalidar e reenviar. Documento invalidado deixa de estar disponível na consulta comum.

#### Mesclagem

- **FR-026**: Apenas gerente DEVE mesclar dois cadastros após selecionar destino, revisar dados e vínculos, decidir conflitos administrativos, confirmar identidade e informar motivo. Para comprovar a identidade, exigir evidência administrativa válida vinculada a cada cadastro, nome completo e nascimento coincidentes após eventuais correções autorizadas e conferência das evidências com confirmação justificada do gerente. Para comparar nomes, desconsiderar somente caixa, acentos e espaços repetidos; semelhança aproximada não basta. Preservar as referências internas das evidências de origem e destino no registro de mesclagem. Exigir prévia e confirmação explícita.
- **FR-027**: Preservar documentos, resumos, responsáveis e convênios históricos, consentimentos e auditoria com origens e restrições. NÃO deduplicar atendimentos pelo conteúdo nem conferir permissão clínica adicional.
- **FR-028**: Origem DEVE permanecer marcada como mesclada, sem edição comum, com prontuário imutável resolvendo para destino. Identificadores vigentes na origem imediatamente antes da mesclagem devem continuar localizáveis e reservados à identidade consolidada, impedindo novos cadastros com eles. Correção documental de identidade anterior à mesclagem deve preservar auditoria e não manter uma associação comprovadamente errada como identificação atual.
- **FR-029**: Mesclagem DEVE ser integral e sem associações parciais; detectar versões alteradas e bloquear auto-mesclagem, ciclos, origem já mesclada e CPFs válidos divergentes. Considerar a identidade não comprovada e bloquear quando faltar evidência administrativa válida vinculada a qualquer dos cadastros, coincidência de nome completo e nascimento conforme FR-026 ou confirmação justificada do gerente após conferência das evidências. A confirmação gerencial isolada não dispensa esses critérios. Exigir resolução documentada da identidade e dos conflitos administrativos antes de nova prévia e confirmação.

#### Permissões e auditoria

- **FR-030**: Toda operação DEVE exigir autenticação e autorização conforme a matriz; negar ações não autorizadas sem expor dados. O paciente não terá acesso direto ao módulo.
- **FR-031**: Auditar criação, alteração, inativação, reativação, consentimentos, anexação e invalidação, mesclagem, busca, leitura de ficha, resumo e documento e tentativas negadas. Incluir consultas sem resultado e à própria auditoria, sem geração recursiva de eventos.
- **FR-032**: Evento DEVE conter identificador, usuário, perfil atuante, instante, operação, referência interna do objeto/paciente quando existente e resultado. Alterações indicam nomes dos campos; mesclagens indicam origem, destino, motivo e referências internas das evidências administrativas de ambos os cadastros, sem copiar valores ou conteúdo documental para o evento. Correções de CPF ou documento alternativo incluem motivo e referência interna do documento comprobatório, sem copiar seus valores ou conteúdo para o evento. Não registrar CPF integral, contatos, conteúdo documental, texto bruto de busca, diagnóstico, senha ou token. Orientar motivos livres a não incluir dados clínicos ou segredos.
- **FR-033**: Apenas gerente DEVE consultar auditoria com filtros combináveis por período, usuário, paciente e operação e páginas de até 20 eventos. Nenhum perfil pode alterar ou excluir eventos. Consulta por destino consolidado deve incluir origens sem reescrever autoria histórica.
- **FR-034**: Sem auditoria obrigatória disponível, NÃO confirmar alteração nem liberar leitura protegida. Informar indisponibilidade sem detalhes internos; nova tentativa após falha ou confirmação incerta não deve duplicar operação.

### Access Matrix

| Capacidade | Secretária/recepção | Gerente | Médico | Paciente |
| --- | --- | --- | --- | --- |
| Buscar e ler ficha, resumo e anexos administrativos válidos | Sim | Sim | Sim | Sem acesso |
| Criar, editar ativos, inativar, reativar e manter responsáveis e convênios | Sim | Sim | Não | Sem acesso |
| Corrigir CPF ou documento alternativo já registrado, com comprovação e motivo | Sim | Sim | Não | Sem acesso |
| Registrar manifestação documentada e revogação | Sim | Sim | Não | Fornece manifestação fora do sistema |
| Enviar documentos administrativos ou clínicos | Sim | Sim | Não | Sem acesso |
| Ler conteúdo de anexo clínico neste módulo | Não | Não | Não | Sem acesso |
| Invalidar vínculo documental | Não | Sim | Não | Sem acesso |
| Mesclar ou consultar auditoria | Não | Sim | Não | Sem acesso |
| Excluir definitivamente ou alterar auditoria | Não | Não | Não | Sem acesso |

### Quality & Privacy Requirements

- **QR-001**: Conferir a busca e a exibição em dezenas de registros fictícios, conforme SC-001 e constituição 2.0.0. A demonstração não exige carga automatizada, SLO ou ambiente separado.
- **QR-002**: Jornadas DEVEM atender aos critérios aplicáveis de WCAG 2.1 A e AA, com teclado, leitor de tela, foco visível, nomes acessíveis, contraste e erros associados aos campos, sem depender só de cor nem apagar entradas válidas.
- **QR-003**: Proteger dados e arquivos contra acesso indevido no armazenamento, envio e consulta. Segredos e dados reais de pacientes não podem compor exemplos, demonstrações, registros técnicos ou materiais públicos.
- **QR-004**: Inativação não equivale à eliminação nem define retenção ilimitada. Antes de dados reais, a clínica deve aprovar finalidades, fundamentos de tratamento, termos, evidências, retenção e procedimento externo de solicitações dos titulares. Não apagar automaticamente dados por revogação; eventual eliminação legalmente cabível exige procedimento específico, fora da operação cadastral deste módulo.

### Key Entities *(include if feature involves data)*

- **Paciente**: Identidade administrativa, prontuário imutável, dados pessoais, telefone e endereço obrigatórios, e-mail opcional, situação e preferência; mantém referências de origens mescladas.
- **Documento de identificação**: CPF ou combinação alternativa de tipo, número, país e emissor, motivo e proveniência, inclusive após obtenção posterior de CPF.
- **Responsável legal**: Nome, documento, contato, vínculo e período; não precisa ser paciente.
- **Contato de emergência**: Nome, vínculo e telefone; função distinta do responsável legal.
- **Vínculo de convênio**: Operadora, plano, carteirinha, validade, vigência e situação, inclusive histórico encerrado.
- **Documento anexado**: Arquivo, classificação, categoria, paciente, autor, data e eventual invalidação, preservando origem.
- **Decisão de consentimento**: Finalidade, versão, decisão, datas, manifestante, operador e evidência, com histórico preservado.
- **Resumo de atendimento**: Referência externa ao módulo com data, profissional, tipo e origem, sem conteúdo clínico.
- **Registro de mesclagem**: Origem, destino, referências das evidências administrativas dos dois cadastros, resolução administrativa, motivo, gerente e instante, sem renumerar pacientes.
- **Evento de auditoria**: Evidência imutável da ação, objeto, autoria, instante, resultado e metadados minimizados.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Demonstrar busca por nome, CPF e prontuário em dezenas de pacientes fictícios, com resultado ou estado vazio compreensível e paginação de até 20. Conferir a resposta por navegação manual; não exigir teste de carga ou SLO para esta apresentação.
- **SC-002**: Bloquear 100% das tentativas com CPF inválido ou de outra identidade, inclusive simultâneas e envolvendo inativos ou mesclados; nenhum prontuário é duplicado ou reutilizado. Bloquear também 100% das correções de CPF ou documento alternativo já registrado sem documento comprobatório ou motivo; correções autorizadas por secretária ou gerente devem preservar o prontuário e produzir auditoria com referência da comprovação.
- **SC-003**: 100% das operações de FR-031 têm auditoria com quem, quando, ação e resultado; nenhuma alteração é confirmada nem leitura protegida liberada sem o evento exigido.
- **SC-004**: O roteiro de apresentação permite cadastrar paciente fictício com telefone, endereço, identidade e manifestação já disponíveis. E-mail continua opcional. Não exigir pesquisa com secretárias ou meta cronometrada.
- **SC-005**: Em todos os cenários da matriz, bloquear ações proibidas sem exposição; nenhum perfil consegue ler ou baixar anexos clínicos neste módulo.
- **SC-006**: Nas mesclagens de aceite, 100% dos documentos, resumos e decisões permanecem rastreáveis, ambos os números originais levam ao destino e nenhuma revogação é anulada. Bloquear 100% das tentativas sem evidência administrativa válida de qualquer dos cadastros, sem coincidência de nome completo e nascimento conforme FR-026 ou sem confirmação justificada do gerente; CPFs válidos divergentes continuam bloqueando. Mesclagens confirmadas preservam as referências das duas evidências no registro e na auditoria, sem copiar conteúdo documental. Falhas não deixam vínculos parcialmente movidos.
- **SC-007**: Os roteiros de menor sem CPF, estrangeiro, múltiplos convênios, troca, vencimento, contato compartilhado, revogação, busca vazia, conflito de edição e retorno com reativação alcançam os resultados previstos sem cadastro duplicado ou exclusão definitiva. Incluir consulta de convênio em data passada anterior ao seu encerramento posterior, apresentando a vigência e a validade daquela data sem alterar vínculo, preferência atual ou atendimentos anteriores. Incluir revogação de paciente inativo com nova evidência administrativa anexada exclusivamente nesse fluxo, mantendo o paciente inativo e bloqueada a anexação avulsa. Toda reativação autorizada preserva prontuário e vínculos, registra motivo e auditoria e devolve o paciente à busca padrão; origens mescladas permanecem sem reativação independente. Sem fonte de atendimentos na primeira entrega, todas as consultas ao histórico mostram «Histórico ainda não disponível», sem dados fictícios em produção ou lançamento manual; os estados de fonte vazia e falha devem ser verificáveis separadamente com fonte controlada de teste.
- **SC-008**: Conferir labels, semântica, mensagens textuais, foco visível e navegação por teclado nas telas essenciais, sem exigir certificação ou laudo formal de acessibilidade.
- **SC-009**: Bloquear 100% das tentativas de criação ou edição cadastral sem telefone ou sem endereço, identificando os campos faltantes e preservando os demais valores; permitir 100% dos cenários válidos sem e-mail quando todos os obrigatórios estiverem atendidos.

## Assumptions

- **AS-001 — Escopo**: Uma clínica, com cadastro compartilhado pelos perfis autorizados. Multiclínica, importação em lote, autoatendimento e desfazer mesclagem ficam fora desta entrega. Reativação por secretária ou gerente, com motivo e auditoria, integra esta entrega. Número de prontuário é identificador administrativo e não concede acesso clínico.
- **AS-002 — Completude**: O mínimo obrigatório é FR-002 e FR-006, incluindo telefone e endereço. E-mail é opcional; sua ausência não caracteriza pendência de completude. Emergência e convênio são informados quando aplicáveis; sexo pode ser «não informado». Não usar dados fictícios para contornar campos obrigatórios.
- **AS-003 — Suspeita**: Alertar para nascimento idêntico e nomes iguais após desconsiderar caixa, acentos e espaços repetidos, ou nomes que diferem por uma inserção, remoção, substituição de letra ou troca de duas letras adjacentes. Também alertar para nome normalizado idêntico e nascimento com dia/mês invertidos quando ambas as datas forem válidas. Outras aproximações ficam fora desta entrega; documento idêntico sempre bloqueia. Validar com exemplos positivos e negativos, incluindo gêmeos e homônimos.
- **AS-004 — Perfis**: Gerente acumula permissões administrativas da secretária. Identidade de usuário e perfis são dependências obrigatórias antes da liberação; criação de contas e administração de perfis não integram este módulo.
- **AS-005 — Dependências**: A primeira entrega não depende de implementar agenda ou atendimento para liberar o cadastro. Enquanto não existir módulo fornecedor, o histórico mostra «Histórico ainda não disponível», sem lançamento manual ou dados fictícios em produção. Dados fictícios controlados ficam restritos à verificação dos futuros estados de consulta e preservação de associações; a consulta real dependerá da fonte de resumos administrativos quando existir. Agenda e agendamento, check-in, faturamento/TISS, financeiro, prontuário clínico, telemedicina e portal permanecem fora; esta entrega fornece situação de convênio e preserva associações e resumos disponíveis.
- **AS-006 — Arquivos e datas**: PDF/JPEG/PNG e 10 MiB são limites iniciais de produto. Nascimento, maioridade e validade seguem data local da clínica. O envio clínico não habilita inspeção pela recepção; leitura futura exige escopo e autorização próprios.
- **AS-007 — Verificação**: MVP para apresentação, com dezenas de pacientes fictícios, poucos usuários e poucos testes essenciais. Seguir a constituição 2.0.0, sem cobertura percentual ou carga obrigatória.
- **AS-008 — Governança**: A instrução do usuário autoriza implementar as oito histórias com estrutura simplificada, sem aprovações por papel. Políticas e termos para dados reais ficam fora da apresentação; as restrições clínicas continuam valendo na auditoria, invalidação e mesclagem.

## Clarifications

### Session 2026-10-23

- **Q1**: Como anexar exames e encaminhamentos sem conceder leitura clínica à recepção? **Resposta do usuário**: «A — Enviar anexos clínicos sem permitir leitura posterior». A recepção pode enviar; todos os perfis ficam restritos a dados administrativos neste módulo. Leitura clínica fica para módulo futuro.
- Q: Quando um paciente inativo voltar à clínica, quem poderá reativar o cadastro existente? → A: Secretária e gerente podem reativar, com motivo obrigatório e auditoria, preservando prontuário e vínculos.
- Q: Quem poderá corrigir um CPF ou documento alternativo já registrado no cadastro do paciente? → A: Secretária e gerente, mediante documento comprobatório e motivo registrado na auditoria.
- Q: Enquanto os módulos que fornecem os atendimentos ainda não existirem, como o histórico deverá funcionar nesta primeira entrega? → A: Mostrar «Histórico ainda não disponível», sem permitir lançamento manual neste módulo.
- Q: Quais dados de contato devem ser obrigatórios para salvar o cadastro de um paciente? → A: Exigir telefone e endereço; e-mail opcional.

### Session 2026-10-24

- Q: Como deve ser registrada uma nova evidência de revogação de consentimento para um paciente inativo? → A: Permitir anexar evidência administrativa somente no fluxo de revogação, mantendo o paciente inativo e as demais restrições.
- Q: Que comprovação deve ser exigida para confirmar que dois cadastros pertencem à mesma pessoa antes de mesclá-los? → A: Exigir evidência administrativa de cada cadastro, nome e nascimento coincidentes após correções e confirmação justificada do gerente.
- Q: Como a consulta de convênios deve tratar uma data passada? → A: Reconstruir a vigência e a validade na data consultada, incluindo convênios encerrados depois dela.

## References

- [Constituição do projeto, versão 2.0.0](../../.specify/memory/constitution.md): qualidade proporcional ao MVP, organização, interface e segurança.
- [LGPD — texto atualizado na Câmara dos Deputados](https://www2.camara.leg.br/legin/fed/lei/2018/lei-13709-14-agosto-2018-787077-normaatualizada-pl.html): referência para finalidade, necessidade, consentimento, revogação, conservação e eliminação (arts. 6º, 8º, 11, 15, 16 e 18). As regras de produto não declaram conformidade jurídica integral.
