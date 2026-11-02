#!/bin/sh
# Pruebas sin Ollama: ZIP portable, instalacion y actualizacion en un HOME temporal.
set -eu
root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd -P)
version=$(tr -d '\r\n' < "$root/VERSION")
archive=${1:-"$root/target/kumo-$version.zip"}
temp=$(mktemp -d "${TMPDIR:-/tmp}/kumo-smoke.XXXXXX")
trap 'rm -rf -- "$temp"' EXIT
trap 'exit 1' HUP INT TERM
fail() { printf '%s\n' "Fallo de prueba: $*" >&2; exit 1; }
expect_exit() {
    expected=$1; shift
    if "$@" > "$temp/command.log" 2>&1; then code=0; else code=$?; fi
    if [ "$code" -ne "$expected" ]; then
        cat "$temp/command.log" >&2
        fail "Salida $code; se esperaba $expected."
    fi
}
if [ -n "${JAVA_HOME:-}" ]; then jar="$JAVA_HOME/bin/jar"; else jar=$(command -v jar); fi
mkdir "$temp/unpacked"
(cd "$temp/unpacked" && "$jar" -xf "$archive")
package="$temp/unpacked/kumo-$version"
for file in kumo.jar lib bin/kumo bin/kumo.cmd bin/kumo.ps1 install.sh install.ps1 README.md LICENSE VERSION examples/brief-template.json; do
    [ -e "$package/$file" ] || fail "Falta $file en el ZIP."
done
[ ! -e "$package/.kumo" ] || fail "El ZIP incluye indices."
HOME="$temp/home"; export HOME
KUMO_HOME="$HOME/.kumo"; export KUMO_HOME
mkdir -p "$KUMO_HOME" "$temp/corpus con espacios/.kumo"
printf '%s' '{}' > "$KUMO_HOME/config.json"
printf '%s' 'NO MODIFICAR' > "$KUMO_HOME/brief-template.json"
printf '%s' 'INDICE DEL USUARIO' > "$temp/corpus con espacios/.kumo/index.json"
expect_exit 0 sh "$package/bin/kumo" --version
grep -Fqx "kumo $version" "$temp/command.log" || fail "Version portable incorrecta."
# Incluye espacios y un dolar literal para comprobar el escape de ~/.profile.
destination="$temp/app con espacios \$literal"
expect_exit 0 sh "$package/install.sh" --prefix "$destination"
[ ! -e "$HOME/.profile" ] || fail "No debe modificar el perfil sin autorizacion."
expect_exit 0 "$destination/bin/kumo" --version
grep -Fqx "kumo $version" "$temp/command.log" || fail "Version instalada incorrecta."
expect_exit 0 "$destination/bin/kumo" --help
grep -Fq 'Comandos:' "$temp/command.log" || fail "Ayuda incorrecta."
expect_exit 2 "$destination/bin/kumo" --opcion-inexistente
(cd "$temp/corpus con espacios" && "$destination/bin/kumo" brief 'archivo inexistente.txt' > "$temp/cwd.log" 2>&1) && fail "La ruta ausente debe fallar."
grep -Fq 'corpus con espacios' "$temp/cwd.log" || fail "Se perdio el directorio de trabajo."
previous=$(cat "$destination/current")
expect_exit 0 sh "$package/install.sh" --prefix "$destination" --add-to-path
[ "$(cat "$destination/current")" != "$previous" ] || fail "No actualizo la version."
[ -d "$destination/releases/$previous" ] || fail "Borro la version anterior."
expect_exit 0 sh "$package/install.sh" --prefix "$destination" --add-to-path
[ "$(grep -c '^export PATH=' "$HOME/.profile")" -eq 1 ] || fail "Duplico la entrada de PATH."
expect_exit 0 sh -c '. "$HOME/.profile"; kumo --version'
[ "$(cat "$KUMO_HOME/config.json")" = '{}' ] || fail "Modifico config.json."
[ "$(cat "$KUMO_HOME/brief-template.json")" = 'NO MODIFICAR' ] || fail "Modifico la plantilla."
[ "$(cat "$temp/corpus con espacios/.kumo/index.json")" = 'INDICE DEL USUARIO' ] || fail "Modifico el indice."
expect_exit 0 sh "$root/bin/kumo" --version
expect_exit 0 sh "$root/install.sh" --skip-build --prefix "$temp/desde repo"
active_before_failure=$(cat "$destination/current")
printf '%s\n' '9.9.9' > "$package/VERSION"
expect_exit 1 sh "$package/install.sh" --prefix "$destination"
[ "$(cat "$destination/current")" = "$active_before_failure" ] || fail "Activo un paquete incompatible."
[ -z "$(find "$destination/releases" -name '.stage.*' -print)" ] || fail "No limpio su staging fallido."
printf '%s\n' "$version" > "$package/VERSION"
mkdir "$temp/foreign"
printf '%s' 'CONSERVAR' > "$temp/foreign/usuario.txt"
expect_exit 1 sh "$package/install.sh" --prefix "$temp/foreign"
[ "$(cat "$temp/foreign/usuario.txt")" = 'CONSERVAR' ] || fail "Piso archivos ajenos."
active=$(cat "$destination/current")
JAVA_HOME="$temp/no-java"; export JAVA_HOME
expect_exit 1 "$destination/bin/kumo" --version
expect_exit 1 sh "$package/install.sh" --prefix "$destination"
[ "$(cat "$destination/current")" = "$active" ] || fail "Activo una version tras un fallo."
printf '%s\n' 'OK: ZIP, repositorio, instalacion, actualizacion, fallos seguros, PATH y conservacion de datos (POSIX).'
