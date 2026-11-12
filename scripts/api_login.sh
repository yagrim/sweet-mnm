#!/usr/bin/env bash
set -euo pipefail

API=${API:-https://account.monstersandmemories.com/api}
# Set EMAIL and PASSWORD in the environment to skip the prompts
EMAIL=${EMAIL:-}
PASSWORD=${PASSWORD:-}
[[ -n "$EMAIL" ]] || read -rp 'Email: ' EMAIL
[[ -n "$PASSWORD" ]] || { read -rsp 'Password: ' PASSWORD; echo; }

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
  mapfile -t METHODS < <(jq -r '.methods // [] | .[]' <<<"$BODY" 2>/dev/null || true)
  if (( ${#METHODS[@]} > 1 )); then
    DEFAULT=$METHOD
    echo 'Available verification methods:'
    for i in "${!METHODS[@]}"; do
      label=${METHODS[i]}
      [[ "$label" == "$DEFAULT" ]] && label+=' (default)'
      echo "  $((i + 1))) $label"
    done
    while :; do
      read -rp "Method [1-${#METHODS[@]}, Enter for $DEFAULT]: " CHOICE
      if [[ -z "$CHOICE" ]]; then
        break
      elif [[ "$CHOICE" =~ ^[0-9]+$ ]] && (( CHOICE >= 1 && CHOICE <= ${#METHODS[@]} )); then
        METHOD=${METHODS[CHOICE - 1]}; break
      fi
      echo "Incorrect selection: $CHOICE" >&2
    done
    # The server sends the email only for the default method, thus request it for a different selection
    if [[ "$METHOD" == email && "$DEFAULT" != email ]]; then
      call -X POST "$API/account/login/2fa/email" \
        -H 'Content-Type: application/json; charset=utf-8' \
        -d "$(jq -n --arg t "$CHALLENGE" '{challenge_token:$t}')"
      # The server reports success as status=6
      [[ "$(field status)" == 6 ]] || fail "Sending the verification email failed"
    fi
  fi
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
