#!/usr/bin/env bash
# generate_diagrams.sh
# Usage: ./scripts/generate_diagrams.sh
# Makes an SVG file in docs/images for each Mermaid file (docs/*.mmd).
# Uses the mermaid-cli Docker image. Docker must be installed.

set -euo pipefail

MERMAID_IMAGE=${MERMAID_IMAGE:-minlag/mermaid-cli:12.0.1}
DOCS_DIR="$(cd "$(dirname "$0")/../docs" && pwd)"
IMAGES_DIR="$DOCS_DIR/images"

shopt -s nullglob
SOURCES=("$DOCS_DIR"/*.mmd)
if [[ ${#SOURCES[@]} -eq 0 ]]; then
  echo "No .mmd files in $DOCS_DIR" >&2
  exit 1
fi

mkdir -p "$IMAGES_DIR"

for source in "${SOURCES[@]}"; do
  name=$(basename "$source" .mmd)
  echo "Generating docs/images/$name.svg"
  docker run --rm -u "$(id -u):$(id -g)" -v "$DOCS_DIR:/data" "$MERMAID_IMAGE" \
    -i "/data/$name.mmd" -o "/data/images/$name.svg" -b white
done
