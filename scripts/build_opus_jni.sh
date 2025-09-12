#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR/.."

BASE_BUILD_DIR="$PROJECT_ROOT/build/buildJniMacos"
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
    
    cmake -B "$BUILD_DIR" -S "$SRC_DIR" \
        -DCMAKE_OSX_ARCHITECTURES="$ARCH" \
        -DJAVA_HOME="${JAVA_HOME}" \
        -DOPUS_INCLUDE_DIR="$PROJECT_ROOT/build/opus/macosx/$ARCH/include/opus" \
        -DOPUS_LIB_PATH="$PROJECT_ROOT/build/opus/macosx/$ARCH/lib/libopus.a"
    
    cmake --build "$BUILD_DIR" --config Release
    
    echo "✅ Built macOS JNI for $ARCH"
done

echo "✅ All macOS JNI architectures built successfully"

