#!/usr/bin/env bash
# Check each step the game client needs before it can connect to a world server.
# It tests every server that the server list returns.
# Usage: MNM_TOKEN=<token> ./check_servers.sh
# Optional: MNM_BASE_URL overrides the account server URL.
# The token and the key values are never printed. Downloaded keys are deleted on exit.

set -u

BASE="${MNM_BASE_URL:-https://account.monstersandmemories.com}"

readonly LIST_PATH="/api/account/world_servers"
readonly KEY_LAYOUT_VERSION=1
readonly HMAC_KEY_BYTES=64
readonly AES_KEY_BYTES=32
readonly XOR_KEY_BYTES=64
readonly EXPECTED_KEYS_SIZE=$((1 + HMAC_KEY_BYTES + AES_KEY_BYTES + XOR_KEY_BYTES))
readonly MAX_CLOCK_SKEW_SECONDS=300

WORK_DIR=""
HEADERS_FILE=""
LIST_FILE=""
KEYS_FILE=""
SERVER_ROWS_FILE=""
SERVER_ID=""
SERVER_NAME=""
SERVER_ADDRESS=""
SERVER_PORT=""
SERVER_SUBSCRIPTION=""
PASS_COUNT=0
FAIL_COUNT=0
HAS_JQ=0
SERVER_RESULTS=()

report_pass() { PASS_COUNT=$((PASS_COUNT + 1)); echo "  [PASS] $1"; }
report_fail() { FAIL_COUNT=$((FAIL_COUNT + 1)); echo "  [FAIL] $1"; }
report_info() { echo "  [INFO] $1"; }

print_step() {
  echo
  echo "== $1"
}

print_summary() {
  echo
  echo "== Summary: $PASS_COUNT passed, $FAIL_COUNT failed"
}

# Print the summary and stop. Use it when a later step cannot run.
abort_run() {
  report_fail "$1"
  print_summary
  exit 2
}

require_token() {
  if [ -z "${MNM_TOKEN:-}" ]; then
    echo "Error: MNM_TOKEN is not set." >&2
    echo "Usage: MNM_TOKEN=<token> $0" >&2
    exit 1
  fi
}

require_curl() {
  if ! command -v curl >/dev/null 2>&1; then
    echo "Error: curl is not installed." >&2
    exit 1
  fi
}

detect_jq() {
  if command -v jq >/dev/null 2>&1; then
    HAS_JQ=1
  fi
}

create_work_dir() {
  WORK_DIR="$(mktemp -d)"
  trap 'rm -rf "$WORK_DIR"' EXIT
  HEADERS_FILE="$WORK_DIR/headers.txt"
  LIST_FILE="$WORK_DIR/list.json"
  KEYS_FILE="$WORK_DIR/keys.bin"
  SERVER_ROWS_FILE="$WORK_DIR/servers.tsv"
}

print_environment() {
  echo "Account server: $BASE"
  echo "Local time:     $(date -u '+%Y-%m-%d %H:%M:%S UTC')"
  echo "jq installed:   $([ "$HAS_JQ" = 1 ] && echo yes || echo "no (some checks are skipped)")"
}

check_network_and_tls() {
  print_step "Step 1: network, DNS and TLS"
  local result
  result="$(curl -sS -m 15 -o /dev/null -D "$HEADERS_FILE" \
    -w "%{http_code} %{remote_ip} %{ssl_verify_result}" "$BASE/" 2>&1)"
  local code="${result%% *}"

  if [ "$code" = "000" ] || ! [[ "$code" =~ ^[0-9]{3}$ ]]; then
    abort_run "No HTTP answer from $BASE. curl says: $result"
  fi
  report_pass "Got an HTTP answer (code $code) from $(echo "$result" | cut -d' ' -f2)"

  if [ "$(echo "$result" | cut -d' ' -f3)" = "0" ]; then
    report_pass "TLS certificate is valid"
  else
    report_fail "TLS certificate check failed (code $(echo "$result" | cut -d' ' -f3))"
  fi
}

check_clock() {
  print_step "Step 2: system clock"
  local server_date
  server_date="$(grep -i '^date:' "$HEADERS_FILE" | head -n 1 | cut -d' ' -f2- | tr -d '\r')"
  if [ -z "$server_date" ]; then
    report_info "The server did not send a Date header. Clock not checked."
    return 0
  fi

  local server_epoch local_epoch skew
  if ! server_epoch="$(date -u -d "$server_date" +%s 2>/dev/null)"; then
    report_info "Could not read the server date ($server_date). Clock not checked."
    return 0
  fi
  local_epoch="$(date -u +%s)"
  skew=$((local_epoch - server_epoch))
  skew=${skew#-}

  if [ "$skew" -le "$MAX_CLOCK_SKEW_SECONDS" ]; then
    report_pass "Clock difference to the server: ${skew}s"
  else
    report_fail "Clock difference to the server is ${skew}s. Fix the system time."
  fi
}

http_code_has_error_body() {
  grep -aq '"error"' "$1"
}

fetch_server_list() {
  print_step "Step 3: token check on the server list (GET $LIST_PATH)"
  local code
  code="$(curl -s -m 15 -o "$LIST_FILE" -w "%{http_code}" \
    -H "Authorization: Bearer $MNM_TOKEN" "$BASE$LIST_PATH")"

  if [ "$code" = "000" ]; then
    abort_run "The server list request got no answer."
  fi
  if [[ ! "$code" =~ ^2 ]]; then
    abort_run "The server list request returned HTTP $code."
  fi
  if http_code_has_error_body "$LIST_FILE"; then
    abort_run "HTTP $code, but the body has an error: $(head -c 100 "$LIST_FILE"). Log in again."
  fi
  report_pass "Token accepted (HTTP $code)"
}

# Write one line per server: id, name, address, port, subscription access.
# The fields are separated by the unit separator character, because read merges empty tab fields.
list_servers() {
  print_step "Step 4: servers in the list"
  if [ "$HAS_JQ" = 1 ]; then
    jq -r '.servers[]? | [.id, .name, .address, .port, .subscription_access]
      | map(. // "" | tostring) | join("\u001f")' "$LIST_FILE" >"$SERVER_ROWS_FILE" 2>/dev/null
  else
    grep -oE '"id"[[:space:]]*:[[:space:]]*"[^"]+"' "$LIST_FILE" \
      | sed -E 's/.*:[[:space:]]*"//; s/"$//' | awk 'BEGIN {s = sprintf("%c", 31)} {print $1 s $1 s s s}' >"$SERVER_ROWS_FILE"
  fi

  local count
  count="$(grep -c . "$SERVER_ROWS_FILE")"
  if [ "$count" -eq 0 ]; then
    abort_run "No servers found in the list."
  fi
  report_pass "The list has $count server(s)"
  if [ "$HAS_JQ" = 0 ]; then
    report_info "Without jq, address, port and key comparison are not available."
  fi
}

# Return 1 when the keys cannot be used, so the caller can go on with the next server.
fetch_connection_keys() {
  local code
  code="$(curl -s -m 15 -o "$KEYS_FILE" -w "%{http_code}" -X POST \
    -H "Authorization: Bearer $MNM_TOKEN" "$BASE$LIST_PATH/$SERVER_ID/connection_keys")"

  if [ "$code" = "000" ]; then
    report_fail "The connection_keys request got no answer."
    return 1
  fi
  if [[ ! "$code" =~ ^2 ]]; then
    report_fail "The connection_keys request returned HTTP $code."
    return 1
  fi
  if http_code_has_error_body "$KEYS_FILE"; then
    report_fail "HTTP $code, but the body has an error: $(head -c 100 "$KEYS_FILE")"
    return 1
  fi
  report_pass "Got a response (HTTP $code, $(wc -c <"$KEYS_FILE" | tr -d ' ') bytes)"
}

check_key_layout() {
  echo " key layout: [version][hmac $HMAC_KEY_BYTES][aes $AES_KEY_BYTES][xor $XOR_KEY_BYTES]"
  local size version
  size="$(wc -c <"$KEYS_FILE" | tr -d ' ')"
  version="$(head -c 1 "$KEYS_FILE" | od -An -tu1 | tr -d ' ')"

  if [ "$size" = "$EXPECTED_KEYS_SIZE" ]; then
    report_pass "Size is $size bytes"
  else
    report_fail "Size is $size bytes, expected $EXPECTED_KEYS_SIZE"
  fi
  if [ "$version" = "$KEY_LAYOUT_VERSION" ]; then
    report_pass "Version byte is $version"
  else
    report_fail "Version byte is '$version', expected $KEY_LAYOUT_VERSION"
  fi
}

# Return the bytes of one key from the response as a hex string.
key_hex_from_response() {
  local offset="$1" length="$2"
  tail -c +"$((offset + 1))" "$KEYS_FILE" | head -c "$length" | xxd -p | tr -d '\n'
}

# Return one key from the server list (base64 text) as a hex string.
key_hex_from_list() {
  jq -r --arg id "$SERVER_ID" ".servers[] | select(.id == \$id) | .$1 // empty" "$LIST_FILE" \
    | base64 -d 2>/dev/null | xxd -p | tr -d '\n'
}

compare_one_key() {
  local field="$1" offset="$2" length="$3"
  local from_list from_response
  from_list="$(key_hex_from_list "$field")"
  from_response="$(key_hex_from_response "$offset" "$length")"

  if [ -z "$from_list" ]; then
    report_info "$field is not in the server list. Not compared."
  elif [ "$from_list" = "$from_response" ]; then
    report_pass "$field in the response is the same as in the server list"
  else
    report_fail "$field in the response is different from the server list"
  fi
}

compare_keys_with_list() {
  if [ "$HAS_JQ" = 0 ]; then
    report_info "jq is not installed. Not compared."
    return 0
  fi
  compare_one_key hmac_key 1 "$HMAC_KEY_BYTES"
  compare_one_key aes_key $((1 + HMAC_KEY_BYTES)) "$AES_KEY_BYTES"
  compare_one_key xor_key $((1 + HMAC_KEY_BYTES + AES_KEY_BYTES)) "$XOR_KEY_BYTES"
}

# UDP has no handshake. A reply is not expected, so this check is weak.
check_udp_reachability() {
  echo " UDP send to the world server (weak check)"
  if [ -z "$SERVER_ADDRESS" ] || [ -z "$SERVER_PORT" ]; then
    report_info "No address and port in the server list. Not checked."
    return 0
  fi

  if command -v nc >/dev/null 2>&1; then
    if nc -u -z -w 3 "$SERVER_ADDRESS" "$SERVER_PORT" 2>/dev/null; then
      report_info "UDP packet to $SERVER_ADDRESS:$SERVER_PORT sent, no ICMP error received."
    else
      report_fail "UDP to $SERVER_ADDRESS:$SERVER_PORT gave an error (blocked or port closed)."
    fi
  elif timeout 3 bash -c "echo -n ping > /dev/udp/$SERVER_ADDRESS/$SERVER_PORT" 2>/dev/null; then
    report_info "UDP packet to $SERVER_ADDRESS:$SERVER_PORT sent. This does not prove the server received it."
  else
    report_fail "Could not send a UDP packet to $SERVER_ADDRESS:$SERVER_PORT."
  fi
}

read_server_row() {
  IFS=$'\x1f' read -r SERVER_ID SERVER_NAME SERVER_ADDRESS SERVER_PORT SERVER_SUBSCRIPTION <<<"$1"
}

# Run all checks for one server. Arguments: position, total.
test_server() {
  local position="$1" total="$2"
  local fails_before="$FAIL_COUNT"

  print_step "Server $position of $total: $SERVER_NAME ($SERVER_ID)"
  [ -n "$SERVER_SUBSCRIPTION" ] && report_info "Subscription access: $SERVER_SUBSCRIPTION"
  report_info "UDP target: ${SERVER_ADDRESS:-unknown}:${SERVER_PORT:-unknown}"

  if fetch_connection_keys; then
    check_key_layout
    compare_keys_with_list
  fi
  check_udp_reachability

  if [ "$FAIL_COUNT" -eq "$fails_before" ]; then
    SERVER_RESULTS+=("OK      $SERVER_NAME")
  else
    SERVER_RESULTS+=("FAILED  $SERVER_NAME")
  fi
}

test_all_servers() {
  local total position=0 row
  total="$(grep -c . "$SERVER_ROWS_FILE")"
  while IFS= read -r row; do
    position=$((position + 1))
    read_server_row "$row"
    test_server "$position" "$total"
  done <"$SERVER_ROWS_FILE"
}

print_server_results() {
  print_step "Result per server"
  local line
  for line in "${SERVER_RESULTS[@]}"; do
    echo "  $line"
  done
}

main() {
  require_token
  require_curl
  detect_jq
  create_work_dir
  print_environment
  check_network_and_tls
  check_clock
  fetch_server_list
  list_servers
  test_all_servers
  print_server_results
  print_summary
  [ "$FAIL_COUNT" -eq 0 ]
}

main
