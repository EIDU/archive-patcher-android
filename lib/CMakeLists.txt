cmake_minimum_required(VERSION 3.4.1)

project(android_deflater)

add_library(
        android_deflater
        SHARED
        src/main/jni/AndroidDeflater.c)

set(ZLIB_PREBUILT_PATH "${CMAKE_SOURCE_DIR}/src/main/prebuilt/${ANDROID_ABI}/libz.a")

target_link_libraries(
        android_deflater
        ${ZLIB_PREBUILT_PATH})
