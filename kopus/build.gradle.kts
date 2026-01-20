import com.android.build.gradle.tasks.ExternalNativeBuildTask
import org.gradle.jvm.tasks.Jar

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.vanniktechMavenPublish)
}

// ────────────────────────────────────────────────────────────────────────────
//  Build variant configuration
//  Set -Pkopus.full=true to build kopus-full with DRED/OSCE/QEXT support
// ────────────────────────────────────────────────────────────────────────────
val isFullVariant = project.findProperty("kopus.full")?.toString()?.toBoolean() ?: false
val variantSuffix = if (isFullVariant) "-full" else ""
val artifactId = "kopus$variantSuffix"
val opusBuildDirName = if (isFullVariant) "opus-full" else "opus"

if (isFullVariant) {
    logger.lifecycle("Building kopus-full variant with DRED/OSCE/QEXT support")
} else {
    logger.lifecycle("Building kopus (base) variant")
}

// ────────────────────────────────────────────────────────────────────────────
//  Generated build flags
// ────────────────────────────────────────────────────────────────────────────
val generatedSrcDir = layout.buildDirectory.dir("generated/kopus/commonMain/kotlin")

val generateBuildFlags by tasks.registering {
    val outputDir = generatedSrcDir
    outputs.dir(outputDir)
    inputs.property("isFullVariant", isFullVariant)

    doLast {
        val dir = outputDir.get().asFile.resolve("eu/buney/kopus")
        dir.mkdirs()
        dir.resolve("BuildFlags.kt").writeText(
            """
            |/*
            | * Generated file - do not edit manually.
            | * Build variant: ${if (isFullVariant) "kopus-full" else "kopus"}
            | */
            |package eu.buney.kopus
            |
            |internal const val IS_FULL_VARIANT = $isFullVariant
            """.trimMargin()
        )
    }
}

android {
    namespace = "eu.buney.kopus"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    ndkVersion = "27.3.13750724"

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
        ndk { abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86_64") }

        consumerProguardFiles("consumer-rules.pro")

        // Pass DRED flag to CMake for full variant
        externalNativeBuild {
            cmake {
                arguments("-DKOPUS_ENABLE_DRED=${if (isFullVariant) "ON" else "OFF"}")
            }
        }
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }

    externalNativeBuild {
        cmake {
            path = file("jni/CMakeLists.txt")
        }
    }
    // Ensure Opus is built before NDK build
    tasks.withType<ExternalNativeBuildTask>().configureEach {
        dependsOn(buildOpusAndroid)
    }
}

kotlin {
    jvmToolchain(17)
    androidTarget {
        publishLibraryVariants("release")
    }

    val iosTargets = listOf(
        iosArm64(),
        iosX64(),
        iosSimulatorArm64(),
    )
    jvm()

    listOf(
        iosArm64(),
        iosX64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "kopus"
            isStatic = true
        }
    }

    iosTargets.forEach { target ->

        target.compilations.getByName("main") {
            cinterops {
                val libopus by creating {
                    // Use variant-specific cinterop definition
                    val defFileName = if (isFullVariant) "opus-full.def" else "opus.def"
                    defFile(project.file("cinterop/$defFileName"))

                    // Header search path
                    includeDirs.allHeaders(layout.projectDirectory.file("../third_party/opus/include").asFile)

                }
            }
        }
    }
    applyDefaultHierarchyTemplate()
    sourceSets {
        commonMain {
            kotlin.srcDir(generatedSrcDir)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        val androidAndJvmMain by creating {
            dependsOn(commonMain.get())
        }
        androidMain.get().dependsOn(androidAndJvmMain)
        jvmMain.get().dependsOn(androidAndJvmMain)
    }

    targets.configureEach {
        compilations.configureEach {
            compileTaskProvider.get().compilerOptions {
                freeCompilerArgs.add("-Xexpect-actual-classes")
            }
        }
    }
}

// Ensure build flags are generated before all Kotlin compilation and related tasks
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    dependsOn(generateBuildFlags)
}
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile>().configureEach {
    dependsOn(generateBuildFlags)
}
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompileCommon>().configureEach {
    dependsOn(generateBuildFlags)
}

// Ensure build flags are generated before sources jar and metadata tasks
tasks.matching { it.name.contains("ourcesJar") || it.name.contains("MetadataJar") }.configureEach {
    dependsOn(generateBuildFlags)
}

// ────────────────────────────────────────────────────────────────────────────
//  Desktop JNI build (macOS / Linux / Windows)
// ────────────────────────────────────────────────────────────────────────────

val dockerContext    = project.layout.projectDirectory.asFile    // contains Dockerfile
val dockerContextFile = dockerContext.resolve("Dockerfile")
val jniSources       = project.layout.projectDirectory.dir("jni").asFile       // C/JNI sources
val jniDockerOutputDir = layout.buildDirectory.dir("jni_docker")

val buildJniLinuxWindows by tasks.register<Exec>("buildJniLinuxWindows") {

    group       = "native"
    description = "Build Opus + JNI libs in Docker"

    workingDir = File("/opt/homebrew/bin/")

    inputs.file(dockerContextFile)
    inputs.dir(jniSources)
    inputs.property("variant", isFullVariant)
    outputs.dir(jniDockerOutputDir)

    environment("DOCKER_BUILDKIT", "1")
    val variantArg = if (isFullVariant) "full" else "base"
    commandLine("bash", "-c", "/opt/homebrew/bin/docker build --build-arg OPUS_VARIANT=$variantArg -o ${jniDockerOutputDir.get().asFile.absolutePath} ${dockerContext.absolutePath}")
}

val buildOpusAppleDir = layout.buildDirectory.dir(opusBuildDirName)
val buildOpusApple by tasks.register<Exec>("buildOpusApple") {
    group = "build"
    description = "Build Opus for macOS/iOS arm64/x86_64"
    inputs.file(project.rootProject.file("scripts/build_opus_apple.sh"))
    inputs.property("variant", isFullVariant)
    outputs.dir(buildOpusAppleDir)

    val variantArg = if (isFullVariant) "--full" else ""
    commandLine("bash", "-c", "../scripts/build_opus_apple.sh $variantArg")
}
val jniMacosFolderName = if (isFullVariant) "buildJniMacosFull" else "buildJniMacos"
val buildJniMacosFolder = rootProject.layout.buildDirectory.dir(jniMacosFolderName)
val buildJniMacos by tasks.register<Exec>("buildJniMacos") {
    group = "build"
    description = "Build JNI libs for macOS arm64/x86_64"
    dependsOn(buildOpusApple)
    inputs.dir(jniSources)
    inputs.property("variant", isFullVariant)
    outputs.dir(buildJniMacosFolder)

    val variantArg = if (isFullVariant) "--full" else ""
    commandLine("bash", "-c", "../scripts/build_opus_jni.sh $variantArg")
}

val buildOpusAndroidDir = rootDir.resolve("build/$opusBuildDirName/android")
// Task to build Opus for Android before NDK build
val buildOpusAndroid by tasks.register<Exec>("buildOpusAndroid") {
    group = "build"
    description = "Builds Opus native library for Android"

    workingDir = rootDir

    inputs.file(rootDir.resolve("scripts/build_opus_android.sh"))
    inputs.property("variant", isFullVariant)
    // would be nice to add opus sources as input, but I am confused how to
    // separate source files from the build outputs so skipping it for now

    // Inject the NDK path from Android Gradle plugin
    environment("ANDROID_NDK_HOME", android.ndkDirectory.absolutePath)

    val variantArg = if (isFullVariant) "--full" else ""
    commandLine("bash", "-c", "./scripts/build_opus_android.sh $variantArg")
    outputs.dir(buildOpusAndroidDir)
}

tasks.named { "ios" in it }.configureEach {
    if (!project.hasProperty("ci.skip.native.build")) {
        dependsOn(buildOpusApple)
    }
}

tasks.named<Jar>("jvmJar") {
    // Only depend on native build tasks if not in CI mode
    if (!project.hasProperty("ci.skip.native.build")) {
        dependsOn(buildJniLinuxWindows, buildJniMacos)
    }

    from(buildJniMacosFolder) {
        include("arm64/libopus_jni.dylib")
        into("native/macos")
    }
    from(buildJniMacosFolder) {
        include("x86_64/libopus_jni.dylib")
        into("native/macos")
    }
    from(jniDockerOutputDir) {
        into("native")
    }
}

// Configure JVM tests to find native libraries
tasks.named<Test>("jvmTest") {
    // Depend on native library build
    if (!project.hasProperty("ci.skip.native.build")) {
        dependsOn(buildJniMacos)
    }

    // Determine architecture-specific library path
    val arch = System.getProperty("os.arch").lowercase()
    val archPath = if (arch.contains("aarch") || arch.contains("arm")) "arm64" else "x86_64"
    val nativeLibPath = buildJniMacosFolder.get().asFile.resolve(archPath).absolutePath

    // Set java.library.path so System.loadLibrary can find the native library
    systemProperty("java.library.path", nativeLibPath)

    // Show test output in console
    testLogging {
        events("passed", "skipped", "failed", "standardOut", "standardError")
        showStandardStreams = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }

    // Generate plain text reports in addition to HTML
    reports {
        junitXml.required.set(true)  // Machine-readable XML (for CI)
        html.required.set(true)       // HTML reports
    }
}

val cleanBuildOpusAndroid by tasks.registering(Delete::class) {
    delete(buildOpusAndroidDir)
}
val cleanBuildJniMacos by tasks.registering(Delete::class) {
    delete(buildJniMacosFolder)
}
val cleanBuildOpusApple by tasks.registering(Delete::class) {
    delete(buildOpusAppleDir)
}
val cleanBuildJniMacosWindows by tasks.registering(Delete::class) {
    delete(jniDockerOutputDir)
}


tasks.named("clean") {
    dependsOn(cleanBuildOpusAndroid, cleanBuildJniMacos, cleanBuildOpusApple, cleanBuildJniMacosWindows)
}

group = "eu.buney.kopus"
version = libs.versions.kopus.get()

mavenPublishing {
    publishToMavenCentral()

    signAllPublications()

    coordinates(group.toString(), artifactId, version.toString())

    pom {
        name = if (isFullVariant) "Kopus Full" else "Kopus"
        description = if (isFullVariant) {
            "Kotlin Multiplatform bindings for Opus with experimental DNN features (DRED, OSCE, QEXT)"
        } else {
            "Kotlin Multiplatform bindings for Opus"
        }
        inceptionYear = "2025"
        url = "https://github.com/yankeppey/kopus"
        licenses {
            license {
                name = "The MIT License"
                url = "https://opensource.org/licenses/MIT"
            }
        }
        developers {
            developer {
                id = "yankeppey"
                name = "Andrei Buneyeu"
                email = "yankeppey@gmail.com"
                url = "http://buney.eu"
            }
        }
        scm {
            url = "https://github.com/yankeppey/kopus/"
            connection = "scm:git:git://github.com/yankeppey/kopus.git"
            developerConnection = "scm:git:ssh://git@github.com/yankeppey/kopus.git"
        }
    }
}
