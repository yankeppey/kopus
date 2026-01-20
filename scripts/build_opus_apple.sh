#!/usr/bin/env bash
set -euo pipefail

# Build static libopus.a for iOS and macOS architectures
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OPUS_DIR="$(cd "$SCRIPT_DIR/../third_party/opus" && pwd)"

# Parse arguments
VARIANT="base"
if [[ "${1:-}" == "--full" ]]; then
  VARIANT="full"
fi

if [[ "$VARIANT" == "full" ]]; then
  OUT_ROOT="$SCRIPT_DIR/../build/opus-full"
else
  OUT_ROOT="$SCRIPT_DIR/../build/opus"
fi

# Configure flags for full variant (DRED, OSCE, QEXT)
EXTRA_CONFIGURE_FLAGS=""
if [[ "$VARIANT" == "full" ]]; then
  EXTRA_CONFIGURE_FLAGS="--enable-dred --enable-osce --enable-qext"
  echo "🔧 Building FULL variant with DNN features (DRED, OSCE, QEXT)"
else
  echo "🔧 Building BASE variant (vanilla Opus)"
fi

# Target architectures for iOS and macOS
TARGETS=(
  "arm64 iphoneos"          # iOS device
  "x86_64 iphonesimulator"  # iOS Intel simulator
  "arm64 iphonesimulator"   # iOS Apple Silicon simulator
  "arm64 macosx"            # macOS Apple Silicon
  "x86_64 macosx"           # macOS Intel
)

for entry in "${TARGETS[@]}"; do
  read -r ARCH SDK <<<"$entry"

  # Map each architecture to the appropriate configure host triplet
  case "$ARCH-$SDK" in
    arm64-iphoneos)        HOST=aarch64-apple-darwin ;;
    arm64-iphonesimulator) HOST=aarch64-apple-darwin ;;
    x86_64-iphonesimulator)HOST=x86_64-apple-darwin  ;;
    arm64-macosx)          HOST=aarch64-apple-darwin ;;
    x86_64-macosx)         HOST=x86_64-apple-darwin  ;;
    *) echo "Unknown combo $ARCH/$SDK"; exit 1 ;;
  esac

  # Normalize output path: remove 'simulator' suffix from SDK name
  PREFIX="$OUT_ROOT/${SDK%%simulator}/$ARCH"
  mkdir -p "$PREFIX"

  pushd "$OPUS_DIR" >/dev/null
    make distclean >/dev/null 2>&1 || true
    ./autogen.sh                       # Only generates configure script on first run
    # shellcheck disable=SC2086
    ./configure \
        --disable-shared \
        --enable-static \
        --host="$HOST" \
        --disable-rtcd \
        CC="$(xcrun --sdk $SDK --find clang)" \
        CFLAGS="-isysroot $(xcrun --sdk $SDK --show-sdk-path) -O3 -arch $ARCH" \
        --prefix="$PREFIX" \
        $EXTRA_CONFIGURE_FLAGS
    make -j"$(sysctl -n hw.ncpu)"
    make install
  popd >/dev/null

  echo "✅  Built libopus.a for $ARCH ($SDK)"
done
