#!/usr/bin/env bash
set -euo pipefail

API=${API:-https://account.monstersandmemories.com/api}
read -rp 'Email: ' EMAIL
read -rsp 'Password: ' PASSWORD; echo

# Runs curl and sets BODY to the response body and HTTP to the HTTP status code
call() {
  local out
  out=$(curl -s -w '\n%{http_code}' "$@")
  HTTP=${out##*$'\n'}
  BODY=${out%$'\n'*}
}

fail() {
  echo "$1 (HTTP $HTTP): $BODY" >&2; exit 1
}

# Prints a JSON field, or nothing if the body is not a JSON object
field() {
  jq -r "if type == \"object\" then .$1 // empty else empty end" <<<"$BODY" 2>/dev/null || true
}

call -X POST "$API/account/login" \
  -H 'Content-Type: application/json; charset=utf-8' \
  -d "$(jq -n --arg e "$EMAIL" --arg p "$PASSWORD" '{email:$e,password:$p,version:21}')"

STATUS=$(field status)
if [[ "$STATUS" == 0 ]]; then
  TOKEN=$(field token)
elif [[ "$STATUS" == 6 && $(field code) == two_factor_required ]]; then
  METHOD=$(field method)
  CHALLENGE=$(field challenge_token)
  read -rp "Verification code ($METHOD): " CODE
  call -X POST "$API/account/login/2fa" \
    -H 'Content-Type: application/json; charset=utf-8' \
    -d "$(jq -n --arg m "$METHOD" --arg c "$CODE" --arg t "$CHALLENGE" '{method:$m,code:$c,challenge_token:$t}')"
  TOKEN=$(field token)
  [[ -n "$TOKEN" ]] || fail "2FA verification failed"
else
  fail "Login failed"
fi

call "$API/game/versions?token=$TOKEN"
STATUS=$(field status)
if [[ "$HTTP" != 2* || ( -n "$STATUS" && "$STATUS" != 0 ) ]]; then
  fail "Fetching game versions failed"
fi
jq . <<<"$BODY"
