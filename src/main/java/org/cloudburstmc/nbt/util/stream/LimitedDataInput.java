package org.cloudburstmc.nbt.util.stream;

import org.cloudburstmc.nbt.NbtUtils;

import java.io.Closeable;
import java.io.DataInput;
import java.io.IOException;

public class LimitedDataInput implements DataInput, Closeable {

    private final DataInput delegate;
    private final long maxReadSize;

    private long readSize = 0;

    public LimitedDataInput(DataInput delegate) {
        this(delegate, NbtUtils.MAX_READ_SIZE);
    }

    public LimitedDataInput(DataInput delegate, long maxReadSize) {
        this.delegate = delegate;
        this.maxReadSize = maxReadSize;
    }

    @Override
    public void close() throws IOException {
        if (delegate instanceof Closeable) {
            ((Closeable) delegate).close();
        }
    }

    protected void tryRead(int size) throws IOException {
        this.readSize += size;
        if (this.maxReadSize > 0 && this.readSize > this.maxReadSize) {
            throw new IOException("Read size exceeded limit: read=" + this.readSize + ", limit=" + this.maxReadSize);
        }
    }

    @Override
    public void readFully(byte[] b) throws IOException {
        this.tryRead(b.length);
        this.delegate.readFully(b);
    }

    @Override
    public void readFully(byte[] b, int off, int len) throws IOException {
        this.tryRead(len);
        this.delegate.readFully(b, off, len);
    }

    @Override
    public int skipBytes(int n) throws IOException {
        this.tryRead(n);
        return this.delegate.skipBytes(n);
    }

    @Override
    public boolean readBoolean() throws IOException {
        this.tryRead(1);
        return this.delegate.readBoolean();
    }

    @Override
    public byte readByte() throws IOException {
        this.tryRead(1);
        return this.delegate.readByte();
    }

    @Override
    public int readUnsignedByte() throws IOException {
        this.tryRead(1);
        return this.delegate.readUnsignedByte();
    }

    @Override
    public short readShort() throws IOException {
        this.tryRead(2);
        return this.delegate.readShort();
    }

    @Override
    public int readUnsignedShort() throws IOException {
        this.tryRead(2);
        return this.delegate.readUnsignedShort();
    }

    @Override
    public char readChar() throws IOException {
        this.tryRead(2);
        return this.delegate.readChar();
    }

    @Override
    public int readInt() throws IOException {
        this.tryRead(4);
        return this.delegate.readInt();
    }

    @Override
    public long readLong() throws IOException {
        this.tryRead(8);
        return this.delegate.readLong();
    }

    @Override
    public float readFloat() throws IOException {
        this.tryRead(4);
        return this.delegate.readFloat();
    }

    @Override
    public double readDouble() throws IOException {
        this.tryRead(8);
        return this.delegate.readDouble();
    }

    @Override
    public String readLine() throws IOException {
        String line =  this.delegate.readLine();
        this.tryRead(line.length()); // not ideal, but we do not have a better way to estimate the size
        return line;
    }

    @Override
    public String readUTF() throws IOException {
        String utf = this.delegate.readUTF();
        this.tryRead(utf.length()); // not ideal, but we do not have a better way to estimate the size
        return utf;
    }

    public DataInput delegate() {
        return this.delegate;
    }
}
