#!/usr/bin/env bash
# Scripted walkthrough of the Digital Wallet API.
# Usage: docker compose up -d --build && ./scripts/demo.sh
set -euo pipefail

API=${API:-http://localhost:8080}
RUN=$RANDOM

field() { grep -o "\"$1\":\"\?[^,\"}]*" | head -1 | sed 's/.*:"\{0,1\}//'; }
key() { echo "demo-$RUN-$1"; }
step() { printf '\n\033[1m== %s\033[0m\n' "$1"; }

until [ "$(curl -s -o /dev/null -w '%{http_code}' "$API/actuator/health")" = "200" ]; do sleep 2; done

register_and_login() {
  curl -s -X POST "$API/api/auth/register" -H 'Content-Type: application/json' \
    -d "{\"email\":\"$1-$RUN@example.com\",\"password\":\"Secret123\",\"fullName\":\"$1\"}" > /dev/null
  curl -s -X POST "$API/api/auth/login" -H 'Content-Type: application/json' \
    -d "{\"email\":\"$1-$RUN@example.com\",\"password\":\"Secret123\"}" | field accessToken
}

step "Register Alice and Bob, log in (JWT)"
ALICE=$(register_and_login alice); BOB=$(register_and_login bob)
echo "alice token: ${ALICE:0:25}..."

step "Open USD wallets"
A=$(curl -s -X POST "$API/api/wallets" -H "Authorization: Bearer $ALICE" -H 'Content-Type: application/json' -d '{"currency":"USD"}' | field id)
B=$(curl -s -X POST "$API/api/wallets" -H "Authorization: Bearer $BOB" -H 'Content-Type: application/json' -d '{"currency":"USD"}' | field id)
echo "alice wallet: $A"; echo "bob wallet:   $B"

step "Alice deposits 100.00"
curl -s -X POST "$API/api/wallets/$A/deposits" -H "Authorization: Bearer $ALICE" -H "Idempotency-Key: $(key dep)" \
  -H 'Content-Type: application/json' -d '{"amount":100.00,"description":"Card top-up"}'; echo

step "Alice sends Bob 25.00, then the client retries the SAME request (same Idempotency-Key)"
for attempt in 1 2; do
  curl -s -D - -o /tmp/transfer.json -X POST "$API/api/transfers" -H "Authorization: Bearer $ALICE" \
    -H "Idempotency-Key: $(key xfer)" -H 'Content-Type: application/json' \
    -d "{\"sourceWalletId\":\"$A\",\"destinationWalletId\":\"$B\",\"amount\":25.00,\"description\":\"Dinner\"}" \
    | grep -i "idempotent-replayed" | sed "s/^/attempt $attempt: /"
done
echo "balances -> alice: $(curl -s "$API/api/wallets/$A" -H "Authorization: Bearer $ALICE" | field balance)  bob: $(curl -s "$API/api/wallets/$B" -H "Authorization: Bearer $BOB" | field balance)   (moved once)"

step "Alice tries to overspend"
curl -s -X POST "$API/api/transfers" -H "Authorization: Bearer $ALICE" -H "Idempotency-Key: $(key over)" \
  -H 'Content-Type: application/json' -d "{\"sourceWalletId\":\"$A\",\"destinationWalletId\":\"$B\",\"amount\":1000.00}"; echo

step "Alice's statement (newest first, running balance)"
curl -s "$API/api/wallets/$A/transactions" -H "Authorization: Bearer $ALICE"; echo

step "Admin freezes Bob's wallet; Bob can no longer withdraw"
ADMIN=$(curl -s -X POST "$API/api/auth/login" -H 'Content-Type: application/json' \
  -d "{\"email\":\"admin@wallet.local\",\"password\":\"${ADMIN_PASSWORD:-Admin12345}\"}" | field accessToken)
curl -s -X POST "$API/api/admin/wallets/$B/freeze" -H "Authorization: Bearer $ADMIN" | field status
curl -s -X POST "$API/api/wallets/$B/withdrawals" -H "Authorization: Bearer $BOB" -H "Idempotency-Key: $(key wd)" \
  -H 'Content-Type: application/json' -d '{"amount":5.00}'; echo
