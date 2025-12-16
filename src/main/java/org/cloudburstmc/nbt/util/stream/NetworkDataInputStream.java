package org.cloudburstmc.nbt.util.stream;

import org.jetbrains.annotations.NotNull;
import org.cloudburstmc.nbt.NbtUtils;
import org.cloudburstmc.nbt.util.VarInts;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class NetworkDataInputStream extends LittleEndianDataInputStream {

    public NetworkDataInputStream(InputStream stream) {
        this(stream, NbtUtils.MAX_READ_SIZE);
    }

    public NetworkDataInputStream(InputStream stream, long maxReadSize) {
        super(new DataInputStream(stream), maxReadSize);
    }

    public NetworkDataInputStream(DataInputStream stream) {
        this(stream, NbtUtils.MAX_READ_SIZE);
    }

    public NetworkDataInputStream(DataInputStream stream, long maxReadSize) {
        super(stream, maxReadSize);
    }

    @Override
    public int readInt() throws IOException {
        this.tryRead(4); // assume that the int is 4 bytes
        return VarInts.readInt(this.delegate());
    }

    @Override
    public long readLong() throws IOException {
        this.tryRead(8); // assume that the long is 8 bytes
        return VarInts.readLong(this.delegate());
    }

    @Override
    @NotNull
    public String readUTF() throws IOException {
        int length = VarInts.readUnsignedInt(this.delegate());
        this.tryRead(length + 4);

        byte[] bytes = new byte[length];
        readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
