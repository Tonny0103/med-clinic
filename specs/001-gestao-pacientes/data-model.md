# Modelo do MVP

Patient é o agregado administrativo: UUID, prontuário imutável, versão, nome/nascimento/sexo/nacionalidade, CPF e documento alternativo, telefone/email/endereço, situação ATIVO/INATIVO/MESCLADO e destino de mesclagem. Contém responsáveis com vigência, emergência, convênios com períodos e segmento anterior, preferência atual, decisões imutáveis, metadados de anexos e registros de mesclagem.

Persistência: tabela patients com colunas de pesquisa/identidade únicas e payload textual; alterações transacionais verificam versão. Prontuário provém de sequência independente e nunca é reutilizado. Filhos têm UUID e originalPatientId; períodos de responsáveis e convênios são preservados. CPF anterior legítimo/documento alternativo continuam reservados; correção documentada libera associação errada e mantém histórico/auditoria. Alias mesclado conserva número e dados de origem.

Coverage: startsOn inclusivo, endsOn primeiro dia fora da vigência, expiresOn inclusivo. Renovação cria sucessor com supersedesCoverageId e mantém validade histórica anterior. Consulta passada não muda preferência atual.

ConsentDecision: finalidade, versão do termo, decisão, manifestação, registro UTC, declarante, responsável quando necessário, autor e evidenceAttachmentId. Sem alteração ou exclusão. Mesclagem conserva decisões e resolve conflito conservadoramente.

Attachment: UUID, origem, categoria, classificação, mime, tamanho, referência privada, autor, instante e invalidação/motivo. Nunca exibir nome original de arquivo clínico. Prova deve ser administrativa válida e pertencer à identidade. Conteúdo fora do banco e da pasta estática; erro transacional pode deixar somente órfão privado, nunca vínculo bem-sucedido.

AuditEvent: UUID, UTC, ator/perfil, operação, referência do paciente/objeto, resultado, nomes de campos e motivo/referências de provas. Sem valores sensíveis. API interna somente append; sem rota de alteração/exclusão. Operações de escrita e evento compartilham transação. Chave de submissão única protege repetição.

