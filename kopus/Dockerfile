# ──────────────────────────────────────────────────────────────────────────────
#  Multi‑arch Opus builder (Debian Bookworm)
#    * Native Linux  x86_64         →  /out/linux/x86_64/{lib,include}
#    * Cross Linux   aarch64 (arm64)→  /out/linux/arm64/{lib,include}
#    * Cross Windows x86_64 (mingw) →  /out/windows/x86_64/{lib,include}
#
#  Build with --build-arg OPUS_VARIANT=full to enable DNN features (DRED, OSCE, QEXT)
# ──────────────────────────────────────────────────────────────────────────────

ARG DEBIAN_RELEASE=bookworm
ARG OPUS_VARIANT=base
FROM debian:${DEBIAN_RELEASE} AS base

LABEL maintainer="yankeppey@gmail.com"
ENV DEBIAN_FRONTEND=noninteractive

# ── Toolchain & common deps ──────────────────────────────────────────────────
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        build-essential autoconf automake libtool pkg-config \
        git ca-certificates curl wget ninja-build cmake mingw-w64 \
        openjdk-17-jdk-headless \
        unzip nano && \
    update-ca-certificates

WORKDIR /build

# ----------------------------------------------------------------------------
# 0) Fetch Opus sources *and* run autogen once.
#    autogen.sh downloads ~150 MB of DNN model data; doing it once keeps
#    subsequent stages fast and layers small.
# ----------------------------------------------------------------------------
FROM base AS opus-src
RUN git clone --depth=1 --branch v1.6.1 https://github.com/xiph/opus.git /build/opus

WORKDIR /build/opus
# Run autogen exactly once here – safe because it merely generates the
# autotools "configure" script and downloads the PLC/DNN resources.
RUN ./autogen.sh

# Copy JNI sources into the image
COPY ./jni /jni

# ----------------------------------------------------------------------------
# 1) Native Linux  x86_64 build  (gcc + glibc)
# ----------------------------------------------------------------------------
FROM opus-src AS build-linux-amd64
ARG OPUS_VARIANT=base
RUN apt-get install -y gcc-x86-64-linux-gnu
ENV CC=x86_64-linux-gnu-gcc
RUN EXTRA_FLAGS="" && \
    if [ "$OPUS_VARIANT" = "full" ]; then EXTRA_FLAGS="--enable-dred --enable-osce --enable-qext"; fi && \
    ./configure --host=x86_64-linux-gnu --disable-shared --disable-rtcd --enable-static --prefix=/out/linux/x86_64 CFLAGS="-O3 -fPIC" $EXTRA_FLAGS && \
    make -j$(nproc) && make install

# ----------------------------------------------------------------------------
# 2) Cross Linux  aarch64 build (aarch64-linux-gnu‑gcc)
# ----------------------------------------------------------------------------
FROM opus-src AS build-linux-arm64
ARG OPUS_VARIANT=base
RUN apt-get install -y gcc-aarch64-linux-gnu
ENV CC=aarch64-linux-gnu-gcc
RUN EXTRA_FLAGS="" && \
    if [ "$OPUS_VARIANT" = "full" ]; then EXTRA_FLAGS="--enable-dred --enable-osce --enable-qext"; fi && \
    ./configure --host=aarch64-linux-gnu --disable-shared --disable-rtcd --enable-static --prefix=/out/linux/arm64 CFLAGS="-O3" $EXTRA_FLAGS && \
    make -j$(nproc) && make install

# ----------------------------------------------------------------------------
# 3) Cross Windows x86_64 build (mingw‑w64)
# ----------------------------------------------------------------------------
FROM opus-src AS build-win-amd64
ARG OPUS_VARIANT=base
RUN apt-get install -y mingw-w64
ENV CC=x86_64-w64-mingw32-gcc
RUN EXTRA_FLAGS="" && \
    if [ "$OPUS_VARIANT" = "full" ]; then EXTRA_FLAGS="--enable-dred --enable-osce --enable-qext"; fi && \
    ./configure --host=x86_64-w64-mingw32 --disable-shared --enable-static \
        --prefix=/out/windows/x86_64 CFLAGS="-D_FORTIFY_SOURCE=0 -O3" $EXTRA_FLAGS && \
    make -j$(nproc) && make install


# Download Windows JDK (JDK 17, Windows x64, zip archive)
RUN curl -L -o jdk-win.zip https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.11+9/OpenJDK17U-jdk_x64_windows_hotspot_17.0.11_9.zip && \
    mkdir -p /winjdk && \
    unzip jdk-win.zip -d /winjdk && \
    rm jdk-win.zip

#

###############################################################################
# 4) Build libopus_jni.so  (Linux  x86_64)
###############################################################################
FROM build-linux-amd64 AS jni-linux-amd64
ARG OPUS_VARIANT=base

WORKDIR /jni
RUN DRED_FLAG="" && \
    if [ "$OPUS_VARIANT" = "full" ]; then DRED_FLAG="-DKOPUS_ENABLE_DRED=ON"; fi && \
    cmake -B build -S . \
        -DJAVA_HOME=/usr/lib/jvm/java-17-openjdk-arm64 \
        -DOPUS_INCLUDE_DIR=/out/linux/x86_64/include/opus \
        -DOPUS_LIB_PATH=/out/linux/x86_64/lib/libopus.a \
        $DRED_FLAG \
    && cmake --build build --config Release

# 4.1) Build libopus_jni.so (Linux arm64)
FROM build-linux-arm64 AS jni-linux-arm64
ARG OPUS_VARIANT=base

WORKDIR /jni
RUN DRED_FLAG="" && \
    if [ "$OPUS_VARIANT" = "full" ]; then DRED_FLAG="-DKOPUS_ENABLE_DRED=ON"; fi && \
    cmake -B build -S . \
        -DJAVA_HOME=/usr/lib/jvm/java-17-openjdk-arm64 \
        -DOPUS_INCLUDE_DIR=/out/linux/arm64/include/opus \
        -DOPUS_LIB_PATH=/out/linux/arm64/lib/libopus.a \
        $DRED_FLAG \
    && cmake --build build --config Release
#
# # 4.2) Build libopus_jni.dll (Windows x86_64)
FROM build-win-amd64 AS jni-win-amd64
ARG OPUS_VARIANT=base

WORKDIR /jni
RUN DRED_FLAG="" && \
    if [ "$OPUS_VARIANT" = "full" ]; then DRED_FLAG="-DKOPUS_ENABLE_DRED=ON"; fi && \
    cmake -B build -S . \
        -DJAVA_HOME=/winjdk/jdk-17.0.11+9 \
        -DOPUS_INCLUDE_DIR=/out/windows/x86_64/include/opus \
        -DOPUS_LIB_PATH=/out/windows/x86_64/lib/libopus.a \
        -DCMAKE_SYSTEM_NAME=Windows \
        -DCMAKE_C_COMPILER=x86_64-w64-mingw32-gcc \
        $DRED_FLAG \
    && cmake --build build --config Release

# ----------------------------------------------------------------------------
# 5) Collect artefacts – minimal scratch stage
# ----------------------------------------------------------------------------
FROM scratch AS output

COPY --from=jni-linux-amd64    /jni/build/libopus_jni.so   /linux/x86_64/
COPY --from=jni-linux-arm64    /jni/build/libopus_jni.so   /linux/arm64/
COPY --from=jni-win-amd64      /jni/build/libopus_jni.dll  /windows/x86_64/
