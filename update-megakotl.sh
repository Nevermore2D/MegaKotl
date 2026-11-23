#!/usr/bin/env bash
# Usage: bash update-megakotl.sh /path/to/MegaKotl_web_overlay_fix.zip ~/kotl/MegaKotl
# RUN_APP=0: do not launch Desktop after update
# CHECK_WEB=0: skip Web validation
# CHECK_WEB=1 (default): compile Web development executable
# CHECK_WEB=production: also build optimized Web distribution
set -euo pipefail

archive="${1:-$HOME/Downloads/MegaKotl_web_overlay_fix.zip}"
destination="${2:-$HOME/kotl/MegaKotl}"

for command in unzip gradle mktemp; do
  if ! command -v "$command" >/dev/null 2>&1; then
    echo "Требуется команда: $command" >&2
    exit 1
  fi
done
if [[ ! -f "$archive" ]]; then
  echo "Архив не найден: $archive" >&2
  exit 1
fi
if ! unzip -tq "$archive" >/dev/null; then
  echo "Архив поврежден: $archive" >&2
  exit 1
fi

parent="$(dirname "$destination")"
mkdir -p "$parent"
staging="$(mktemp -d "$parent/.megakotl-update.XXXXXXXX")"
backup=""
installed=0
cleanup() {
  if [[ "$installed" == 0 && -n "$backup" && -e "$backup" && ! -e "$destination" ]]; then
    mv -- "$backup" "$destination"
    echo "Старая версия восстановлена." >&2
  fi
  [[ ! -d "$staging" ]] || rm -rf -- "$staging"
}
trap cleanup EXIT

unzip -q "$archive" -d "$staging"
new_project="$staging/MegaKotl"
if [[ ! -f "$new_project/build.gradle.kts" || ! -f "$new_project/web/build.gradle.kts" || ! -f "$new_project/shared/build.gradle.kts" ]]; then
  echo "Неожиданная структура архива; прежний проект не изменён." >&2
  exit 1
fi

# Check both applications BEFORE replacing anything.
echo "Проверка компиляции Desktop..."
gradle -p "$new_project" :compileKotlin
case "${CHECK_WEB:-1}" in
  0)
    echo "Проверка Web пропущена (CHECK_WEB=0)."
    ;;
  1|development)
    echo "Проверка WebAssembly в режиме Development..."
    gradle -p "$new_project" :web:compileDevelopmentExecutableKotlinWasmJs
    ;;
  production)
    echo "Проверка WebAssembly в режиме Production..."
    gradle -p "$new_project" :web:wasmJsBrowserDistribution
    ;;
  *)
    echo "CHECK_WEB должен быть 0, 1, development или production." >&2
    exit 1
    ;;
esac

# Keep git history/configuration if the existing project is already a repository.
if [[ -d "$destination/.git" ]]; then
  cp -a -- "$destination/.git" "$new_project/.git"
  echo "История Git и origin будут сохранены."
fi

if [[ -e "$destination" ]]; then
  backup="${destination}.backup.$(date +%Y%m%d-%H%M%S)"
  if [[ -e "$backup" ]]; then
    echo "Резервная копия с таким именем уже есть: $backup" >&2
    exit 1
  fi
  mv -- "$destination" "$backup"
fi
mv -- "$new_project" "$destination"
installed=1

echo "Новая версия установлена: $destination"
if [[ -n "$backup" ]]; then echo "Резервная копия: $backup"; fi
if [[ "${RUN_APP:-1}" == 1 ]]; then
  cd "$destination"
  gradle run
fi
