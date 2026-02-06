package com.github.tartaricacid.mcshelper.run

import com.github.tartaricacid.mcshelper.options.MCRunConfigurationOptions
import com.github.tartaricacid.mcshelper.util.FileUtils
import com.github.tartaricacid.mcshelper.util.LevelDataUtils
import com.github.tartaricacid.mcshelper.util.PackUtils
import com.github.tartaricacid.mcshelper.util.PackUtils.PackInfo
import com.github.tartaricacid.mcshelper.util.PackUtils.PackType
import com.github.tartaricacid.mcshelper.util.PathUtils
import com.google.common.collect.Lists
import com.google.common.collect.Maps
import com.google.gson.Gson
import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.project.Project
import org.cloudburstmc.nbt.NbtMap
import org.cloudburstmc.nbt.NbtMapBuilder
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import kotlin.io.path.absolutePathString
import kotlin.io.path.extension

const val TEST_MOD_DIR_NAME = "183b0dc7-ae4a-48fb-800d-9d68f7162e7e"

@Suppress("DialogTitleCapitalization")
class ConfigRunTask {
    companion object {
        @Throws(ExecutionException::class)
        fun run(project: Project, config: MCRunConfigurationOptions): GeneralCommandLine {
            // 检查启动器路径
            val gamePath = Paths.get(config.gameExecutablePath ?: "")
            if (!Files.isRegularFile(gamePath) || gamePath.fileName.extension != "exe") {
                throw ExecutionException("启动程序路径错误：${config.gameExecutablePath}")
            }

            // 清空符号链接
            FileUtils.removeSymlinks(PathUtils.behaviorPacksDir())
            FileUtils.removeSymlinks(PathUtils.resourcePacksDir())

            // 先把当前项目加入
            val packMaps: EnumMap<PackType, MutableList<PackInfo>> = Maps.newEnumMap(PackType::class.java)
            val projectPath = project.basePath
            if (projectPath == null) {
                throw ExecutionException("当前项目路径为空")
            }
            PackUtils.parsePack(Paths.get(projectPath), packMaps)

            // 再把额外添加的包加入
            for (extraPackPath in config.includedModDirs) {
                val extraPath = Paths.get(extraPackPath)
                PackUtils.parsePack(extraPath, packMaps)
            }

            // 创建符号链接
            val sudoRequiredSymlinks = mutableSetOf<Pair<Path, Path>>()
            for ((type, packList) in packMaps) {
                val targetDir = when (type) {
                    PackType.BEHAVIOR -> PathUtils.behaviorPacksDir()
                    PackType.RESOURCE -> PathUtils.resourcePacksDir()
                }
                if (targetDir == null) {
                    continue
                }
                for (pack in packList) {
                    val target = pack.path
                    val link = targetDir.resolve(pack.uuid)
                    if (!FileUtils.createSymlink(target, link)) {
                        sudoRequiredSymlinks += target to link
                    }
                }
            }

            // 如有需提权的符号链接，则合并提权创建
            if (sudoRequiredSymlinks.isNotEmpty()) {
                FileUtils.sudoCreateSymlinks(sudoRequiredSymlinks.toList())
            }

            // 解压（强制覆盖）测试模组
            val behPackDir = PathUtils.behaviorPacksDir()
            if (behPackDir != null) {
                val testModSource = "data/include_test_mod"
                val testModTarget = behPackDir.resolve(TEST_MOD_DIR_NAME)
                FileUtils.extractResourceDir(testModSource, testModTarget)
                PackUtils.parsePack(testModTarget, packMaps)
            }

            // 读取或新建存档
            val worldDir = PathUtils.worldsDir()
            if (worldDir == null) {
                throw ExecutionException("存档文件夹路径获取失败")
            }
            val worldFolderPath = worldDir.resolve(config.worldFolderName)
            if (!Files.isDirectory(worldFolderPath)) {
                Files.createDirectories(worldFolderPath)
            }

            // 检查 level.dat 是否存在，不存在则创建一个默认的
            val levelDatPath = worldFolderPath.resolve("level.dat")
            if (!Files.isRegularFile(levelDatPath)) {
                LevelDataUtils.createDefaultLevelData(worldFolderPath)
            }

            // 读取 level.dat 内容
            val tagInfo = try {
                Files.newInputStream(levelDatPath).use { input ->
                    LevelDataUtils.readNbt(input)
                }
            } catch (e: Exception) {
                throw ExecutionException("读取 $levelDatPath 文件失败：${e.message}", e)
            }

            // 检查
            val tag = tagInfo.tag
            if (tag !is NbtMap) {
                throw ExecutionException("level.dat 格式错误")
            }
            if (tag.isEmpty()) {
                throw ExecutionException("level.dat 内容为空")
            }
            val builder = NbtMapBuilder.from(tag)

            // 写入自己设定的配置
            // 玩家在最后一次退出游戏时的 Unix 时间戳（秒）。
            builder.putLong("LastPlayed", System.currentTimeMillis() / 1000L)
            // 存档名，修改为项目文件夹名
            builder.putString("LevelName", project.name)
            // 世界种子
            builder.putLong("RandomSeed", config.worldSeed)
            // 游戏模式，0 生存 1 创造
            builder.putInt("GameType", config.gameMode.code)
            // 世界类型，1 默认 2 超平坦
            builder.putInt("Generator", config.levelType.code)
            // 是否允许作弊
            builder.putBoolean("cheatsEnabled", config.enableCheats)
            // 保留物品栏
            builder.putBoolean("keepInventory", config.keepInventory)
            // 是否进行昼夜循环
            builder.putBoolean("dodaylightcycle", config.doDaylightCycle)
            // 是否进行天气循环
            builder.putBoolean("doweathercycle", config.doWeatherCycle)

            // 写回 level.dat
            try {
                Files.newOutputStream(levelDatPath).use { output ->
                    LevelDataUtils.writerNbt(output, builder.build(), tagInfo.version)
                }
            } catch (e: Exception) {
                throw ExecutionException("保存 $levelDatPath 文件失败：${e.message}", e)
            }

            // 写清单文件
            val behPacksManifest = Lists.newArrayList<Any>()
            val resPacksManifest = Lists.newArrayList<Any>()
            for ((type, packList) in packMaps) {
                for (pack in packList) {
                    val manifest = mapOf(
                        "pack_id" to pack.uuid,
                        "version" to pack.version.split(".").mapNotNull { it.toIntOrNull() }
                    )
                    when (type) {
                        PackType.BEHAVIOR -> behPacksManifest.add(manifest)
                        PackType.RESOURCE -> resPacksManifest.add(manifest)
                    }
                }
            }

            // 分别存入 world_behavior_packs.json 和 world_resource_packs.json
            val gson = Gson()
            val behPacksPath = worldFolderPath.resolve("world_behavior_packs.json")
            val resPacksPath = worldFolderPath.resolve("world_resource_packs.json")
            Files.newBufferedWriter(behPacksPath).use { writer ->
                gson.toJson(behPacksManifest, writer)
            }
            Files.newBufferedWriter(resPacksPath).use { writer ->
                gson.toJson(resPacksManifest, writer)
            }

            // 开始写启动参数文件
            val launchConfigPath = worldFolderPath.resolve("launch_config.cppconfig")
            val launchConfig = mapOf(
                "world_info" to mapOf(
                    "level_id" to config.worldFolderName,
                ),
                "room_info" to Maps.newHashMap<String, Any>(),
                "player_info" to mapOf(
                    "urs" to "",
                    "user_id" to 0,
                    "user_name" to config.userName
                ),
                "skin_info" to mapOf(
                    "slim" to false,
                    "skin" to gamePath.parent.resolve("data/skin_packs/vanilla/steve.png").toString()
                )
            )
            Files.newBufferedWriter(launchConfigPath).use { writer ->
                gson.toJson(launchConfig, writer)
            }

            // 调试模组的按键绑定
            // TODO：以后运行用户可以自定义按键
            val debugOptions = Gson().toJson(
                mapOf(
                    // 键码查阅：https://mc.163.com/dev/mcmanual/mc-dev/mcdocs/1-ModAPI/%E6%9E%9A%E4%B8%BE%E5%80%BC/KeyBoardType.html
                    // 绑定热更新快捷键
                    "reload_key" to 82, // R 键
                    // 绑定重载世界快捷键
                    "reload_world_key" to 96, // 小键盘 0 键
                    // 绑定重载 Addon 快捷键
                    "reload_addon_key" to "",
                    // 绑定重载着色器快捷键
                    "reload_shaders_key" to "",
                    // 是否在全体 UI 界面都触发热更新快捷键（默认 false 仅 HUD 界面）
                    "reload_key_global" to false
                )
            )

            // 热重载会重载的模组目录
            // 目前仅包含当前项目
            val projectLinuxStylePath = Paths.get(projectPath).toAbsolutePath().toString().replace('\\', '/')
            val targetModDirs = Gson().toJson(listOf(projectLinuxStylePath))

            val pluginEnv = mutableMapOf(
                "MCS_HELPER_DEBUG_OPTIONS" to debugOptions,
                "MCS_HELPER_TARGET_MOD_DIRS" to targetModDirs
            )

            // 返回命令行对象
            return GeneralCommandLine()
                .withExePath(gamePath.absolutePathString())
                .withParameters("config=${launchConfigPath.absolutePathString()}")
                .withEnvironment(pluginEnv)
        }
    }
}
