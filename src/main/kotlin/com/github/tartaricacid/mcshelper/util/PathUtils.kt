package com.github.tartaricacid.mcshelper.util

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PathUtils {
    companion object {
        /**
         * 获取 Windows 下的 AppData 目录
         */
        fun appDataDir(): Path? {
            val env = System.getenv("APPDATA")
            if (!env.isNullOrBlank()) {
                val p = Paths.get(env)
                if (Files.isDirectory(p)) {
                    return p
                }
            }

            val os = System.getProperty("os.name", "").lowercase()
            if (os.startsWith("windows")) {
                val home = System.getProperty("user.home", "")
                if (home.isNotBlank()) {
                    val fallback = Paths.get(home, "AppData", "Roaming")
                    if (Files.isDirectory(fallback)) {
                        return fallback
                    }
                }
            }

            return null
        }

        /**
         * 获取网易 Minecraft 的游戏目录
         */
        fun mcDir(): Path? {
            val appData = appDataDir() ?: return null
            val mcDir = appData.resolve("MinecraftPE_Netease")
            return if (Files.isDirectory(mcDir)) {
                mcDir
            } else {
                null
            }
        }

        /**
         * 获取网易 games/com.netease 目录，此目录存储了指向组件的符号链接
         */
        fun gameDir(): Path? {
            val mcDir = mcDir() ?: return null
            val gameDir = mcDir.resolve("games").resolve("com.netease")
            return if (Files.isDirectory(gameDir)) {
                gameDir
            } else {
                null
            }
        }

        /**
         * 获取网易 Minecraft 的开发测试的存档目录
         */
        fun worldsDir(): Path? {
            val mcDir = mcDir() ?: return null
            val worldsDir = mcDir.resolve("minecraftWorlds")
            return if (Files.isDirectory(worldsDir)) {
                worldsDir
            } else {
                null
            }
        }

        /**
         * 获取网易 Minecraft 开发组件的行为包目录
         */
        fun behaviorPacksDir(): Path? {
            val gameDir = gameDir() ?: return null
            val bpDir = gameDir.resolve("behavior_packs")
            return if (Files.isDirectory(bpDir)) {
                bpDir
            } else {
                null
            }
        }

        /**
         * 获取网易 Minecraft 开发组件的资源包目录
         */
        fun resourcePacksDir(): Path? {
            val gameDir = gameDir() ?: return null
            val rpDir = gameDir.resolve("resource_packs")
            return if (Files.isDirectory(rpDir)) {
                rpDir
            } else {
                null
            }
        }
    }
}
