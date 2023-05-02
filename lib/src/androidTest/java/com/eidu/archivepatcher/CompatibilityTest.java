package com.eidu.archivepatcher;

import android.os.Build;

import com.eidu.zip.AndroidDeflater;
import com.google.archivepatcher.shared.DefaultDeflateCompatibilityWindow;
import com.google.archivepatcher.shared.DefaultDeflater;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

public class CompatibilityTest {
    @Test
    public void deflaterHasExpectedCompatibilityForCurrentAndroidVersion() {
        Assert.assertEquals(
            new DefaultDeflateCompatibilityWindow(DefaultDeflater::new).isCompatible(),
            Build.VERSION.SDK_INT < 30
        );
    }

    @Test
    public void androidDeflaterIsCompatible() {
        Assert.assertTrue(
            new DefaultDeflateCompatibilityWindow(AndroidDeflater::new).isCompatible()
        );
    }

    @Test
    public void patchYieldsExpectedResult() throws IOException {
        File base = File.createTempFile("base", ".zip");
        copy(getClass().getResourceAsStream("/base.zip"), base);
        ByteArrayOutputStream result = new ByteArrayOutputStream();

        new AndroidFileByFileV1DeltaApplier()
            .applyDelta(
                base,
                getClass().getResourceAsStream("/base-to-expected.patch"),
                result
            );

        Assert.assertArrayEquals(
            readAllBytes(Objects.requireNonNull(getClass().getResourceAsStream("/expected.zip"))),
            result.toByteArray()
        );
    }

    private byte[] readAllBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        copy(inputStream, buffer);
        return buffer.toByteArray();
    }

    private void copy(InputStream inputStream, File outFile) throws IOException {
        copy(inputStream, new FileOutputStream(outFile));
    }

    private void copy(InputStream inputStream, OutputStream outputStream) throws IOException {
        try {
            try {
                int len;
                byte[] data = new byte[16384];

                while ((len = inputStream.read(data, 0, data.length)) != -1)
                    outputStream.write(data, 0, len);
            } finally {
                inputStream.close();
            }
        } finally {
            outputStream.close();
        }
    }
}
