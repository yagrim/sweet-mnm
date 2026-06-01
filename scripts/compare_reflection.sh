#!/usr/bin/env bash
# compare_reflections.sh
# Usage: ./compare_reflections.sh <file1.json> <file2.json>
# Compares reflection arrays between two JSON files by type.

set -euo pipefail

# ── Argument validation ────────────────────────────────────────────────────────
if [[ $# -ne 2 ]]; then
  echo "Usage: $0 <file1.json> <file2.json>" >&2
  exit 1
fi

FILE1="$1"
FILE2="$2"

for f in "$FILE1" "$FILE2"; do
  if [[ ! -f "$f" ]]; then
    echo "Error: file not found: $f" >&2
    exit 1
  fi
done

if ! command -v jq &>/dev/null; then
  echo "Error: 'jq' is required but not installed." >&2
  exit 1
fi

# ── Extract type arrays ────────────────────────────────────────────────────────
mapfile -t TYPES1 < <(jq -r '.reflection[].type' "$FILE1" 2>/dev/null)
mapfile -t TYPES2 < <(jq -r '.reflection[].type' "$FILE2" 2>/dev/null)

if [[ ${#TYPES1[@]} -eq 0 ]]; then
  echo "Warning: no reflection entries found in $FILE1" >&2
fi

# ── Build a lookup set for file2 types ────────────────────────────────────────
declare -A SET2
for t in "${TYPES2[@]}"; do
  SET2["$t"]=1
done

# ── Compare ───────────────────────────────────────────────────────────────────
MISSING=()
IDENTICAL=()
DIFFERENT=()

for t in "${TYPES1[@]}"; do
  if [[ -z "${SET2[$t]+_}" ]]; then
    MISSING+=("$t")
  else
    # Extract the full object for this type from each file (sorted keys for stable comparison)
    OBJ1=$(jq -c --arg type "$t" '.reflection[] | select(.type == $type)' "$FILE1" | jq -S '.')
    OBJ2=$(jq -c --arg type "$t" '.reflection[] | select(.type == $type)' "$FILE2" | jq -S '.')

    if [[ "$OBJ1" == "$OBJ2" ]]; then
      IDENTICAL+=("$t")
    else
      DIFFERENT+=("$t")
    fi
  fi
done

# ── Report ────────────────────────────────────────────────────────────────────
echo "====================================="
echo " Reflection Comparison Report"
echo "====================================="
echo " File 1 : $FILE1  (${#TYPES1[@]} entries)"
echo " File 2 : $FILE2  (${#TYPES2[@]} entries)"
echo "-------------------------------------"

# Identical
echo ""
if [[ ${#IDENTICAL[@]} -eq 0 ]]; then
  echo " ✔  No types are fully identical in both files."
else
  echo " ✔  Identical in both files (${#IDENTICAL[@]}):"
  echo ""
  for t in "${IDENTICAL[@]}"; do
    echo "    - $t"
  done
fi

# Different
echo ""
if [[ ${#DIFFERENT[@]} -eq 0 ]]; then
  echo " ~  No types differ between files."
else
  echo " ~  Present in both but with differences (${#DIFFERENT[@]}):"
  echo ""
  for t in "${DIFFERENT[@]}"; do
    echo "    - $t"
    # Show a per-field diff for clarity
    OBJ1=$(jq -c --arg type "$t" '.reflection[] | select(.type == $type)' "$FILE1" | jq -S '.')
    OBJ2=$(jq -c --arg type "$t" '.reflection[] | select(.type == $type)' "$FILE2" | jq -S '.')
    # Print changed keys
    diff_keys=$(jq -n \
      --argjson o1 "$OBJ1" \
      --argjson o2 "$OBJ2" \
      '[$o1, $o2] | map(keys) | add | unique[] | . as $k |
       select($o1[$k] != $o2[$k]) |
       "      \($k): \($o1[$k] // "null") → \($o2[$k] // "null")"' \
      -r 2>/dev/null || true)
    [[ -n "$diff_keys" ]] && echo "$diff_keys"
  done
fi

# Missing
echo ""
if [[ ${#MISSING[@]} -eq 0 ]]; then
  echo " ✘  All types from File 1 are present in File 2."
else
  echo " ✘  Types in File 1 NOT found in File 2 (${#MISSING[@]}):"
  echo ""
  for t in "${MISSING[@]}"; do
    echo "    - $t"
  done
fi

echo "====================================="
