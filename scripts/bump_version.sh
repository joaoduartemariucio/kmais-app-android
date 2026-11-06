#!/usr/bin/env bash
set -euo pipefail

part="${1:-}"
file="${2:-version.properties}"

case "$part" in
  major|minor|patch) ;;
  *) echo "usage: $0 major|minor|patch [version.properties]" >&2; exit 2 ;;
esac

name=$(sed -n 's/^VERSION_NAME=//p' "$file" | tr -d '\r')
code=$(sed -n 's/^VERSION_CODE=//p' "$file" | tr -d '\r')

if ! [[ "$name" =~ ^([0-9]+)\.([0-9]+)\.([0-9]+)$ ]]; then
  echo "VERSION_NAME '$name' is not MAJOR.MINOR.PATCH" >&2
  exit 1
fi
major="${BASH_REMATCH[1]}"
minor="${BASH_REMATCH[2]}"
patch="${BASH_REMATCH[3]}"

if ! [[ "$code" =~ ^[0-9]+$ ]]; then
  echo "VERSION_CODE '$code' is not a number" >&2
  exit 1
fi

case "$part" in
  major) major=$((major + 1)); minor=0; patch=0 ;;
  minor) minor=$((minor + 1)); patch=0 ;;
  patch) patch=$((patch + 1)) ;;
esac

name="$major.$minor.$patch"
code=$((code + 1))

printf 'VERSION_NAME=%s\nVERSION_CODE=%s\n' "$name" "$code" > "$file"
echo "VERSION_NAME=$name"
echo "VERSION_CODE=$code"
