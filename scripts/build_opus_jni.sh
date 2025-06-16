#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR/.."

BUILD_DIR="$PROJECT_ROOT/../kopus/build/buildJniMacos"
SRC_DIR="$PROJECT_ROOT/kopus/jni"

mkdir -p "$BUILD_DIR"

cmake -B "$BUILD_DIR" -S "$SRC_DIR"
cmake --build "$BUILD_DIR" --config Release

