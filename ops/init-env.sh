#!/usr/bin/env sh
set -eu
cd "$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
if [ -e .env ]; then
    printf '%s\n' '.env já existe; nenhuma credencial foi substituída.'
    exit 0
fi
umask 077
cat > .env <<EOF
DB_PASSWORD=$(openssl rand -hex 24)
RECEPTION_PASSWORD=$(openssl rand -hex 24)
MANAGER_PASSWORD=$(openssl rand -hex 24)
DOCTOR_PASSWORD=$(openssl rand -hex 24)
CLINIC_ZONE=America/Cuiaba
SPRING_PROFILES_ACTIVE=demo
APP_DOMAIN=
EOF
printf '%s\n' 'Credenciais geradas em .env, ignorado pelo Git. Consulte localmente para entrar.'
