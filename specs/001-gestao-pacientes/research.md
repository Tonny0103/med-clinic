# Decisões técnicas do MVP

- Java 21 + Spring Boot + Thymeleaf atendem à stack solicitada em um único processo.
- PostgreSQL na mesma VM evita um serviço pago adicional. H2 facilita testes locais, sem substituir a validação PostgreSQL documentada.
- JDBC e agregado em JSON textual reduzem mapeamentos; índices únicos e versões permanecem no banco. Não há requisito de escala além de dezenas.
- Login de formulário com três contas externas substitui provedor OIDC separado.
- ClamAV mantém a inspeção de anexos exigida pelo produto. Falha ou indisponibilidade não autoriza upload.
- Não há obrigação de SpotBugs, cobertura de 80%, k6, Prometheus, Keycloak ou ambiente de homologação.
- Oracle: documentação consultada em 2026-10-24 informa A1 Always Free equivalente a 2 OCPUs e 12 GB no total, sujeito à capacidade e região principal. Uma VM de 2 OCPUs/6–12 GB é candidata; conferir a indicação Always Free e a cota real da conta antes de criar recursos. Instâncias ociosas podem ser retomadas. Não há garantia de disponibilidade ou custo fora das cotas.
  Fonte: [Oracle Always Free Resources](https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier_topic-Always_Free_Resources.htm).

