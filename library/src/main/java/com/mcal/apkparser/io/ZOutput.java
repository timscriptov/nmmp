package com.mcal.apkparser.io;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;

public final class ZOutput {
    private final OutputStream dos;
    private int written;

    public ZOutput(OutputStream out) {
        written = 0;
        dos = out;
    }

    public void writeShort(short s) throws IOException {
        dos.write(s & 0xFF);
        written++;
        dos.write(s >>> 8 & 0xFF);
        written++;
    }

    public void close() throws IOException {
        dos.close();
    }

    public int size() {
        return written;
    }

    public void writeChar(char c) throws IOException {
        dos.write(c & 0xFF);
        written++;
        dos.write(c >>> 8 & 0xFF);
        written++;
    }

    public void writeCharArray(char @NotNull [] c) throws IOException {
        for (char element : c)
            writeChar(element);
    }

    public void write(int i) throws IOException {
        dos.write(i);
        written++;
    }

    public void writeByte(int b) throws IOException {
        dos.write(b);
        written++;
    }

    public void writeFully(byte[] b) throws IOException {
        dos.write(b, 0, b.length);
        written += b.length;
    }

    public void writeFully(byte[] b, int a, int len) throws IOException {
        dos.write(b, a, len);
        written += len;
    }

    public void writeInt(int i) throws IOException {
        dos.write(i & 0xFF);
        written++;
        dos.write(i >>> 8 & 0xFF);
        written++;
        dos.write(i >>> 16 & 0xFF);
        written++;
        dos.write(i >>> 24 & 0xFF);
        written++;
    }

    public void writeIntArray(int[] buf, int s, int end) throws IOException {
        for (; s < end; s++) {
            writeInt(buf[s]);
        }
    }

    public void writeIntArray(int[] buf) throws IOException {
        writeIntArray(buf, 0, buf.length);
    }

    public void writeNulEndedString(@NotNull String string, int length, boolean fixed) throws IOException {
        char[] ch = string.toCharArray();
        int j = 0;
        while (j < ch.length && length != 0) {
            writeChar(ch[j++]);
            length--;
        }
        if (fixed) {
            for (int i = 0; i < length * 2; i++) {
                dos.write(0);
                written++;
            }
        }
    }
}
