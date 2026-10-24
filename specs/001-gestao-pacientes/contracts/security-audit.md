# Segurança e auditoria — MVP

| Operação | RECEPCAO | GERENTE | MEDICO |
| --- | --- | --- | --- |
| Buscar/consultar administração e anexo administrativo válido | Sim | Sim | Sim |
| Criar/editar ativo, vínculos e manifestações | Sim | Sim | Não |
| Inativar/reativar com motivo | Sim | Sim | Não |
| Revogar inativo, com prova existente ou nova no próprio fluxo | Sim | Sim | Não |
| Enviar arquivo em ativo | Sim | Sim | Não |
| Ler/baixar/preview arquivo clínico | Não | Não | Não |
| Invalidar anexo e mesclar | Não | Sim | Não |
| Consultar auditoria | Não | Sim | Não |

Sessão deriva de contas configuradas por ambiente; servidor aplica matriz independentemente da interface. CSRF em POST. Sem conta de paciente ou administração de usuários. Eventos append-only pela aplicação incluem ator/perfil/UTC/ação/referência/resultado, motivos e IDs de provas; sem CPF, contatos, texto bruto da busca ou conteúdo de arquivo. Auditoria indisponível bloqueia liberação de leitura e confirmação de escrita. Mesclagem e revogação não ampliam permissões.

