# Med Clinic — demonstração

MVP Java 21 / Spring Boot 4.1.2 / Thymeleaf. Oito histórias de gestão administrativa, somente dados fictícios. Backlog: specs/001-gestao-pacientes/tasks.md. Constituição 2.0.0, sem cobertura mínima ou aprovação formal.

## Desenvolvimento

Instale JDK 21 e configure JAVA_HOME. Maven Wrapper baixa a distribuição fixada na primeira execução. No PowerShell:

```powershell
$env:RECEPTION_PASSWORD = Read-Host 'Senha da recepção'
$env:MANAGER_PASSWORD = Read-Host 'Senha do gerente'
$env:DOCTOR_PASSWORD = Read-Host 'Senha do médico'
.\mvnw.cmd spring-boot:run
```

Usuários: recepcao, gerente e medico. Credenciais vêm do ambiente; não há cadastro de contas. Banco local H2 em data/, PostgreSQL na hospedagem. Abrir http://localhost:8080. Scanner ClamAV precisa estar disponível para enviar arquivos; sem scanner o envio é recusado.

```powershell
.\mvnw.cmd test
.\mvnw.cmd verify
.\mvnw.cmd spotless:apply
.\mvnw.cmd spotless:check
```

No Linux, use ./mvnw. Compiler -Xlint:all verifica avisos de Java; Spotless/Palantir formata com quatro espaços. Testes essenciais *Test rodam no test e verify. Sem suíte de carga. Instruções de hospedagem serão acrescentadas na tarefa final.
