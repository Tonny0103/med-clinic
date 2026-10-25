# Uma VM para a apresentação

O alvo é uma VM Linux **Ampere A1, 2 OCPUs e 8 GB de RAM**, com disco de 50 GB e imagens ARM64. Aplicação, PostgreSQL e ClamAV ficam juntos. Não é necessário Kubernetes, banco gerenciado, OIDC ou homologação separada.

Em 2026-10-24, a [Oracle documenta](https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier_topic-Always_Free_Resources.htm) A1 Always Free equivalente a 2 OCPUs e 12 GB totais, sujeito à região principal, disponibilidade e cotas da conta. Confira a indicação Always Free no console antes de criar. Instâncias ociosas podem ser retomadas. Não foi criada conta ou VM por este projeto.

## Subir a demonstração

1. Crie a VM Ubuntu compatível com ARM64 e instale Docker Engine com Compose conforme a [documentação oficial](https://docs.docker.com/engine/install/ubuntu/).
2. Copie ou clone o repositório. Na raiz: `sh ops/init-env.sh`.
3. Execute `docker compose up -d --build`. A primeira compilação e atualização das definições do scanner podem demorar.
4. Verifique `docker compose ps`. O app depende do PostgreSQL pronto; uploads são recusados enquanto o scanner não estiver saudável.
5. Para apresentar por túnel: `ssh -L 8080:localhost:8080 usuario@IP_DA_VM`, abrindo http://localhost:8080 no computador da apresentação. Só SSH precisa estar acessível.
6. Para URL pública, aponte um domínio para a VM, preencha APP_DOMAIN em .env, permita portas 80/443 e execute `docker compose -f compose.yaml -f compose.public.yaml up -d --build`. O Caddy fornece HTTPS e o app usa cookie Secure. Restrinja SSH ao seu IP. Banco e scanner não publicam portas.
7. Entre como recepcao, gerente ou medico; consulte as senhas no .env local. O perfil demo cria 24 pacientes fictícios somente em banco vazio.

O [ClamAV recomenda 3–4 GiB](https://docs.clamav.net/manual/Installing/Docker.html) para inspeção; por isso a VM sugerida usa 8 GB. A variante Debian do scanner foi escolhida por oferecer ARM64. As imagens PostgreSQL/Java também oferecem essa arquitetura. O [Caddy usa HTTPS automático](https://caddyserver.com/docs/automatic-https) quando DNS e portas estão corretos. A publicação com domínio depende desses recursos externos.

## Operação curta

- Parar: `docker compose stop`; voltar: `docker compose up -d`.
- Atualizar uma entrega: `docker compose up -d --build` (acrescente os dois -f quando usar HTTPS).
- Antes da apresentação: conferir login, busca e scanner saudável.
- Cópia consistente: `sh ops/backup.sh`. Pausa o app, copia banco e arquivos privados e volta a iniciá-lo. Guarde a pasta gerada e .env em local privado. Não use `down -v` para parar: isso apaga volumes.

## Recuperar em volumes novos

Use uma cópia confiável e mantenha os volumes originais. Na mesma raiz:

```sh
docker compose -p medclinic-restore up -d postgres clamav
docker compose -p medclinic-restore exec -T postgres sh -c 'pg_restore -U "$POSTGRES_USER" -d "$POSTGRES_DB" --no-owner --no-privileges' < backups/SEU_BACKUP/database.dump
docker compose -p medclinic-restore run --rm --no-deps --entrypoint tar app -xzf - -C /app/data < backups/SEU_BACKUP/files.tgz
docker compose stop app
docker compose -p medclinic-restore up -d app
```

Aguarde o PostgreSQL ficar saudável antes do pg_restore. O nome de projeto diferente cria volumes novos; a porta 8080 só pode ser usada por uma instância. Para HTTPS, use os mesmos dois arquivos -f também na recuperação. Faça cópia de .env separadamente; ela não faz parte do dump. Esta rotina é para a demonstração com dados fictícios.
