#!/usr/bin/env bash
# Regression test: sql/init.sql must apply cleanly AND be idempotent
# (running it twice must produce no ORA-/SP2- errors, and all 6 tables
# and 6 sequences must exist afterwards).
# Usage: scripts/test-db.sh [user/password[@connect]]
set -euo pipefail

CONN="${1:-orderpro/orderpro@//localhost:1521/XEPDB1}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

fail=0

for run in 1 2; do
  out="$("$SCRIPT_DIR/setup-db.sh" "$CONN" 2>&1)"
  if grep -E "ORA-|SP2-|PLS-" <<<"$out"; then
    echo "FAIL: run $run of init.sql produced errors (see above)"
    fail=1
  else
    echo "PASS: run $run of init.sql completed without errors"
  fi
done

counts="$(docker run --rm --network host oracle-proc bash -c "
  { echo \"SELECT 'TABLES=' || count(*) FROM user_tables;\";
    echo \"SELECT 'SEQUENCES=' || count(*) FROM user_sequences;\";
    echo 'EXIT'; } | sqlplus -s '$CONN'
")"

grep -qE "^TABLES=6$" <<<"$counts" && echo "PASS: 6 tables exist" || { echo "FAIL: expected 6 tables"; echo "$counts"; fail=1; }
grep -qE "^SEQUENCES=6$" <<<"$counts" && echo "PASS: 6 sequences exist" || { echo "FAIL: expected 6 sequences"; echo "$counts"; fail=1; }

exit $fail
