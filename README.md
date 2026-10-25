# Med Clinic

MVP demonstrativo em **Java 21, Spring Boot 4.1.2 e Thymeleaf**, para uma clínica e dezenas de pacientes fictícios. Inclui cadastro e busca, manutenção, responsáveis/emergência/convênios, consentimentos, anexos restritos, mesclagem e auditoria. Médico lê administração; recepção e gerente operam os cadastros; mesclagem, invalidação de anexo e auditoria são gerenciais. Nenhum perfil lê conteúdo clínico.

## Rodar no Windows

Instale JDK 21. Na raiz do projeto, PowerShell:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\ops\Start-Local.ps1
```

O script gera senhas em **.env** (ignorado pelo Git), procura JDK 21 em JAVA_HOME/PATH/.jdks e inicia com H2 e 24 pacientes fictícios se o banco estiver vazio. Abra http://localhost:8080. Usuários: **recepcao**, **gerente**, **medico**; consulte a senha correspondente no .env local. Use `-Empty` no script para não gerar a massa inicial; isso preserva dados que já existam.

A execução local permite apresentar o cadastro sem Docker. Uploads exigem ClamAV em CLAM_HOST/CLAM_PORT e são recusados quando a inspeção está indisponível.

## Rodar com PostgreSQL e scanner

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\ops\New-DemoEnvironment.ps1
docker compose up -d --build
docker compose ps
```

App em http://localhost:8080, exposto somente no localhost. PostgreSQL, arquivos e definições ClamAV usam volumes persistentes. O scanner pode levar alguns minutos para ficar saudável. No Linux, gere .env com `sh ops/init-env.sh`. [Hospedagem Oracle, HTTPS e cópia de segurança](docs/oracle-demo.md).

## Verificar ou desenvolver

Com JAVA_HOME configurado:

```powershell
.\mvnw.cmd test
.\mvnw.cmd verify spotless:check
.\mvnw.cmd spotless:apply
```

No Linux, use `./mvnw` e tenha unzip instalado (a imagem Docker já o inclui). O wrapper fixa Maven 3.9.16 e checksum do ZIP; o primeiro uso baixa dependências. Spotless/Palantir formata Java com quatro espaços, e o compilador usa -Xlint:all. Há poucos testes essenciais, sem cobertura mínima, navegador automatizado obrigatório ou teste de carga. Os testes usam H2 e scanner controlado; validações reais são registradas no [relatório da entrega](docs/validation/001-gestao-pacientes/verification.md).

Código em domain/application/infrastructure/presentation; JDBC armazena agregados administrativos, versões, documentos únicos, recibos e eventos transacionais. Banco hospedado PostgreSQL 17; H2 fica para desenvolvimento/testes. Datas seguem CLINIC_ZONE; instantes de auditoria ficam em UTC. Documentos ficam fora das pastas públicas, até 10 MiB, e passam por parsing e ClamAV. A primeira entrega mostra **Histórico ainda não disponível**; não cria atendimentos fictícios.

Configuração: DB_URL/DB_USER/DB_PASSWORD, RECEPTION_PASSWORD/MANAGER_PASSWORD/DOCTOR_PASSWORD (mínimo 12 caracteres), CLINIC_ZONE, CLINIC_FILES, CLAM_HOST/CLAM_PORT e COOKIE_SECURE. Variáveis ficam no ambiente; overrides application-local.* são ignorados.

[Especificação](specs/001-gestao-pacientes/spec.md) · [Tarefas](specs/001-gestao-pacientes/tasks.md) · [Roteiro de apresentação](specs/001-gestao-pacientes/quickstart.md). Constituição 2.0.0: manter as oito histórias com implantação pequena, sem aprovações formais adicionais.
