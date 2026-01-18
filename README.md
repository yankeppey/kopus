# Kopus

Kopus is a lightweight Kotlin Multiplatform wrapper for the [Opus audio codec](https://opus-codec.org/). It provides Kotlin bindings for Opus encoding and decoding functionality across Android, JVM, and iOS platforms.

![Kopus encoding/decoding demonstration](Screenshot_20250615_160803.png)

## Features

- **Thin wrapper** over the native Opus C API
- **Kotlin Multiplatform** support for Android, JVM, and iOS
- **Complete API access** to all Opus encoder and decoder settings
- **Optimized native libraries** for various architectures

## Supported Platforms

- **Android**: arm64-v8a, armeabi-v7a, x86_64
- **iOS**: arm64 (device), x86_64/arm64 (simulator)
- **macOS**: arm64, x86_64
- **Linux**: x86_64, arm64
- **Windows**: x86_64

Note: The library has been primarily tested on macOS arm64 and Linux x86_64. While other platforms should work, they haven't been extensively tested.

## Installation

### Gradle

```kotlin
dependencies {
    implementation("eu.buney.kopus:kopus:1.6.1")
}
```

> **Note**: Kopus version numbers align with the underlying Opus library versions (with an added patch number) to maintain transparency and make it clear which version of the Opus codec is being used.

## Basic Usage

### Encoding Audio

```kotlin
// Create an encoder (defaults to VOIP application mode)
val encoder = OpusEncoder(
    sampleRate = 48000,  // Sample rate in Hz (8000, 12000, 16000, 24000, or 48000)
    channels = 1,        // 1 for mono, 2 for stereo
    application = OpusApplication.Voip  // Voip, Audio, or RestrictedLowDelay
)

// Configure encoder settings if needed
encoder.setBitrate(32000)  // 32 kbps
encoder.setComplexity(10)  // Maximum quality
encoder.setVBR(1)          // Enable variable bitrate

// Encode audio data
val pcmInput = shortArrayOf(/* your PCM audio samples */)
val encodedBytes = encoder.encode(pcmInput)

// Clean up when done
encoder.close()
```

### Decoding Audio

```kotlin
// Create a decoder
val decoder = OpusDecoder(
    sampleRate = 48000,  // Must match encoder sample rate
    channels = 1         // Must match encoder channels
)

// Decode a packet
val encodedData = byteArrayOf(/* your Opus packet */)
val frameSize = 960  // 20ms at 48kHz
val pcmOutput = decoder.decode(encodedData, frameSize)

// Clean up when done
decoder.close()
```

## Advanced Usage

### Direct Control with ctl/ctlQuery

You can directly control encoder and decoder settings using the `ctl` and `ctlQuery` methods:

```kotlin
// Set bitrate directly using ctl
encoder.ctl(OPUS_SET_BITRATE_REQUEST, 32000)

// Get current bitrate using ctlQuery
val currentBitrate = encoder.ctlQuery(OPUS_GET_BITRATE_REQUEST)
println("Current bitrate: $currentBitrate bps")
```

### Extension Functions

Kopus includes convenient extension functions for all Opus control parameters:

```kotlin
// Encoder controls
encoder.setBitrate(32000)           // Set bitrate to 32 kbps
encoder.setSignal(OPUS_SIGNAL_MUSIC)
encoder.setInbandFEC(1)             // Enable Forward Error Correction
encoder.setPacketLossPerc(10)       // Expect 10% packet loss

// Decoder controls
decoder.setGain(10 * 256)           // 10 dB gain
```

> **Note**: Some extension functions haven't been extensively tested. While they should work correctly as they directly mirror the C API, they should be used with caution and tested in your specific application.

## ProGuard Configuration

### Android

For Android applications, ProGuard rules are automatically included with the library.

### JVM

If you're using ProGuard with JVM applications that include Kopus, add the following rules to your ProGuard configuration:

```
# Ensure native method calls remain intact
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

# Keep core JNI implementation classes
-keep class eu.buney.kopus.OpusEncoder
-keep class eu.buney.kopus.OpusDecoder
-keep class eu.buney.kopus.Opus
```

## Building from Source

Kopus uses different build approaches for each supported platform:

### iOS
iOS bindings use Kotlin/Native's cinterop to directly interface with Opus. Build the native Opus libraries for iOS:
```bash
./scripts/build_opus_apple.sh
```

### Android
Android bindings use JNI. Build the native Opus libraries for Android architectures:
```bash
./scripts/build_opus_android.sh
```

### Desktop Platforms
Desktop builds are more complex as they require libraries for multiple operating systems:

- **macOS**: Build the Opus library and JNI bindings for macOS:
  ```bash
  ./scripts/build_opus_apple.sh   # Builds the Opus library
  ./scripts/build_opus_jni.sh     # Builds the JNI bindings
  ```

- **Linux/Windows**: These platforms use Docker to ensure consistent builds:
  ```bash
  cd kopus/
  docker build -o ./build/jni_docker .
  ```

This produces native libraries that are packaged into the JVM artifacts, ensuring cross-platform compatibility.

## License

Kopus is released under the MIT License. See [LICENSE](LICENSE) for details.
