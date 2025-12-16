package com.github.tartaricacid.mcshelper.util

import com.intellij.execution.ExecutionException
import org.cloudburstmc.nbt.NbtUtils
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Path

@Suppress("DialogTitleCapitalization")
class LevelDataUtils {
    companion object {
        /**
         * 将打包的默认 level.dat 复制到目标世界文件夹。成功返回 true。
         */
        @Throws(ExecutionException::class)
        fun createDefaultLevelData(worldFolder: Path) {
            val stream = object {}.javaClass.classLoader.getResourceAsStream("data/level.dat")
            if (stream == null) {
                throw ExecutionException("默认 level.dat 资源文件未找到")
            }
            val targetPath = worldFolder.resolve("level.dat")
            try {
                stream.use { input ->
                    targetPath.toFile().outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: IOException) {
                throw ExecutionException("创建默认 level.dat 文件失败：${e.message}", e)
            }
        }

        /**
         * 创建一个 NBT 读取器，该读取器定位在 Bedrock level.dat 的 NBT 载荷开头。
         * 此方法不会关闭提供的流。它会读取越过 8 字节头部（版本 + 大小），并返回的读取的信息
         */
        @Throws(IOException::class)
        fun readNbt(stream: InputStream): TagInfo {
            val dis = DataInputStream(stream)
            val header = ByteArray(8)
            dis.readFully(header) // 如果不足 8 字节会抛出 IOException

            val version = ByteBuffer.wrap(header, 0, 4)
                .order(ByteOrder.LITTLE_ENDIAN).int
            val size = ByteBuffer.wrap(header, 4, 4)
                .order(ByteOrder.LITTLE_ENDIAN).int

            val nbtTag = NbtUtils.createReaderLE(stream).readTag()
            return TagInfo(version, size, nbtTag)
        }

        /**
         * 使用 8 字节的 Bedrock 头（版本 + 大小，均为小端）写入 NBT 载荷。
         * writer lambda 将接收一个小端的 NBTOutputStream 来写入 NBT 载荷。
         * 此方法不会关闭提供的流。（仅会刷新它）。
         */
        @Throws(IOException::class)
        fun writerNbt(stream: OutputStream, tag: Any, version: Int = 10) {
            // 首先将 NBT 载荷写入临时缓冲区，以便我们计算其大小。
            val byteStream = ByteArrayOutputStream()
            NbtUtils.createWriterLE(byteStream).use { nbtOut ->
                nbtOut.writeTag(tag)
            }

            val nbtBytes = byteStream.toByteArray()
            val size = nbtBytes.size

            // 准备 8 字节头：版本（小端 int）+ 大小（小端 int）
            val header = ByteBuffer.allocate(8)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putInt(version)
                .putInt(size)
                .array()

            // 将头和 NBT 载荷写入提供的流
            stream.write(header)
            stream.write(nbtBytes)
            stream.flush()
        }
    }

    data class TagInfo(
        val version: Int,
        val size: Int,
        val tag: Any
    )
}
