# archive-patcher-android

![Maven Central](https://img.shields.io/maven-central/v/com.eidu/archive-patcher-android)

## Purpose

This library exists because [archive-patcher](https://github.com/EIDU/archive-patcher)'s
patch application (`FileByFileV1DeltaApplier`) does not produce the expected results on
Android 11 and newer, due to changed behaviour of `java.util.zip.Deflater` in those Android
versions. The way this library addresses the issue is by shipping binaries of (unmodified)
zlib 1.2.13, and implementing an `com.eidu.zip.AndroidDeflater` that utilizes the bundled zlib.

## Usage

To add the Gradle dependency:

```
repositories {
    mavenCentral()
}

dependencies {
    implementation("com.eidu:archive-patcher-android:<version>")
}
```

Use `com.eidu.archivepatcher.AndroidFileByFileV1DeltaApplier` as a drop-in replacement for
`archive-patcher`'s `com.google.archivepatcher.applier.FileByFileV1DeltaApplier`:

```java
new AndroidFileByFileV1DeltaApplier()
    .applyDelta(
        baseFile,
        patchInputStream,
        resultOutputStream
    );
```

## License

Since this library contains modified sources that are originally licensed under GPLv2, this
library is as well. See [LICENSE](LICENSE) for details.

test
