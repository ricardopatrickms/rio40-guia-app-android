#!/usr/bin/env bash
#
# Compila o APK no contêiner e instala no aparelho ligado na máquina.
#
# A compilação é do Docker; a instalação é do adb do host, porque o daemon do
# Docker Desktop roda numa VM e não alcança o USB.
#
#   ./instalar.sh                                  # API de debug padrão
#   ./instalar.sh http://192.168.0.10:8092/api/    # apontando para outra API
#
set -euo pipefail
# O compose é o da raiz do rio40, que junta este projeto, a guias-api, o
# scale-guide-pro e o rio40graus num projeto Docker só.
cd "$(dirname "$0")/../.."

API_URL="${1:-}"

ADB="${ADB:-$(command -v adb || echo "$HOME/Android/Sdk/platform-tools/adb")}"
if [ ! -x "$ADB" ]; then
    echo "adb não encontrado. Aponte com ADB=/caminho/para/adb $0" >&2
    exit 1
fi

echo "==> Compilando no contêiner"
if [ -n "$API_URL" ]; then
    docker compose run --rm android ./gradlew assembleDebug "-Pguiascale.apiUrl=$API_URL"
else
    docker compose run --rm android ./gradlew assembleDebug
fi

APK="guia/rio40-guia-app-android/app/build/outputs/apk/debug/app-debug.apk"
[ -f "$APK" ] || { echo "APK não saiu em $APK" >&2; exit 1; }

echo "==> Aparelhos"
"$ADB" start-server
"$ADB" devices -l

if [ -z "$("$ADB" devices | sed '1d' | grep -w device || true)" ]; then
    cat >&2 <<'AVISO'

Nenhum aparelho conectado. Ligue o celular por USB com a depuração USB
ativada, ou suba um emulador, e rode de novo. O APK já está pronto em
guia/rio40-guia-app-android/app/build/outputs/apk/debug/app-debug.apk.
AVISO
    exit 1
fi

echo "==> Instalando"
# -r reinstala por cima mantendo os dados; -d aceita versão igual ou anterior,
# que é o caso quando se está indo e voltando entre builds.
"$ADB" install -r -d "$APK"
echo "Instalado: br.com.rio40graus.guiascale"
