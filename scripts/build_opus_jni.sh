#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR/.."

# Parse arguments
VARIANT="base"
if [[ "${1:-}" == "--full" ]]; then
  VARIANT="full"
fi

if [[ "$VARIANT" == "full" ]]; then
  BASE_BUILD_DIR="$PROJECT_ROOT/build/buildJniMacosFull"
  OPUS_BUILD_DIR="$PROJECT_ROOT/build/opus-full"
  echo "🔧 Building FULL variant JNI"
else
  BASE_BUILD_DIR="$PROJECT_ROOT/build/buildJniMacos"
  OPUS_BUILD_DIR="$PROJECT_ROOT/build/opus"
  echo "🔧 Building BASE variant JNI"
fi

SRC_DIR="$PROJECT_ROOT/kopus/jni"

# Architectures to build
ARCHITECTURES=("arm64" "x86_64")

# Build for each architecture
for ARCH in "${ARCHITECTURES[@]}"; do
    echo "Building macOS JNI for $ARCH..."

    BUILD_DIR="$BASE_BUILD_DIR/$ARCH"
    mkdir -p "$BUILD_DIR"

    # Set JAVA_HOME if not set (for local builds)
    if [ -z "${JAVA_HOME:-}" ]; then
        if [ -x "/usr/libexec/java_home" ]; then
            export JAVA_HOME="$(/usr/libexec/java_home)"
        else
            echo "Warning: JAVA_HOME not set and java_home not available"
        fi
    fi

    DRED_FLAG=""
    if [[ "$VARIANT" == "full" ]]; then
        DRED_FLAG="-DKOPUS_ENABLE_DRED=ON"
    fi

    cmake -B "$BUILD_DIR" -S "$SRC_DIR" \
        -DCMAKE_OSX_ARCHITECTURES="$ARCH" \
        -DJAVA_HOME="${JAVA_HOME}" \
        -DOPUS_INCLUDE_DIR="$OPUS_BUILD_DIR/macosx/$ARCH/include/opus" \
        -DOPUS_LIB_PATH="$OPUS_BUILD_DIR/macosx/$ARCH/lib/libopus.a" \
        $DRED_FLAG

    cmake --build "$BUILD_DIR" --config Release

    echo "✅ Built macOS JNI for $ARCH"
done

echo "✅ All macOS JNI architectures built successfully"

