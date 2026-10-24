# Contrato web — MVP

Páginas Thymeleaf em português, login por formulário e sessão. GET /pacientes pesquisa com q, situação e página (20). GET /pacientes/novo e POST /pacientes criam. GET /pacientes/{id} mostra ficha. POSTs por ação sob /pacientes/{id}: editar, situacao, responsaveis, emergencia, convenios, preferencia, consentimentos, revogacoes, anexos, invalidar. GET /anexos/{id} só libera anexo administrativo válido após autorização e auditoria; arquivos clínicos sempre negados.

GET /mesclagem e POST de prévia/confirmacao são gerenciais, com confirmação explícita e resolução dos conflitos. GET /auditoria é gerencial, filtros período/ator/paciente/operação e paginação20.

Formulários incluem CSRF, versão e chave única de submissão. Respostas privadas usam no-store; cookies HttpOnly/SameSite, Secure quando HTTPS. Post/Redirect/Get no sucesso. Erros mantêm dados válidos; conflito exige revisar versão. Labels, foco visível, semântica de tabela e mensagens textuais.

Anexos aceitam PDF/JPEG/PNG até10MiB, bytes compatíveis e ClamAV antes do vínculo. Não servir pasta privada por rota estática. Não revelar nome original clínico.

