cmake_minimum_required(VERSION 3.18)
project(opus_jni LANGUAGES C)

# Option to build with DRED support (for kopus-full artifact)
option(KOPUS_ENABLE_DRED "Build with DRED support (requires Opus built with --enable-dred)" OFF)

# Core JNI sources (always compiled)
set(JNI_SOURCES
    opus_jni.c
    opus_decoder_jni.c
    opus_opus_jni.c
    opus_repacketizer_jni.c
    opus_multistream_encoder_jni.c
    opus_multistream_decoder_jni.c
)

# DRED JNI sources (only compiled for full variant)
if(KOPUS_ENABLE_DRED)
    list(APPEND JNI_SOURCES opus_dred_jni.c)
    message(STATUS "Building with DRED support enabled")
endif()

# Shared JNI glue code
add_library(opus_jni SHARED ${JNI_SOURCES})

# Include JNI headers
if (NOT ANDROID)
    find_package(JNI COMPONENTS JVM REQUIRED)
endif ()
target_include_directories(opus_jni PRIVATE ${JNI_INCLUDE_DIRS})

# Include Opus headers
if(NOT DEFINED OPUS_INCLUDE_DIR)
    # Fallback to vendored copy
    set(OPUS_INCLUDE_DIR ${CMAKE_CURRENT_LIST_DIR}/../../third_party/opus/include)
endif()
target_include_directories(opus_jni PRIVATE ${OPUS_INCLUDE_DIR})


if(ANDROID)
    # Platform-specific linking
    # Android: link against prebuilt .a for each ABI
    # Use opus-full directory when DRED is enabled
    if(KOPUS_ENABLE_DRED)
        set(PREBUILT_ROOT ${CMAKE_CURRENT_LIST_DIR}/../../build/opus-full/android)
    else()
        set(PREBUILT_ROOT ${CMAKE_CURRENT_LIST_DIR}/../../build/opus/android)
    endif()
    set(OPUS_LIB ${PREBUILT_ROOT}/${ANDROID_ABI}/lib/libopus.a)

    add_library(opus STATIC IMPORTED)
    set_target_properties(opus PROPERTIES IMPORTED_LOCATION ${OPUS_LIB})
    
    # Add 16KB page size alignment for Android 15+ compatibility
    # Required for Google Play starting November 1st, 2025
    target_link_options(opus_jni PRIVATE 
        "-Wl,-z,max-page-size=16384"
        "-Wl,-z,common-page-size=16384"
    )

    target_link_libraries(opus_jni PRIVATE opus ${JNI_LIBRARIES} m)

    message(STATUS "Configured JNI shared library for platform: ${CMAKE_SYSTEM_NAME}")
else ()
    if(NOT DEFINED OPUS_LIB_PATH)
        # Legacy path layout
        set(OPUS_LIB_PATH ${CMAKE_CURRENT_LIST_DIR}/../../build/opus/macosx/${CMAKE_SYSTEM_PROCESSOR}/lib/libopus.a)
    endif()
    add_library(opus STATIC IMPORTED)
    set_target_properties(opus PROPERTIES IMPORTED_LOCATION "${OPUS_LIB_PATH}")


    if (WIN32)
        target_link_libraries(opus_jni PRIVATE ${JNI_LIBRARIES} opus m)
    else()
        target_link_libraries(opus_jni PRIVATE opus m)
    endif()

    message(STATUS "Configured JNI shared library for platform: ${TARGET_PLATFORM}")
endif()
