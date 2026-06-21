#!/usr/bin/env bash
# =============================================================================
# check_system_out.sh
#
# Scans Java sources in a Gradle project and reports any use of
# System.out.println or System.out.printf.
#
# Usage:
#   ./check_system_out.sh [OPTIONS] [PROJECT_ROOT]
#
# Arguments:
#   PROJECT_ROOT   Root directory of the Gradle project (default: current dir)
#
# Options:
#   --exclude-packages <pkg1,pkg2,...>
#         Comma-separated list of package prefixes to ignore.
#         e.g. --exclude-packages com.example.debug,com.example.test
#
#   --exclude-classes <Class1,Class2,...>
#         Comma-separated list of simple or fully-qualified class names to ignore.
#         e.g. --exclude-classes DebugHelper,com.example.util.DevUtils
#
#   --help
#         Show this help message and exit.
#
# Exit codes:
#   0  No violations found (or all findings are excluded).
#   1  One or more violations found.
#   2  Usage / argument error.
# =============================================================================

set -euo pipefail

# ---------------------------------------------------------------------------
# Globals
# ---------------------------------------------------------------------------
PROJECT_ROOT="."
EXCLUDE_PACKAGES=()
EXCLUDE_CLASSES=()

# ---------------------------------------------------------------------------
# Utility functions
# ---------------------------------------------------------------------------

usage() {
  sed -n '/^# Usage:/,/^# ====/p' "$0" | sed 's/^# \{0,3\}//'
  exit "${1:-0}"
}

die() {
  echo "ERROR: $*" >&2
  exit 2
}

# Split a comma-separated string into the caller-scoped array 'result'
split_csv() {
  local IFS=','
  read -ra result <<< "$1"
}

# Return 0 if the given package matches any excluded package prefix
package_excluded() {
  local pkg="$1"
  local excl
  for excl in "${EXCLUDE_PACKAGES[@]}"; do
    [[ "$pkg" == "$excl" || "$pkg" == "${excl}."* ]] && return 0
  done
  return 1
}

# Return 0 if fqcn or simple class name matches any excluded class entry
class_excluded() {
  local fqcn="$1"
  local simple="$2"
  local excl
  for excl in "${EXCLUDE_CLASSES[@]}"; do
    [[ "$fqcn" == "$excl" || "$simple" == "$excl" ]] && return 0
  done
  return 1
}

# Echo the package declared in a Java file (empty string if none)
get_package() {
  local file="$1"
  grep -m1 '^[[:space:]]*package[[:space:]]' "$file" \
    | sed 's/^[[:space:]]*package[[:space:]]\+//; s/[[:space:]]*;//' \
    || true
}

# Echo the public type name declared in a Java file, falling back to filename stem
get_class_name() {
  local file="$1"
  grep -m1 -oP '(?<=\bpublic\s)(class|interface|enum|record)\s+\K\w+' "$file" \
    2>/dev/null || basename "$file" .java
}

# ---------------------------------------------------------------------------
# Core logic functions
# ---------------------------------------------------------------------------

parse_args() {
  while [[ $# -gt 0 ]]; do
    case "$1" in
      --exclude-packages)
        [[ $# -lt 2 ]] && die "--exclude-packages requires a value."
        split_csv "$2"; EXCLUDE_PACKAGES=("${result[@]}")
        shift 2 ;;
      --exclude-classes)
        [[ $# -lt 2 ]] && die "--exclude-classes requires a value."
        split_csv "$2"; EXCLUDE_CLASSES=("${result[@]}")
        shift 2 ;;
      --help)
        usage 0 ;;
      -*)
        die "Unknown option: $1" ;;
      *)
        PROJECT_ROOT="$1"
        shift ;;
    esac
  done

  [[ -d "$PROJECT_ROOT" ]] || die "Project root not found: $PROJECT_ROOT"
}

# Populate the JAVA_FILES array; exit cleanly if none found
collect_sources() {
  mapfile -t JAVA_FILES < <(
    find "$PROJECT_ROOT" \
      -type f -name "*.java" \
      -path "*/src/main/java/*"
  )

  if [[ ${#JAVA_FILES[@]} -eq 0 ]]; then
    echo "No Java source files found under: $PROJECT_ROOT"
    exit 0
  fi
}

print_scan_header() {
  echo "Scanning ${#JAVA_FILES[@]} Java source file(s) in: $(realpath "$PROJECT_ROOT")"
  [[ ${#EXCLUDE_PACKAGES[@]} -gt 0 ]] && echo "  Excluded packages : ${EXCLUDE_PACKAGES[*]}"
  [[ ${#EXCLUDE_CLASSES[@]} -gt 0 ]]  && echo "  Excluded classes  : ${EXCLUDE_CLASSES[*]}"
  echo
}

# Return 0 if the file imports System.out statically
has_static_out_import() {
  local file="$1"
  grep -qE '^[[:space:]]*import[[:space:]]+static[[:space:]]+java\.lang\.System\.out[[:space:]]*;' "$file"
}

# Return the non-comment lines matching the forbidden pattern; empty if none.
# Covers both explicit (System.out.println) and static-import (out.println) forms.
find_violations_in_file() {
  local file="$1"
  local pattern='System\.out\.(println|printf)\s*\('

  # If the file uses `import static java.lang.System.out;`, also catch bare out.* calls.
  # The negative lookahead avoids matching unrelated identifiers that happen to end in "out".
  if has_static_out_import "$file"; then
    pattern="${pattern}|(?<![.\w])out\.(println|printf)\s*\("
  fi

  grep -n -P "$pattern" "$file" \
    | grep -vE '^\s*[0-9]+:[[:space:]]*(//|\*)' \
    || true
}

# Print a formatted violation block for one file
report_file_violations() {
  local fqcn="$1"
  local file="$2"
  local matches="$3"

  echo "VIOLATION  $fqcn"
  echo "  File: $file"
  while IFS= read -r line; do
    local lineno="${line%%:*}"
    local content="${line#*:}"
    printf "  Line %-5s %s\n" "${lineno}:" "$content"
  done <<< "$matches"
  echo
}

# Iterate over all sources, skip excluded ones, collect and print violations
scan_sources() {
  VIOLATION_COUNT=0
  declare -gA REPORTED_FILES

  local file pkg cls fqcn matches
  for file in "${JAVA_FILES[@]}"; do
    pkg=$(get_package "$file")
    cls=$(get_class_name "$file")
    fqcn="${pkg:+${pkg}.}${cls}"

    { [[ -n "$pkg" ]] && package_excluded "$pkg"; } && continue
    class_excluded "$fqcn" "$cls"                    && continue

    matches=$(find_violations_in_file "$file")
    [[ -z "$matches" ]] && continue

    REPORTED_FILES["$file"]=1
    report_file_violations "$fqcn" "$file" "$matches"
    VIOLATION_COUNT=$(( VIOLATION_COUNT + $(echo "$matches" | wc -l) ))
  done
}

print_summary() {
  if [[ $VIOLATION_COUNT -eq 0 ]]; then
    echo "✔  No System.out.println / System.out.printf violations found."
    return 0
  else
    echo "✘  Found $VIOLATION_COUNT violation(s) in ${#REPORTED_FILES[@]} file(s)."
    return 1
  fi
}

# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------
main() {
  parse_args "$@"
  collect_sources
  print_scan_header
  scan_sources
  print_summary
}

main "$@"
