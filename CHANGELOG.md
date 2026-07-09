# Changelog

## 1.6.1.3

### Fixed
- Fix `OpusDecoder` failing to initialize when it is the first Kopus object created in a process — `nativeCreate()` was called before the native library was loaded ([#8](https://github.com/yankeppey/kopus/pull/8))

## 1.6.1.2

### Fixed
- Fix Windows JNI library loading failure (`opus_jni.dll` renamed to `libopus_jni.dll` to match build output) ([#5](https://github.com/yankeppey/kopus/issues/5), [#6](https://github.com/yankeppey/kopus/pull/6))

### Changed
- Kotlin and library dependency updates

## 1.6.1.1

### Added
- **Multi-channel audio**: `OpusMultistreamEncoder`/`OpusMultistreamDecoder` for 5.1/7.1 surround sound
- **Spatial audio**: `OpusProjectionEncoder`/`OpusProjectionDecoder` for ambisonics
- **Packet loss handling**: PLC support in `OpusDecoder` (pass `null` to generate concealment audio)
- **24-bit audio**: `encode24()`/`decode24()` methods for higher precision
- **Packet utilities**: `OpusPacket` for inspection, `OpusRepacketizer` for merging/splitting packets
- **kopus-full artifact**: New variant with DRED, OSCE, and QEXT neural network extensions

## 1.6.1

### Changed
- Opus 1.6 -> 1.6.1
- Kotlin and library dependency updates

## 1.6

### Changed
- Opus 1.5.2 -> 1.6
- Kotlin and library dependency updates

## 1.5.2

### Added
- Initial release of Kopus
- Kotlin Multiplatform wrapper for the Opus audio codec (v1.5.2)