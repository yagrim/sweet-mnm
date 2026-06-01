#!/usr/bin/env bash
# merge_reflections.sh
# Usage: ./merge_reflections.sh <origin.json> <destination.json> <java.package.prefix>
#
# Copies all reflection entries from origin whose "type" starts with the given
# package prefix into destination. Existing entries in destination are preserved:
#   - If a type already exists in destination, its fields and methods are merged
#     with the origin value instead of replacing the whole object.
#   - If a type is new, it is appended.
# The result is written back to destination.json (a backup is created first).

set -euo pipefail

# ── Argument validation ────────────────────────────────────────────────────────
if [[ $# -ne 3 ]]; then
  echo "Usage: $0 <origin.json> <destination.json> <java.package.prefix>" >&2
  exit 1
fi

ORIGIN="$1"
DEST="$2"
PACKAGE="$3"

for f in "$ORIGIN" "$DEST"; do
  if [[ ! -f "$f" ]]; then
    echo "Error: file not found: $f" >&2
    exit 1
  fi
done

if ! command -v jq &>/dev/null; then
  echo "Error: 'jq' is required but not installed." >&2
  exit 1
fi

# ── Extract matching entries from origin ───────────────────────────────────────
mapfile -t MATCHING_TYPES < <(jq -r --arg pkg "$PACKAGE" \
  '.reflection[]
   | select((.type | type) == "string" and (.type | startswith($pkg)))
   | .type' "$ORIGIN")

if [[ ${#MATCHING_TYPES[@]} -eq 0 ]]; then
  echo "No reflection entries in '$ORIGIN' match package prefix '$PACKAGE'."
  exit 0
fi

echo "Found ${#MATCHING_TYPES[@]} matching entries for prefix '$PACKAGE'."

# ── Backup destination ─────────────────────────────────────────────────────────
BACKUP="${DEST}.bak"
cp "$DEST" "$BACKUP"
echo "Backup created: $BACKUP"

# ── Merge ──────────────────────────────────────────────────────────────────────
# Strategy:
#   1. Keep destination entries outside the package prefix as-is.
#   2. For matching types, merge origin into destination.
#   3. Append matching types that are only present in origin.
MERGED=$(jq \
  --arg pkg "$PACKAGE" \
  --slurpfile origin "$ORIGIN" \
  '
    def pkg_match($pkg):
      (.type | type) == "string" and (.type | startswith($pkg));

    def sort_fields:
      sort_by(.name // "");

    def sort_methods:
      sort_by(.name // "", (.parameterTypes // [] | join("\u0000")));

    def merge_fields($dest; $src):
      (($dest // []) + ($src // []))
      | reduce .[] as $item ({}; .[$item.name] = $item)
      | [.[]]
      | sort_fields;

    def merge_methods($dest; $src):
      (($dest // []) + ($src // []))
      | reduce .[] as $item ({};
          .[{name: $item.name, parameterTypes: ($item.parameterTypes // [])} | @json] = $item)
      | [.[]]
      | sort_methods;

    def merge_entry($dest; $src):
      ($dest + $src)
      | if ($dest.fields == null and $src.fields == null) then del(.fields)
        else .fields = merge_fields($dest.fields; $src.fields)
        end
      | if ($dest.methods == null and $src.methods == null) then del(.methods)
        else .methods = merge_methods($dest.methods; $src.methods)
        end;

    ($origin[0].reflection | map(select(pkg_match($pkg)))) as $from_origin
    | ($from_origin | INDEX(.type)) as $origin_by_type
    | (.reflection | map(select(pkg_match($pkg))) | INDEX(.type)) as $dest_by_type
    | .reflection = (
        (.reflection
          | map(
              if pkg_match($pkg) and ($origin_by_type[.type] != null) then
                merge_entry(.; $origin_by_type[.type])
              else
                .
              end
            )
        )
        + ($from_origin | map(select($dest_by_type[.type] == null)))
      )
  ' "$DEST")

echo "$MERGED" > "$DEST"

# ── Report ────────────────────────────────────────────────────────────────────
UPDATED=()
APPENDED=()

mapfile -t DEST_TYPES_BEFORE < <(jq -r --arg pkg "$PACKAGE" \
  '.reflection[]
   | select((.type | type) == "string" and (.type | startswith($pkg)))
   | .type' "$BACKUP")

declare -A DEST_SET
for t in "${DEST_TYPES_BEFORE[@]}"; do
  DEST_SET["$t"]=1
done

for t in "${MATCHING_TYPES[@]}"; do
  if [[ -n "${DEST_SET[$t]+_}" ]]; then
    UPDATED+=("$t")
  else
    APPENDED+=("$t")
  fi
done

echo ""
echo "====================================="
echo " Merge Report"
echo "====================================="
echo " Origin      : $ORIGIN"
echo " Destination : $DEST"
echo " Package     : $PACKAGE"
echo "-------------------------------------"

echo ""
if [[ ${#APPENDED[@]} -eq 0 ]]; then
  echo " +  No new types appended."
else
  echo " +  Appended (${#APPENDED[@]}):"
  for t in "${APPENDED[@]}"; do
    echo "    - $t"
  done
fi

echo ""
if [[ ${#UPDATED[@]} -eq 0 ]]; then
  echo " ~  No existing types updated."
else
  echo " ~  Updated (${#UPDATED[@]}):"
  for t in "${UPDATED[@]}"; do
    echo "    - $t"
  done
fi

echo "====================================="
