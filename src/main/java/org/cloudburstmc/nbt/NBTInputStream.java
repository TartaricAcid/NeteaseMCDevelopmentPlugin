package org.cloudburstmc.nbt;

import org.cloudburstmc.nbt.util.stream.LimitedDataInput;

import java.io.Closeable;
import java.io.DataInput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

import static org.cloudburstmc.nbt.NbtUtils.MAX_DEPTH;
import static org.cloudburstmc.nbt.NbtUtils.MAX_READ_SIZE;

public class NBTInputStream implements Closeable {
    private final DataInput input;
    private final boolean internKeys;
    private final boolean internValues;
    private boolean closed = false;

    public NBTInputStream(DataInput input, boolean internKeys, boolean internValues) {
        this(input, internKeys, internValues, MAX_READ_SIZE);
    }

    public NBTInputStream(DataInput input, boolean internKeys, boolean internValues, long maxReadSize) {
        Objects.requireNonNull(input, "input");
        if (input instanceof LimitedDataInput) {
            this.input = input;
        } else {
            this.input = new LimitedDataInput(input, maxReadSize);
        }
        this.internKeys = internKeys;
        this.internValues = internValues;
    }

    public NBTInputStream(DataInput input) {
        this(input, false, false);
    }

    public NBTInputStream(DataInput input, long maxReadSize) {
        this(input, false, false, maxReadSize);
    }

    public Object readTag() throws IOException {
        return readTag(MAX_DEPTH);
    }

    public Object readTag(int maxDepth) throws IOException {
        if (closed) {
            throw new IllegalStateException("Trying to read from a closed reader!");
        }
        int typeId = input.readUnsignedByte();
        NbtType<?> type = NbtType.byId(typeId);
        input.readUTF(); // Root tag name

        return deserialize(type, maxDepth);
    }

    public <T> T readValue(NbtType<T> type) throws IOException {
        return readValue(type, MAX_DEPTH);
    }

    @SuppressWarnings("unchecked")
    public <T> T readValue(NbtType<T> type, int maxDepth) throws IOException {
        if (closed) {
            throw new IllegalStateException("Trying to read from a closed reader!");
        }

        return (T) deserialize(type, maxDepth);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object deserialize(NbtType<?> type, int maxDepth) throws IOException {
        if (maxDepth < 0) {
            throw new IllegalArgumentException("NBT compound is too deeply nested");
        }

        switch (type.getEnum()) {
            case END:
                return null;
            case BYTE:
                return input.readByte();
            case SHORT:
                return input.readShort();
            case INT:
                if (this.internValues) {
                    return IntegerInternPool.intern(input.readInt());
                } else {
                    return input.readInt();
                }
            case LONG:
                return input.readLong();
            case FLOAT:
                if (this.internValues) {
                    return FloatInternPool.intern(input.readFloat());
                } else {
                    return input.readFloat();
                }
            case DOUBLE:
                return input.readDouble();
            case BYTE_ARRAY:
                int arraySize = input.readInt();
                byte[] bytes = new byte[arraySize];
                input.readFully(bytes);
                return bytes;
            case STRING:
                if (this.internValues) {
                    return input.readUTF().intern();
                } else {
                    return input.readUTF();
                }
            case COMPOUND:
                LinkedHashMap<String, Object> map = new LinkedHashMap<>();
                NbtType<?> nbtType;
                while ((nbtType = NbtType.byId(input.readUnsignedByte())) != NbtType.END) {
                    String name;
                    if (this.internKeys) {
                        name = input.readUTF().intern();
                    } else {
                        name = input.readUTF();
                    }
                    map.put(name, deserialize(nbtType, maxDepth - 1));
                }
                return new NbtMap(map);
            case LIST:
                int typeId = input.readUnsignedByte();
                NbtType<?> listType = NbtType.byId(typeId);
                List<? super Object> list = new ArrayList<>();
                int listLength = input.readInt();
                for (int i = 0; i < listLength; i++) {
                    list.add(deserialize(listType, maxDepth - 1));
                }
                return new NbtList(listType, list);
            case INT_ARRAY:
                arraySize = input.readInt();
                int[] ints = new int[arraySize];
                for (int i = 0; i < arraySize; i++) {
                    ints[i] = input.readInt();
                }
                return ints;
            case LONG_ARRAY:
                arraySize = input.readInt();
                long[] longs = new long[arraySize];
                for (int i = 0; i < arraySize; i++) {
                    longs[i] = input.readLong();
                }
                return longs;
        }

        throw new IllegalArgumentException("Unknown type " + type);
    }

    @Override
    public void close() throws IOException {
        if (closed) return;
        closed = true;
        if (input instanceof Closeable) {
            ((Closeable) input).close();
        }
    }
}
