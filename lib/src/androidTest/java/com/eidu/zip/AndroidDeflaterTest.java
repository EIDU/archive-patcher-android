package com.eidu.zip;

import org.junit.Assert;
import org.junit.Test;

import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public class AndroidDeflaterTest {
    private final byte[] BYTES = {
            0x00, 0x00, 0x00, 0x00,
            0x01, 0x01, 0x01, 0x01,
            0x00, 0x00, 0x00, 0x00,
            0x01, 0x01, 0x01, 0x01,
            0x00, 0x00, 0x00, 0x00,
            0x01, 0x01, 0x01, 0x01,
            0x00, 0x00, 0x00, 0x00,
            0x01, 0x01, 0x01, 0x01,
    };

    @Test
    public void customDeflaterDeflates() throws DataFormatException {
        AndroidDeflater deflater = new AndroidDeflater();
        deflater.setInput(BYTES);
        deflater.finish();
        byte[] deflated = new byte[31];
        int deflatedLength = deflater.deflate(deflated);
        deflater.end();

        Assert.assertEquals(17, deflatedLength);

        Inflater inflater = new Inflater();
        inflater.setInput(deflated, 0, deflatedLength);
        byte[] inflated = new byte[BYTES.length];
        int inflatedLength = inflater.inflate(inflated);
        inflater.end();

        Assert.assertEquals(BYTES.length, inflatedLength);
        Assert.assertArrayEquals(BYTES, inflated);
    }
}
