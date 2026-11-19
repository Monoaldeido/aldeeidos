#!/usr/bin/env bash
# Requires Java 21, network access and the official RimuLib 1.3.0 NeoForge jar.
set -euo pipefail
cd "$(dirname "$0")"
if [[ ! -f libs/rimulib-1.3.0-neoforge.jar ]]; then
  echo 'ERRO: coloque o JAR oficial da RimuLib 1.3.0 em libs/rimulib-1.3.0-neoforge.jar' >&2
  exit 1
fi
if ! command -v java >/dev/null 2>&1 || ! java -version 2>&1 | grep -q 'version "21\.'; then
  echo 'ERRO: instale JDK 21 e defina JAVA_HOME corretamente.' >&2
  exit 1
fi
if command -v gradle >/dev/null 2>&1; then
  GRADLE="$(command -v gradle)"
else
  DIST=.local-gradle
  GRADLE="$DIST/gradle-8.14/bin/gradle"
  if [[ ! -x "$GRADLE" ]]; then
    mkdir -p "$DIST"
    echo 'Baixando Gradle 8.14 (distribuicao oficial)...'
    if command -v curl >/dev/null 2>&1; then
      curl -fL --retry 3 'https://services.gradle.org/distributions/gradle-8.14-bin.zip' -o "$DIST/gradle.zip"
    elif command -v wget >/dev/null 2>&1; then
      wget -O "$DIST/gradle.zip" 'https://services.gradle.org/distributions/gradle-8.14-bin.zip'
    else
      echo 'ERRO: curl ou wget necessario para baixar Gradle.' >&2; exit 1
    fi
    unzip -q "$DIST/gradle.zip" -d "$DIST"
    rm -f "$DIST/gradle.zip"
  fi
fi
"$GRADLE" --no-daemon clean build
JAR='build/libs/whip_cinematic-0.1.0.jar'
[[ -f "$JAR" ]] || { echo 'ERRO: build terminou sem criar o JAR esperado' >&2; exit 2; }
echo "JAR criado: $(pwd)/$JAR"
