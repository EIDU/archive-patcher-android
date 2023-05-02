package com.eidu.archivepatcher;

import com.eidu.zip.AndroidDeflater;
import com.google.archivepatcher.applier.DeltaApplier;
import com.google.archivepatcher.applier.FileByFileV1DeltaApplier;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class AndroidFileByFileV1DeltaApplier implements DeltaApplier {
    private final FileByFileV1DeltaApplier applier;

    public AndroidFileByFileV1DeltaApplier() {
        this(null);
    }

    public AndroidFileByFileV1DeltaApplier(File tempDir) {
        applier = new FileByFileV1DeltaApplier(tempDir, AndroidDeflater::new);
    }

    @Override
    public void applyDelta(File oldBlob, InputStream deltaIn, OutputStream newBlobOut) throws IOException {
        applier.applyDelta(oldBlob, deltaIn, newBlobOut);
    }
}
