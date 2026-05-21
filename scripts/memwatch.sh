#!/usr/bin/env bash

# memwatch.sh
# Monitors memory usage (RSS + VSZ) of a process by exact name.
# Prints one line per PID at a fixed interval.
#
# Usage:
#   ./memwatch.sh <process_name> [interval_ms]
#
# Example:
#   ./memwatch.sh sweet 200

set -euo pipefail

PROCESS_NAME="${1:-}"
INTERVAL_MS="${2:-500}"

if [[ -z "$PROCESS_NAME" ]]; then
  echo "Usage: $0 <process_name> [interval_ms]"
  exit 1
fi

INTERVAL_SEC=$(awk "BEGIN { printf \"%.3f\", $INTERVAL_MS / 1000 }")

while true; do
  TIMESTAMP=$(date +"%H:%M:%S.%3N")

  while read -r pid; do
    [[ -z "$pid" ]] && continue
    [[ "$pid" == "$$" ]] && continue

    if [[ -d "/proc/$pid" ]]; then
      read -r rss_kb vsz_kb cmd <<< "$(ps -p "$pid" -o rss=,vsz=,comm=)"
      rss_mb=$(awk "BEGIN { printf \"%.2f\", $rss_kb / 1024 }")
      vsz_mb=$(awk "BEGIN { printf \"%.2f\", $vsz_kb / 1024 }")
      echo "$TIMESTAMP pid=$pid rss=${rss_mb}MB vsz=${vsz_mb}MB cmd=$cmd"
    fi

  done < <(pgrep -x "$PROCESS_NAME" || true)

  sleep "$INTERVAL_SEC"
done
