#!/usr/bin/env bash
# Tests for api_login.sh. A fake curl replaces the real API.
# Usage: ./scripts/test_api_login.sh
set -uo pipefail

SCRIPT="$(cd "$(dirname "$0")" && pwd)/api_login.sh"
WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT

# The fake curl writes each request body to $WORK/requests and
# sends a successful response with an HTTP status line
mkdir -p "$WORK/bin"
cat >"$WORK/bin/curl" <<'EOF'
#!/usr/bin/env bash
body=
while (($#)); do
  if [[ "$1" == -d ]]; then body=$2; shift; fi
  shift
done
[[ -n "$body" ]] && echo "$body" >>"$FAKE_DIR/requests"
printf '{"status":0,"token":"tok"}\n200'
EOF
chmod +x "$WORK/bin/curl"

FAILURES=0

check() {
  local name=$1 expected=$2
  local actual
  actual=$(jq -c '{email,password}' "$WORK/requests")
  if [[ "$actual" == "$expected" ]]; then
    echo "PASS: $name"
  else
    echo "FAIL: $name: expected $expected, got $actual"
    FAILURES=$((FAILURES + 1))
  fi
  rm -f "$WORK/requests"
}

run() {
  env -u EMAIL -u PASSWORD PATH="$WORK/bin:$PATH" FAKE_DIR="$WORK" "$@" "$SCRIPT" >/dev/null 2>&1
}

run EMAIL=a@b.c PASSWORD=secret </dev/null
check "uses EMAIL and PASSWORD from the environment" '{"email":"a@b.c","password":"secret"}'

printf 'x@y.z\npw\n' | run
check "prompts for EMAIL and PASSWORD when not set" '{"email":"x@y.z","password":"pw"}'

printf 'pw\n' | run EMAIL=a@b.c
check "prompts only for PASSWORD when EMAIL is set" '{"email":"a@b.c","password":"pw"}'

printf 'x@y.z\n' | run PASSWORD=secret
check "prompts only for EMAIL when PASSWORD is set" '{"email":"x@y.z","password":"secret"}'

((FAILURES == 0)) || { echo "$FAILURES test(s) failed"; exit 1; }
echo "All tests passed"
