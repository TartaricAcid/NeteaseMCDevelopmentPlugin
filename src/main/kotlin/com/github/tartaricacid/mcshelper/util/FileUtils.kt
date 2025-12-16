package com.github.tartaricacid.mcshelper.util

import com.intellij.execution.ExecutionException
import java.io.File
import java.net.URI
import java.net.URL
import java.nio.file.*
import javax.swing.filechooser.FileSystemView


class FileUtils {
    companion object {
        /**
         * 寻找开发启动器的可执行文件路径
         */
        fun findMinecraftExecutables(): List<String> {
            val paths = mutableListOf<String>()
            val fsv = FileSystemView.getFileSystemView()
            File.listRoots().filter { root ->
                !fsv.isFloppyDrive(root) && root.totalSpace > 0
            }.forEach { drive ->
                val basePath = File("${drive}MCStudioDownload\\game\\MinecraftPE_Netease")
                if (basePath.exists() && basePath.isDirectory) {
                    // 遍历版本目录
                    basePath.listFiles()?.forEach { versionDir ->
                        if (versionDir.isDirectory) {
                            val exePath = File(versionDir, "Minecraft.Windows.exe")
                            if (exePath.exists() && exePath.isFile) {
                                paths.add(exePath.absolutePath)
                            }
                        }
                    }
                }
            }
            return paths
        }

        /**
         * 清理行为包目录下所有符号链接
         */
        fun removeSymlinks(dir: Path?) {
            val removeDir = dir ?: return
            Files.list(removeDir).use { paths ->
                paths.forEach { path ->
                    if (Files.isSymbolicLink(path)) {
                        Files.deleteIfExists(path)
                    }
                }
            }
        }

        /**
         * 创建符号链接
         */
        fun createSymlink(target: Path, link: Path) {
            try {
                Files.createSymbolicLink(link, target)
            } catch (e: Exception) {
                // FIXME: Windows 下可能会因为权限问题创建失败，应该弹出 idea 弹窗提示用户
                e.printStackTrace()
            }
        }

        /**
         * 将 jar 下指定目录下的所有文件解压（强制覆盖）到目标目录
         */
        @Throws(ExecutionException::class)
        fun extractResourceDir(sourcePath: String, targetPath: Path) {
            val url: URL? = FileUtils::class.java.classLoader.getResource(sourcePath)
            if (url == null) {
                return
            }
            val uri: URI = url.toURI()

            // 检查是否为 JAR 文件系统，如果是则需要创建文件系统
            val sourceFolderPath: Path = if (uri.scheme == "jar") {
                // 对于 JAR 文件，需要使用 FileSystems.newFileSystem 来创建文件系统
                val parts = uri.toString().split("!")
                if (parts.size >= 2) {
                    val jarUri = URI.create(parts[0])
                    val fs = try {
                        FileSystems.getFileSystem(jarUri)
                    } catch (_: FileSystemNotFoundException) {
                        FileSystems.newFileSystem(jarUri, emptyMap<String, Any>())
                    }
                    fs.getPath(parts[1])
                } else {
                    Paths.get(uri)
                }
            } else {
                Paths.get(uri)
            }

            Files.walk(sourceFolderPath, Int.Companion.MAX_VALUE).use { stream ->
                stream.forEach { source ->
                    // 使用相对路径计算，避免 URI 转换产生的非法字符
                    val relativePath = sourceFolderPath.relativize(source)
                    // 将 JAR 文件系统的相对路径转换为字符串，然后让目标文件系统重新解析
                    // 这样避免了不同文件系统 Provider 之间的冲突
                    val relativePathString = relativePath.toString().replace('\\', '/')
                    val target = targetPath.resolve(relativePathString)
                    try {
                        if (Files.isDirectory(source)) {
                            Files.createDirectories(target)
                        } else {
                            // 确保目标目录存在
                            val parentDir = target.parent
                            if (parentDir != null && !Files.isDirectory(parentDir)) {
                                Files.createDirectories(parentDir)
                            }
                            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING)
                        }
                    } catch (e: Exception) {
                        throw ExecutionException("从插件中复制文件夹失败：${e.message}", e)
                    }
                }
            }
        }
    }
}