package org.cloudburstmc.nbt.util;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class VarInts {
    private VarInts() {
    }

    public static void writeInt(DataOutput buffer, int value) throws IOException {
        encode(buffer, ((value << 1) ^ (value >> 31)) & 0xFFFFFFFFL);
    }

    public static int readInt(DataInput buffer) throws IOException {
        int n = (int) decode(buffer, 32);
        return (n >>> 1) ^ -(n & 1);
    }

    public static void writeUnsignedInt(DataOutput buffer, long value) throws IOException {
        encode(buffer, value & 0xFFFFFFFFL);
    }

    public static int readUnsignedInt(DataInput buffer) throws IOException {
        return (int) decode(buffer, 32);
    }

    public static void writeLong(DataOutput buffer, long value) throws IOException {
        encode(buffer, (value << 1) ^ (value >> 63));
    }

    public static long readLong(DataInput buffer) throws IOException {
        long n = decode(buffer, 64);
        return (n >>> 1) ^ -(n & 1);
    }

    public static void writeUnsignedLong(DataOutput buffer, long value) throws IOException {
        encode(buffer, value);
    }

    public static long readUnsignedLong(DataInput buffer) throws IOException {
        return decode(buffer, 64);
    }

    // Based off of Andrew Steinborn's blog post:
    // https://steinborn.me/posts/performance/how-fast-can-you-write-a-varint/
    private static void encode(DataOutput out, long value) throws IOException {
        // Peel the one and two byte count cases explicitly as they are the most common VarInt sizes
        // that the server will write, to improve inlining.
        if ((value & ~0x7FL) == 0) {
            out.writeByte((byte) value);
        } else if ((value & ~0x3FFFL) == 0) {
            byte[] bytes = {
                    (byte) (value & 0x7FL | 0x80L),
                    (byte) (value >>> 7)
            };
            out.write(bytes);
        } else {
            encodeFull(out, value);
        }
    }

    @SuppressWarnings({"DuplicatedCode"})
    private static void encodeFull(DataOutput out, long value) throws IOException {
        byte[] bytes;
        if ((value & ~0x1FFFFFL) == 0) {
            bytes = new byte[] {
                    (byte) (value & 0x7FL | 0x80L),
                    (byte) ((value >>> 7) & 0x7FL | 0x80L),
                    (byte) (value >>> 14)
            };
        } else if ((value & ~0xFFFFFFFL) == 0) {
            bytes = new byte[] {
                    (byte) (value & 0x7FL | 0x80L),
                    (byte) ((value >>> 7) & 0x7FL | 0x80L),
                    (byte) ((value >>> 14) & 0x7FL | 0x80L),
                    (byte) (value >>> 21)
            };
        } else if ((value & ~0x7FFFFFFFFL) == 0) {
            bytes = new byte[] {
                    (byte) (value & 0x7FL | 0x80L),
                    (byte) ((value >>> 7) & 0x7FL | 0x80L),
                    (byte) ((value >>> 14) & 0x7FL | 0x80L),
                    (byte) ((value >>> 21) & 0x7FL | 0x80L),
                    (byte) (value >>> 28)
            };
        } else if ((value & ~0x3FFFFFFFFFFL) == 0) {
            bytes = new byte[] {
                    (byte) (value & 0x7FL | 0x80L),
                    (byte) ((value >>> 7) & 0x7FL | 0x80L),
                    (byte) ((value >>> 14) & 0x7FL | 0x80L),
                    (byte) ((value >>> 21) & 0x7FL | 0x80L),
                    (byte) ((value >>> 28) & 0x7FL | 0x80L),
                    (byte) (value >>> 35)
            };
        } else if ((value & ~0x1FFFFFFFFFFFFL) == 0) {
            bytes = new byte[] {
                    (byte) (value & 0x7FL | 0x80L),
                    (byte) ((value >>> 7) & 0x7FL | 0x80L),
                    (byte) ((value >>> 14) & 0x7FL | 0x80L),
                    (byte) ((value >>> 21) & 0x7FL | 0x80L),
                    (byte) ((value >>> 28) & 0x7FL | 0x80L),
                    (byte) ((value >>> 35) & 0x7FL | 0x80L),
                    (byte) (value >>> 42)
            };
        } else if ((value & ~0xFFFFFFFFFFFFFFL) == 0) {
            bytes = new byte[] {
                    (byte) (value & 0x7FL | 0x80L),
                    (byte) ((value >>> 7) & 0x7FL | 0x80L),
                    (byte) ((value >>> 14) & 0x7FL | 0x80L),
                    (byte) ((value >>> 21) & 0x7FL | 0x80L),
                    (byte) ((value >>> 28) & 0x7FL | 0x80L),
                    (byte) ((value >>> 35) & 0x7FL | 0x80L),
                    (byte) ((value >>> 42) & 0x7FL | 0x80L),
                    (byte) (value >>> 49)
            };
        } else if ((value & ~0x7FFFFFFFFFFFFFFFL) == 0) {
            bytes = new byte[] {
                    (byte) (value & 0x7FL | 0x80L),
                    (byte) ((value >>> 7) & 0x7FL | 0x80L),
                    (byte) ((value >>> 14) & 0x7FL | 0x80L),
                    (byte) ((value >>> 21) & 0x7FL | 0x80L),
                    (byte) ((value >>> 28) & 0x7FL | 0x80L),
                    (byte) ((value >>> 35) & 0x7FL | 0x80L),
                    (byte) ((value >>> 42) & 0x7FL | 0x80L),
                    (byte) ((value >>> 49) & 0x7FL | 0x80L),
                    (byte) (value >>> 56)
            };
        } else {
            bytes = new byte[] {
                    (byte) (value & 0x7FL | 0x80L),
                    (byte) ((value >>> 7) & 0x7FL | 0x80L),
                    (byte) ((value >>> 14) & 0x7FL | 0x80L),
                    (byte) ((value >>> 21) & 0x7FL | 0x80L),
                    (byte) ((value >>> 28) & 0x7FL | 0x80L),
                    (byte) ((value >>> 35) & 0x7FL | 0x80L),
                    (byte) ((value >>> 42) & 0x7FL | 0x80L),
                    (byte) ((value >>> 49) & 0x7FL | 0x80L),
                    (byte) ((value >>> 56) & 0x7FL | 0x80L),
                    (byte) (value >>> 63)
            };
        }
        out.write(bytes);
    }

    private static long decode(DataInput buffer, int maxBits) throws IOException {
        long result = 0;
        for (int shift = 0; shift < maxBits; shift += 7) {
            final byte b = buffer.readByte();
            result |= (b & 0x7FL) << shift;
            if ((b & 0x80) == 0) {
                return result;
            }
        }
        throw new ArithmeticException("VarInt was too large");
    }
}
