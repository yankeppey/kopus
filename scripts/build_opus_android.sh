#!/usr/bin/env bash
set -euo pipefail

: "${ANDROID_NDK_HOME:?Please export ANDROID_NDK_HOME to point at your NDK}"

# Parse arguments
VARIANT="base"
if [[ "${1:-}" == "--full" ]]; then
  VARIANT="full"
fi

OPUS_DIR="$(cd "$(dirname "$0")/../third_party/opus" && pwd)"
if [[ "$VARIANT" == "full" ]]; then
  OUT_ROOT="$(pwd)/build/opus-full/android"
else
  OUT_ROOT="$(pwd)/build/opus/android"
fi
API=24

ABIS=("arm64-v8a" "armeabi-v7a" "x86_64")
HOSTS=("aarch64-linux-android" "armv7a-linux-androideabi" "x86_64-linux-android")

# Configure flags for full variant (DRED, OSCE, QEXT)
EXTRA_CONFIGURE_FLAGS=""
if [[ "$VARIANT" == "full" ]]; then
  EXTRA_CONFIGURE_FLAGS="--enable-dred --enable-osce --enable-qext"
  echo "🔧 Building FULL variant with DNN features (DRED, OSCE, QEXT)"
else
  echo "🔧 Building BASE variant (vanilla Opus)"
fi

for i in "${!ABIS[@]}"; do
  ABI="${ABIS[$i]}"
  HOST="${HOSTS[$i]}"
  # Auto-detect platform for cross-platform compatibility
  OS="$(uname -s | tr '[:upper:]' '[:lower:]')"
  if [[ "$OS" == "darwin" ]]; then
    # NDK always uses darwin-x86_64 even on Apple Silicon Macs
    PLATFORM="darwin-x86_64"
  else
    PLATFORM="$OS-$(uname -m)"
  fi
  TOOLBIN="$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/$PLATFORM/bin"
  CC_PATH="$TOOLBIN/${HOST}${API}-clang"

  OUT_DIR="$OUT_ROOT/$ABI"
  mkdir -p "$OUT_DIR"

  echo "▶️  Building Opus for $ABI …"
  pushd "$OPUS_DIR" >/dev/null
    make distclean >/dev/null 2>&1 || true
    ./autogen.sh
    # shellcheck disable=SC2086
    ./configure \
        --disable-shared \
        --enable-static \
        --host="$HOST" \
        CC="$CC_PATH" \
        AR="$TOOLBIN/llvm-ar" \
        RANLIB="$TOOLBIN/llvm-ranlib" \
        STRIP="$TOOLBIN/llvm-strip" \
        NM="$TOOLBIN/llvm-nm" \
        CFLAGS="-Oz" \
        --prefix="$OUT_DIR" \
        $EXTRA_CONFIGURE_FLAGS
    make -j"$(nproc 2>/dev/null || sysctl -n hw.ncpu)"
    make install
  popd >/dev/null
  echo "✅  libopus.a → $OUT_DIR/lib/libopus.a"
done