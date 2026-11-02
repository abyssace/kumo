#!/bin/sh
# Genera el ZIP con Maven y un checksum usando herramientas locales de Linux.
set -eu
root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd -P)
cd "$root"
sh ./mvnw -B -ntp clean verify
version=$(tr -d '\r\n' < VERSION)
cd target
sha256sum "kumo-$version.zip" > "kumo-$version.zip.sha256"
printf 'Distribucion: %s/target/kumo-%s.zip\n' "$root" "$version"
