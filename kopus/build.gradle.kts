import com.android.build.gradle.tasks.ExternalNativeBuildTask
import org.gradle.jvm.tasks.Jar

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    // Add this:
    `maven-publish`
}

android {
    namespace = "eu.buney.kopus"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    ndkVersion = "27.0.12077973"

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
        ndk { abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86_64") }

        consumerProguardFiles("consumer-rules.pro")
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
                    defFile(project.file("cinterop/opus.def"))

                    // Header search path
                    includeDirs.allHeaders(layout.projectDirectory.file("../third_party/opus/include").asFile)

                }
            }
        }
    }
    applyDefaultHierarchyTemplate()
    sourceSets {
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
    outputs.dir(jniDockerOutputDir)

    environment("DOCKER_BUILDKIT", "1")
    commandLine("/opt/homebrew/bin/docker build -o ${jniDockerOutputDir.get().asFile.absolutePath} ${dockerContext.absolutePath}".split(" "))
}

val buildOpusAppleDir = layout.buildDirectory.dir("opus")
val buildOpusApple by tasks.register<Exec>("buildOpusApple") {
    group = "build"
    description = "Build Opus for macOS/iOS arm64/x86_64"
    inputs.file(project.rootProject.file("scripts/build_opus_apple.sh"))
    outputs.dir(buildOpusAppleDir)

    commandLine("../scripts/build_opus_apple.sh")
}
val buildJniMacosFolder = rootProject.layout.buildDirectory.dir("buildJniMacos")
val buildJniMacos by tasks.register<Exec>("buildJniMacos") {
    group = "build"
    description = "Build JNI libs for macOS arm64/x86_64"
    dependsOn(buildOpusApple)
    inputs.dir(jniSources)
    outputs.dir(buildJniMacosFolder)

    commandLine("../scripts/build_opus_jni.sh")
}

val buildOpusAndroidDir = rootDir.resolve("build/opus/android")
// Task to build Opus for Android before NDK build
val buildOpusAndroid by tasks.register<Exec>("buildOpusAndroid") {
    group = "build"
    description = "Builds Opus native library for Android"

    workingDir = rootDir

    inputs.file(rootDir.resolve("scripts/build_opus_android.sh"))
    // would be nice to add opus sources as input, but I am confused how to
    // separate source files from the build outputs so skipping it for now

    // Inject the NDK path from Android Gradle plugin
    environment("ANDROID_NDK_HOME", android.ndkDirectory.absolutePath)

    commandLine("./scripts/build_opus_android.sh")
    outputs.dir(buildOpusAndroidDir)
}

tasks.named { "ios" in it }.configureEach {
    dependsOn(buildOpusApple)
}

tasks.named<Jar>("jvmJar") {
    dependsOn(buildJniLinuxWindows, buildJniMacos)

    from(buildJniMacosFolder) {
        include("libopus_jni.dylib")
        into("native/macos/arm64")
    }
    from(jniDockerOutputDir) {
        into("native")
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


publishing {
    publications {
        withType<MavenPublication>().configureEach {
            groupId = "eu.buney.kopus"
            version = "1.5.2-SNAPSHOT"

            pom {
                name.set("Kopus")
                description.set("Kotlin Multiplatform bindings for Opus")
                url.set("https://github.com/your-repo")
                licenses {
                    license {
                        name.set("The MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
            }
        }
    }

    repositories {
        mavenLocal()
    }
}
