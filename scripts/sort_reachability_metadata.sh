#!/usr/bin/env bash

# sort_reachability_metadata.sh
# Sorts GraalVM reachability metadata in a deterministic order.
#
# Usage:
#   ./scripts/sort_reachability_metadata.sh [path/to/reachability-metadata.json]
#
# Defaults to:
#   src/main/resources/META-INF/native-image/reachability-metadata.json

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
DEFAULT_FILE="$ROOT_DIR/src/main/resources/META-INF/native-image/reachability-metadata.json"
TARGET_FILE="${1:-$DEFAULT_FILE}"

if [[ ! -f "$TARGET_FILE" ]]; then
  echo "Error: file not found: $TARGET_FILE" >&2
  exit 1
fi

if ! command -v jq >/dev/null 2>&1; then
  echo "Error: 'jq' is required but not installed." >&2
  exit 1
fi

TMP_FILE="$(mktemp "${TMPDIR:-/tmp}/${TARGET_FILE##*/}.XXXXXX")"
BACKUP_FILE="${TARGET_FILE}.bak"

cleanup() {
  rm -f "$TMP_FILE"
}
trap cleanup EXIT

cp "$TARGET_FILE" "$BACKUP_FILE"

jq '
  def sort_fields:
    sort_by(.name // "");

  def sort_methods:
    sort_by(.name // "", (.parameterTypes // [] | map(tostring) | join("\u0000")));

  def sort_reflection:
    sort_by(.type // "");

  def sort_resource_globs:
    sort_by(.glob // "");

  def sort_resources:
    (map(select(has("glob"))) | sort_resource_globs)
    + map(select(has("glob") | not));

  . as $root
  | {
      reflection: (
        ($root.reflection // [])
        | map(
            if has("fields") then
              .fields |= sort_fields
            else
              .
            end
            | if has("methods") then
                .methods |= sort_methods
              else
                .
              end
          )
        | sort_reflection
      ),
      resources: (($root.resources // []) | sort_resources)
    }
  + (
      $root
      | to_entries
      | map(select(.key != "reflection" and .key != "resources"))
      | from_entries
    )
' "$TARGET_FILE" > "$TMP_FILE"

mv "$TMP_FILE" "$TARGET_FILE"
trap - EXIT
rm -f "$BACKUP_FILE"

echo "Sorted: $TARGET_FILE"
