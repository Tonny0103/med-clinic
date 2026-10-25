# Modelo do MVP

Patient é o agregado administrativo: UUID, prontuário imutável, versão, nome/nascimento/sexo/nacionalidade, CPF e documento alternativo, telefone/email/endereço, situação ATIVO/INATIVO/MESCLADO e destino de mesclagem. Contém responsáveis com vigência, emergência, convênios com períodos e segmento anterior, preferência atual, decisões imutáveis, metadados de anexos e registros de mesclagem.

Persistência: patients contém pesquisa, prontuário único, versão e payload textual; identities reserva chaves documentais únicas, inclusive de origens mescladas. Busca de CPF usa a reserva atual, não snapshots antigos. Correção comprovada libera a associação errada mesmo após mesclagem, preservando o histórico e os aliases. Recibos usam ator/chave de submissão. Prontuário vem de sequência independente e nunca é reutilizado. Filhos mantêm UUID/origin e períodos; sourceId/sourceChart das mesclagens conservam as referências originais.

Coverage: startsOn inclusivo, endsOn primeiro dia fora da vigência, expiresOn inclusivo. Renovação cria sucessor com supersedesCoverageId e mantém validade histórica anterior. Consulta passada não muda preferência atual.

ConsentDecision: finalidade, versão do termo, decisão, manifestação, registro UTC, declarante, responsável quando necessário, autor e evidenceAttachmentId. Sem alteração ou exclusão. Mesclagem conserva decisões e resolve conflito conservadoramente.

Attachment: UUID, origem, categoria, classificação, mime, tamanho, referência privada, autor, instante e invalidação/motivo. Nunca exibir nome original de arquivo clínico. Prova deve ser administrativa válida e pertencer à identidade. Conteúdo fora do banco e da pasta estática; erro transacional pode deixar somente órfão privado, nunca vínculo bem-sucedido.

AuditEvent: UUID, UTC, ator/perfil, operação, referência do paciente/objeto, resultado, nomes de campos e motivo/referências de provas. Sem valores sensíveis. API interna somente append; sem rota de alteração/exclusão. Operações de escrita e evento compartilham transação. Chave de submissão única protege repetição.

