#!/usr/bin/env sh
set -eu
cd "$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
umask 077
backup_dir="${1:-backups/$(date -u +%Y%m%dT%H%M%SZ)}"
if [ -e "$backup_dir/database.dump" ] || [ -e "$backup_dir/files.tgz" ]; then
    printf '%s\n' 'Escolha um diretório novo para a cópia.' >&2
    exit 1
fi
mkdir -p "$backup_dir"
docker compose stop app
trap 'docker compose start app >/dev/null' EXIT
docker compose exec -T postgres sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc' > "$backup_dir/database.dump"
docker compose run --rm --no-deps --entrypoint tar app -czf - -C /app/data files > "$backup_dir/files.tgz"
printf 'Cópia concluída em %s\n' "$backup_dir"
