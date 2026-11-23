#!/usr/bin/env bash
# Usage: bash update-megakotl.sh /path/to/MegaKotl_docs.zip "$HOME/kotl/MegaKotl"
# RUN_APP=0 bash ...  to install without launching the application.
set -euo pipefail

archive="${1:-$HOME/Downloads/MegaKotl_docs.zip}"
destination="${2:-$HOME/kotl/MegaKotl}"

if ! command -v unzip >/dev/null 2>&1; then
  printf 'Ошибка: установите unzip\n' >&2; exit 1
fi
if ! command -v gradle >/dev/null 2>&1; then
  printf 'Ошибка: не найден Gradle в PATH\n' >&2; exit 1
fi
if [[ ! -f "$archive" ]]; then
  printf 'Архив не найден: %s\n' "$archive" >&2; exit 1
fi
if ! unzip -tq "$archive" >/dev/null; then
  printf 'Архив повреждён или некорректен: %s\n' "$archive" >&2; exit 1
fi

parent="$(dirname "$destination")"
mkdir -p "$parent"
staging="$(mktemp -d "$parent/.megakotl-update.XXXXXXXX")"
backup=""
installed=0
cleanup() {
  if [[ "$installed" == 0 && -n "$backup" && -e "$backup" && ! -e "$destination" ]]; then
    mv -- "$backup" "$destination"
    printf 'Предыдущая версия восстановлена.\n' >&2
  fi
  [[ ! -d "$staging" ]] || rm -rf -- "$staging"
}
trap cleanup EXIT

unzip -q "$archive" -d "$staging"
if [[ ! -f "$staging/MegaKotl/build.gradle.kts" ]]; then
  printf 'Неожиданная структура архива; старый проект не изменён.\n' >&2
  exit 1
fi

printf 'Проверка компиляции новой версии до замены старой...\n'
gradle -p "$staging/MegaKotl" compileKotlin

if [[ -e "$destination" ]]; then
  backup="${destination}.backup.$(date +%Y%m%d-%H%M%S)"
  if [[ -e "$backup" ]]; then
    printf 'Уже существует резервная копия: %s\n' "$backup" >&2
    exit 1
  fi
  mv -- "$destination" "$backup"
fi
mv -- "$staging/MegaKotl" "$destination"
installed=1

printf '\nНовая версия установлена: %s\n' "$destination"
if [[ -n "$backup" ]]; then printf 'Резервная копия: %s\n' "$backup"; fi
if [[ "${RUN_APP:-1}" == 1 ]]; then
  cd "$destination"
  gradle run
fi
