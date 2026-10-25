# Contrato web — MVP implementado

Thymeleaf em português, sessão e login por formulário. Contas externas recepcao, gerente e medico; sem administração de usuários.

| Ação | Rota |
| --- | --- |
| Busca com q/status/page, 20 por página | GET /pacientes |
| Novo cadastro | GET /pacientes/novo; POST /pacientes |
| Ficha e consulta de convênio por reference | GET /pacientes/{id} |
| Edição | GET /pacientes/{id}/editar; POST /pacientes/{id}/editar |
| Inativar/reativar | POST /pacientes/{id}/situacao |
| Responsáveis, emergência, convênio e preferência | POST /pacientes/{id}/vinculos/{action} |
| Manifestação com prova existente | POST /pacientes/{id}/consentimentos |
| Revogação, com prova existente OU nova | POST multipart /pacientes/{id}/revogacoes |
| Upload em ativo / invalidação gerencial | POST /pacientes/{id}/anexos; POST /pacientes/{id}/anexos/invalidar |
| Download administrativo, nunca clínico | GET /anexos/{id} |
| Mesclagem gerencial | GET /mesclagem; POST /mesclagem/comparar, /mesclagem/previa, /mesclagem/confirmar |
| Auditoria gerencial | GET /auditoria |
| Sessão | GET/POST /login; POST /logout |

Actions de vínculos: responsavel, encerrar-responsavel, emergencia, convenio, encerrar-convenio e preferencia. Datas de vigência são locais; timestamps de auditoria UTC.

Formulários de escrita usam CSRF, versão e chave de submissão; sucesso usa Post/Redirect/Get. Conflito exige revisão. Cadastro/edição conservam campos válidos após erro. Respostas protegidas usam no-store. Cookies HttpOnly/SameSite, Secure no perfil público HTTPS.

Anexos PDF/JPEG/PNG não vazios até 10 MiB, conteúdo validado e ClamAV antes do vínculo. A pasta privada não é recurso estático; não se expõe nome original clínico. Revogação de inativo com arquivo novo só ocorre na rota própria, com categoria fixada no servidor.

Referência executável: controllers em src/main/java/br/com/medclinic/presentation. Não há API de agenda, prontuário clínico ou edição de eventos.
