package com.mcal.apkparser.xml;

import com.mcal.apkparser.io.ZInput;
import com.mcal.apkparser.io.ZOutput;
import com.mcal.apkparser.util.StringDecoder;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class AXmlDecoder {
    private static final int AXML_CHUNK_TYPE = 0x00080003;
    private final ZInput mIn;
    public StringDecoder mTableStrings;
    byte[] data;

    private AXmlDecoder(ZInput in) {
        this.mIn = in;
    }

    public static @NotNull AXmlDecoder decode(InputStream input) throws IOException {
        final AXmlDecoder axml = new AXmlDecoder(new ZInput(input));
        axml.readStrings();
        return axml;
    }

    private void readStrings() throws IOException {
        final int type = mIn.readInt();
        checkChunk(type, AXML_CHUNK_TYPE);
        mIn.readInt();// Chunk size
        mTableStrings = StringDecoder.read(this.mIn);

        final ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        final byte[] buf = new byte[2048];
        int num;
        while ((num = mIn.read(buf, 0, 2048)) != -1) {
            byteOut.write(buf, 0, num);
        }
        data = byteOut.toByteArray();
        mIn.close();
        byteOut.close();
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public void write(List<String> list, OutputStream out) throws IOException {
        write(list, new ZOutput(out));
    }

    public void write(@NotNull List<String> list, @NotNull ZOutput out) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final ZOutput buf = new ZOutput(baos);
        final String[] array = new String[list.size()];
        list.toArray(array);
        mTableStrings.write(array, buf);
        buf.writeFully(data);
        // write out
        out.writeInt(AXML_CHUNK_TYPE);
        out.writeInt(baos.size() + 8);
        out.writeFully(baos.toByteArray());
        buf.close();
    }

    public void write(@NotNull ZOutput out) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final ZOutput buf = new ZOutput(baos);
        mTableStrings.write(buf);
        buf.writeFully(data);
        // write out
        out.writeInt(AXML_CHUNK_TYPE);
        out.writeInt(baos.size() + 8);
        out.writeFully(baos.toByteArray());
        baos.reset();
        buf.close();
    }

    public byte[] encode() throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final ZOutput buf = new ZOutput(baos);
        mTableStrings.write(buf);
        buf.writeFully(data);

        final byte[] bytes = baos.toByteArray();
        baos.reset();
        // write out
        buf.writeInt(AXML_CHUNK_TYPE);
        buf.writeInt(bytes.length + 8);
        buf.writeFully(bytes);
        return baos.toByteArray();
    }


    private void checkChunk(int type, int expectedType) throws IOException {
        if (type != expectedType) {
            throw new IOException(String.format("Invalid chunk type: expected=0x%08x, got=0x%08x", expectedType, type));
        }
    }
}
