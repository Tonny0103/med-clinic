# Contratos internos — MVP

Domínio Java puro valida identidade, datas, suspeita e cobertura. Application expõe casos de uso por Actor (usuário/perfil), sem aceitar perfil do formulário. Portas isolam persistência, auditoria e arquivos. Infrastructure configura transações, armazenamento privado e ClamAV; apresentação só chama aplicação.

Toda escrita recebe versão da raiz e chave de submissão; rejeita conflito e repetição divergente. Sucesso exige alteração e auditoria na mesma transação. Leitura registra evento antes de devolver dados. Permissões conforme spec: R/G escrevem ativos; M lê; G mescla, invalida e consulta auditoria.

Identidade: reserva única inclusive inativo/mesclado; suspeita permite justificativa, documento idêntico bloqueia. Correção exige motivo e prova administrativa válida da mesma pessoa. Inativação/reativação exigem motivo; mesclado nunca reativa.

Revogação: uma prova existente OU arquivo novo, nunca ambos. Arquivo novo passa pela mesma preparação privada, categoria fixa EVIDENCIA_REVOGACAO; vínculo e decisão são atômicos, inclusive para INATIVO. Não habilita upload avulso em inativo.

Mesclagem: prévia com origem/destino, versões, duas provas válidas, escolhas de conflitos e gerente; validade 15 minutos e uso único. Confirmação revalida tudo sob locks ordenados; nomes iguais após normalização estrita e nascimento igual, CPFs divergentes bloqueiam. Preserva IDs, decisões, provas e aliases. Auditoria referencia ambas as provas, sem conteúdo.

Histórico: primeira entrega retorna NÃO_CONFIGURADO (“Histórico ainda não disponível”). Porta pode representar DISPONIVEL, VAZIO ou FALHA quando houver fonte; sem edição manual.

