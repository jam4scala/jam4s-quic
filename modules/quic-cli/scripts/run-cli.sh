#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
JAR_PATH="$SCRIPT_DIR/jam4s-quic-cli.jar"

if [[ ! -f "$JAR_PATH" ]]; then
  echo "Fat JAR not found at $JAR_PATH" >&2
  exit 1
fi

JAVA_OPTS="${JAVA_OPTS:--Xms512m -Xmx1G}"

# If requires deep reflective access on JDK 21+
UNNAMED_FLAGS=(
  --add-opens=java.base/java.nio.channels.spi=ALL-UNNAMED
  --add-opens=java.base/java.nio=ALL-UNNAMED
  --add-exports=java.base/jdk.internal.ref=ALL-UNNAMED
  --add-exports=java.base/sun.nio.ch=ALL-UNNAMED
  --add-exports=jdk.unsupported/sun.misc=ALL-UNNAMED
  --add-opens=java.base/java.lang=ALL-UNNAMED
  --add-opens=java.base/java.lang.reflect=ALL-UNNAMED
  --add-opens=java.base/java.io=ALL-UNNAMED
  --add-opens=java.base/java.util=ALL-UNNAMED
)

exec java $JAVA_OPTS "${UNNAMED_FLAGS[@]}" -jar "$JAR_PATH" "$@"
