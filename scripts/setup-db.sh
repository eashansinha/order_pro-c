#!/usr/bin/env bash
# Apply sql/procedures.sql and sql/init.sql to an Oracle database.
# Usage: scripts/setup-db.sh [user/password[@connect]]
# Default connection: orderpro/orderpro@//localhost:1521/XEPDB1
set -euo pipefail

CONN="${1:-orderpro/orderpro@//localhost:1521/XEPDB1}"
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

docker run --rm --network host -v "$REPO_ROOT/sql:/sql:ro" oracle-proc bash -c "
{ cat /sql/procedures.sql; echo 'SHOW ERRORS'; cat /sql/init.sql; echo; echo 'EXIT'; } \
  | sqlplus -s '$CONN'
"
