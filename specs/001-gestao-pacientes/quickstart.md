# Verificação rápida do MVP

Os comandos e variáveis executáveis estão no README da raiz, atualizado com o scaffold.

Usar somente dados fictícios, dezenas de pacientes e três perfis. Executar testes essenciais com Maven Wrapper. Para demonstração, subir aplicação, PostgreSQL e ClamAV; fazer login como recepção, gerente e médico.

Roteiro: cadastrar adulto e menor sem CPF; rejeitar CPF duplicado e cadastro incompleto; buscar nome/CPF/prontuário e confirmar histórico ainda não disponível; editar com versão, inativar e reativar; manter responsável, emergência e dois convênios; consultar cobertura em data passada; registrar aceite/recusa/revogação, inclusive nova evidência de inativo pelo próprio fluxo; enviar PDF administrativo e arquivo clínico fictício, negar leitura clínica em todos os perfis; invalidar como gerente; pré-visualizar/confirmar mesclagem com duas provas e conferir aliases/decisões; filtrar auditoria como gerente e negar aos outros perfis.

Não há teste de carga, certificação de acessibilidade ou pesquisa de tempo de cadastro obrigatórios. Registrar somente verificações realmente executadas, com limitações, em docs/validation/001-gestao-pacientes/.

