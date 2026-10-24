#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")/.."
MVN=${MVN:-mvn}
exec "$MVN" --batch-mode --no-transfer-progress "$@" test
