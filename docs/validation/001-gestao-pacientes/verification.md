# Verificação do MVP — 2026-10-24

Escopo: oito histórias, 24 pacientes fictícios na demonstração e poucos testes essenciais. Sem dados reais, cobertura mínima, carga ou aprovações formais.

## Automatizado

Comando: `mvnw.cmd -B -ntp -o spotless:apply verify spotless:check`.

Resultado: **21 testes, zero falhas/erros**, pacote executável gerado e Spotless aprovado. Classes: RulesTest (5), AccessPolicyTest (2), PrivateFileTest (2) e FoundationTest (12). A integração automatizada usa H2 e scanner controlado.

Os testes cobrem CPF/menor/contatos/suspeita, vigência histórica, três logins reais e senha errada, documento único, repetição, conflito de versão, rollback de ficha/evento, ciclo de situação, renovação, revogação de inativo com nova prova, arquivos privados, negação clínica aos três perfis, invalidação, mesclagem/idempotência/prévia obsoleta, aliases, preservação de resumos/revogação e consulta gerencial de auditoria sem recursão. Na revisão final foram corrigidos o verificador de senhas e a reserva/busca de um CPF errado corrigido após mesclagem.

## Execução real local

- Docker Desktop/Linux amd64: imagem compilada do código; wrapper ZIP conferido com SHA512 oficial. A imagem de build inclui unzip para manter o formato de distribuição e seu SHA256 fixado.
- PostgreSQL 17 real: migração Flyway aplicada e 24 pacientes fictícios criados. Reinício mantém a massa sem duplicá-la.
- App, PostgreSQL e ClamAV saudáveis. Manifestos de Java/PostgreSQL e da variante Debian do scanner oferecem ARM64; ARM64 e Oracle não foram executados.
- Navegador Edge isolado: login dos três perfis; busca por nome sem acento e paginação 20/4; ficha, formulário de edição e auditoria renderizados.
- Dois uploads PNG fictícios passaram pelo ClamAV real e foram persistidos no volume privado. Documento administrativo retornou HTTP 200 ao médico; arquivo clínico retornou HTTP 403 a gerente, recepção e médico. Médico foi recusado na auditoria; recepção na mesclagem.
- Labels dos controles da ficha conferidos; Enter usado no login/busca. Tela de 390 px sem transbordamento horizontal, além de conferência visual desktop. Não é laudo de WCAG ou teste completo com leitor de tela.
- Scripts PowerShell e Linux passaram na validação de sintaxe. O gerador foi executado novamente e preservou .env; reinícios mantiveram os mesmos 24 pacientes. Compose básico e override HTTPS passaram em config --quiet, sem publicar domínio.

Capturas locais de conferência ficam em target/qa-*.png (ignoradas no Git). Credenciais ficam somente no .env local, igualmente ignorado.

## Limites

Não foi provisionada VM Oracle nem publicado domínio/HTTPS real. O guia e os comandos de cópia/recuperação estão preparados; restauração completa não foi executada. Não foi feito teste de carga, estudo com usuários ou avaliação jurídica/clínica. A primeira entrega mantém o histórico de atendimentos não configurado; os dados fictícios semeados não contêm atendimentos.
