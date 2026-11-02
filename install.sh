#!/bin/sh
# Instalacion por usuario. Sin sudo, sin Ollama y sin borrar versiones anteriores.
set -eu
source_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd -P)
install_dir="${HOME:?Falta HOME}/.local/share/kumo"
add_to_path=false
skip_build=false
stage=
fail() { printf '%s\n' "Error: $*" >&2; exit 1; }
cleanup() { if [ -n "$stage" ] && [ -d "$stage" ]; then rm -rf -- "$stage"; fi; }
trap cleanup EXIT
trap 'exit 1' HUP INT TERM
while [ "$#" -gt 0 ]; do
    case "$1" in
        --prefix) [ "$#" -ge 2 ] || fail "Falta la ruta de --prefix."; install_dir=$2; shift 2 ;;
        --add-to-path) add_to_path=true; shift ;;
        --skip-build) skip_build=true; shift ;;
        --help|-h)
            printf '%s\n' 'Uso: sh install.sh [--prefix <carpeta>] [--add-to-path] [--skip-build]'
            exit 0 ;;
        *) fail "Opcion desconocida: $1" ;;
    esac
done
case "$install_dir" in /*) ;; *) fail "--prefix debe ser una ruta absoluta." ;; esac
case "$install_dir" in *:*) fail "La ruta no puede contener dos puntos (separador de PATH)." ;; esac
case "$install_dir" in *'
'*) fail "La ruta no puede contener saltos de linea." ;; esac

if [ -n "${JAVA_HOME:-}" ]; then java="$JAVA_HOME/bin/java"
else java=$(command -v java) || fail "Instala Java 21 o superior y revisa JAVA_HOME."
fi
[ -x "$java" ] || fail "JAVA_HOME no contiene un Java ejecutable."
java_version=$("$java" --version 2>/dev/null) || fail "Se necesita Java 21 o superior."
major=$(printf '%s\n' "$java_version" | sed -n '1s/^[^0-9]*\([0-9][0-9]*\).*/\1/p')
case "$major" in ''|*[!0-9]*) fail "No se pudo comprobar Java." ;; esac
[ "$major" -ge 21 ] || fail "Se necesita Java 21 o superior; se encontro $major."
version=$(tr -d '\r\n' < "$source_dir/VERSION")
case "$version" in ''|.*|*[!A-Za-z0-9._-]*) fail "VERSION invalida." ;; esac

if [ -f "$source_dir/kumo.jar" ]; then
    payload=$source_dir
else
    if ! $skip_build; then
        [ -x "$(dirname -- "$java")/javac" ] || fail "Para compilar necesitas un JDK, no solo un runtime."
        (cd "$source_dir" && sh ./mvnw -B -ntp clean verify) || fail "Fallaron la compilacion o las pruebas."
    fi
    payload="$source_dir/target"
fi
[ -f "$payload/kumo.jar" ] && [ -d "$payload/lib" ] || fail "Falta kumo.jar o lib/. Compila o extrae el ZIP completo."
set -- "$payload/lib/"*.jar
[ -f "$1" ] || fail "No hay dependencias en lib/."

[ ! -L "$install_dir" ] || fail "El destino no puede ser un enlace."
marker="$install_dir/.kumo-install"
if [ -e "$install_dir" ]; then
    [ -d "$install_dir" ] || fail "El destino no es una carpeta."
    if [ -f "$marker" ]; then
        [ "$(cat "$marker")" = 'kumo installer v1' ] || fail "El marcador no pertenece a kumo."
    elif [ -n "$(find "$install_dir" -mindepth 1 -maxdepth 1 -print -quit)" ]; then
        fail "El destino contiene archivos ajenos a kumo. Elige una carpeta nueva o vacia."
    fi
fi
for name in bin releases; do
    [ ! -L "$install_dir/$name" ] || fail "No se instalaran archivos a traves de enlaces."
done
mkdir -p "$install_dir/releases" "$install_dir/bin"
install_dir=$(CDPATH= cd -- "$install_dir" && pwd -P)
marker="$install_dir/.kumo-install"
stage=$(mktemp -d "$install_dir/releases/.stage.XXXXXX")
mkdir "$stage/lib"
cp "$payload/kumo.jar" "$stage/"
for document in LICENSE README.md CHANGELOG.md VERSION; do cp "$source_dir/$document" "$stage/"; done
cp "$payload/lib/"*.jar "$stage/lib/"
check=$("$java" -jar "$stage/kumo.jar" --version) || fail "El paquete no arranca."
[ "$check" = "kumo $version" ] || fail "VERSION no coincide con el manifiesto."
slot="$version-$(basename -- "$stage" | tr -d '.')"
mv "$stage" "$install_dir/releases/$slot"
stage=
cp "$source_dir/bin/kumo" "$install_dir/bin/kumo"
chmod +x "$install_dir/bin/kumo"
printf '%s\n' 'kumo installer v1' > "$marker"
printf '%s\n' "$slot" > "$install_dir/current.tmp"
mv -f "$install_dir/current.tmp" "$install_dir/current"

if $add_to_path; then
    # Escapa caracteres especiales antes de escribir una linea literal en el perfil.
    escaped=$(printf '%s' "$install_dir/bin" | sed 's/[\\"$`]/\\&/g')
    line="export PATH=\"$escaped:\$PATH\""
    profile="$HOME/.profile"
    if [ ! -f "$profile" ] || ! grep -Fqx "$line" "$profile"; then
        printf '\n%s\n%s\n' '# kumo: PATH agregado con autorizacion --add-to-path' "$line" >> "$profile"
    fi
    printf '%s\n' 'PATH agregado a ~/.profile. Abre una sesion nueva o aplica el perfil manualmente.'
else
    printf '%s\n' 'PATH no modificado. Puedes repetir con --add-to-path o usar el lanzador por ruta.'
fi
printf 'Instalado kumo %s en %s\n' "$version" "$install_dir"
printf 'Lanzador: %s/bin/kumo\n' "$install_dir"
printf '%s\n' 'Configuracion, plantilla e indices conservados. Ejecuta kumo init cuando quieras.'
