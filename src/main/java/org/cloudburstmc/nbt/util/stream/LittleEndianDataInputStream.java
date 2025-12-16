package org.cloudburstmc.nbt.util.stream;

import org.jetbrains.annotations.NotNull;
import org.cloudburstmc.nbt.NbtUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class LittleEndianDataInputStream extends LimitedDataInput {

    public LittleEndianDataInputStream(InputStream stream) {
        this(stream, NbtUtils.MAX_READ_SIZE);
    }

    public LittleEndianDataInputStream(InputStream stream, long maxReadSize) {
        super(new DataInputStream(stream), maxReadSize);
    }

    public LittleEndianDataInputStream(DataInputStream stream) {
        this(stream, NbtUtils.MAX_READ_SIZE);
    }

    public LittleEndianDataInputStream(DataInputStream stream, long maxReadSize) {
        super(stream, maxReadSize);
    }

    @Override
    public short readShort() throws IOException {
        this.tryRead(2);
        return Short.reverseBytes(this.delegate().readShort());
    }

    @Override
    public int readUnsignedShort() throws IOException {
        this.tryRead(2);
        return Short.toUnsignedInt(Short.reverseBytes(this.delegate().readShort()));
    }

    @Override
    public char readChar() throws IOException {
        this.tryRead(2);
        return Character.reverseBytes(this.delegate().readChar());
    }

    @Override
    public int readInt() throws IOException {
        this.tryRead(4);
        return Integer.reverseBytes(this.delegate().readInt());
    }

    @Override
    public long readLong() throws IOException {
        this.tryRead(8);
        return Long.reverseBytes(this.delegate().readLong());
    }

    @Override
    public float readFloat() throws IOException {
        this.tryRead(4);
        return Float.intBitsToFloat(Integer.reverseBytes(this.delegate().readInt()));
    }

    @Override
    public double readDouble() throws IOException {
        this.tryRead(8);
        return Double.longBitsToDouble(Long.reverseBytes(this.delegate().readLong()));
    }

    @NotNull
    @Override
    public String readUTF() throws IOException {
        int size = this.readUnsignedShort();
        this.tryRead(size + 4); // do it here, so we do not preallocate the buffer

        byte[] bytes = new byte[size];
        this.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
