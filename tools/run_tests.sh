#!/usr/bin/env bash
# Testy JVM VitalTrack — bez emulatora i zegarka.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TC="$ROOT/toolchain"
KOTLINC="$TC/kotlinc/bin/kotlinc"
STDLIB="$TC/kotlinc/lib/kotlin-stdlib.jar"
JAVA_HOME="${BIPSYNC_JDK:-$TC/jdk}"
OUT="$(mktemp -d /tmp/vitaltrack_tests_XXXXXX)"
trap 'rm -rf "$OUT"' EXIT

echo "==> Kompilacja czystego rdzenia JVM + testów"
SRC_DIRS=(
  "$ROOT/core/common/src/main/java"
  "$ROOT/core/model/src/main/java"
  "$ROOT/core/analytics/src/main/java"
  "$ROOT/core/testing/src/main/java"
  "$ROOT/core/healthconnect/src/main/java/pl/vitaltrack/core/healthconnect/HcContracts.kt"
  "$ROOT/core/sync/src/main/java"
  "$ROOT/core/bluetooth/src/main/java/pl/vitaltrack/core/bluetooth/Gatt.kt"
  "$ROOT/core/bluetooth/src/main/java/pl/vitaltrack/core/bluetooth/Gateway.kt"
  "$ROOT/device/api/src/main/java"
  "$ROOT/device/amazfit/src/main/java/pl/vitaltrack/device/amazfit/Auth.kt"
  "$ROOT/device/amazfit/src/main/java/pl/vitaltrack/device/amazfit/Codec.kt"
  "$ROOT/tests/src"
)
find "${SRC_DIRS[@]}" -name "*.kt" 2>/dev/null | sort > "$OUT/sources.txt"

export JAVA_HOME="$JAVA_HOME"
export PATH="$JAVA_HOME/bin:$PATH"
"$KOTLINC" -jvm-target 1.8 -nowarn -no-stdlib -classpath "$STDLIB" \
  -d "$OUT/classes" @"$OUT/sources.txt"

echo "==> Uruchamianie testów"
"$JAVA_HOME/bin/java" -cp "$OUT/classes:$STDLIB" pl.vitaltrack.core.testing.TestRunner \
  pl.vitaltrack.core.TimeTest \
  pl.vitaltrack.core.HexTest \
  pl.vitaltrack.core.DedupeTest \
  pl.vitaltrack.core.RangesTest \
  pl.vitaltrack.device.AuthTest \
  pl.vitaltrack.device.CodecTest \
  pl.vitaltrack.ble.GatewayTest \
  pl.vitaltrack.sync.PipelineTest
