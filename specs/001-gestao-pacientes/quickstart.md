# Roteiro curto de apresentação

1. Siga o README para gerar .env e iniciar. Para todos os fluxos de arquivos, use docker compose up -d --build e confirme app/postgres/clamav saudáveis. Abra http://localhost:8080 e consulte as senhas no .env local.
2. Entre como recepcao. Busque “Demonstração”: 24 pacientes fictícios, em páginas de 20 e 4. Abra uma ficha e confira “Histórico ainda não disponível”.
3. Cadastre adulto fictício com telefone/endereço e CPF válido que ainda não esteja na base. Tente repetir o documento e omitir contato. Menor precisa de responsável; menor ou estrangeiro sem CPF precisa de documento alternativo completo. E-mail é opcional.
4. Envie imagem/PDF fictício como identidade. Edite dados; para corrigir CPF/documento já registrado, selecione a comprovação e informe motivo. Inative e reative com motivo, conferindo o mesmo prontuário.
5. Acrescente/substitua responsável, atualize emergência e registre dois convênios. Escolha preferência. Renove um segmento, consulte data anterior e confira validade/vigência preservadas.
6. Registre aceite ou recusa por finalidade/versão com prova. Inative e registre revogação com nova prova no próprio formulário; a ficha deve continuar inativa. Upload avulso permanece bloqueado.
7. Em ativo, envie exame fictício. Observe apenas categoria, data, autor e referência neutra. Nenhum perfil poderá baixá-lo; médico consegue ler administração e documentos administrativos, com todas as escritas bloqueadas.
8. Como gerente, invalide vínculo com motivo. Prepare mesclagem de dois cadastros da mesma pessoa, com nome/nascimento coincidentes, CPFs não divergentes e prova administrativa válida em cada um. Resolva diferenças, revise a prévia e confirme. Ambos os prontuários levam ao destino; decisões e origens permanecem.
9. Filtre /auditoria como gerente. Recepção e médico são recusados. Eventos registram também buscas vazias e acessos negados, sem CPF, contatos, texto bruto de pesquisa ou conteúdo de arquivo.

Use apenas dados e documentos fictícios e mantenha a base nas dezenas. Não há carga, aprovação formal, pesquisa com secretárias ou certificação obrigatória. Evidência efetivamente executada: docs/validation/001-gestao-pacientes/verification.md. Hospedagem/backup: docs/oracle-demo.md.
