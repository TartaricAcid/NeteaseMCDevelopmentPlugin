package com.github.tartaricacid.nemc.log

import com.github.tartaricacid.nemc.options.LogLevel
import com.github.tartaricacid.nemc.options.MCRunConfigurationOptions
import com.github.tartaricacid.nemc.util.PathUtils
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.AnsiEscapeDecoder
import com.intellij.execution.process.KillableProcessHandler
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.openapi.util.Key
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

const val RESET: String = "\u001B[0m"
const val RED: String = "\u001B[31m"
const val GREEN: String = "\u001B[32m"
const val YELLOW: String = "\u001B[33m"
const val DARK_GRAY: String = "\u001B[90m"
const val GRAY: String = "\u001B[37m"
const val CYAN: String = "\u001B[36m"
const val BOLD: String = "\u001B[1m"

/**
 * 系统日志的正则表达式匹配，一般情况下不需要显示
 */
val SYS_LOG = Regex(
    """
    (?x)                                            # 启用扩展模式：忽略空白并允许使用 '#' 注释
    ^
    \[                                      
    (\d{4}-\d{2}-\d{2}\s\d{2}:\d{2}:\d{2}:\d{3})\s  # 时间戳: yyyy-MM-dd HH:mm:ss:SSS
    (VERBOSE|INFO|WARN|ERROR)\s                     # 日志等级
    (\S+)\s                                         # 模块/标签名
    (\d+)\s                                         # pid，进程 ID
    (\d+)                                           # tid，线程 ID
    ]\s
    (.*)                                            # 日志消息
    $
    """.trimIndent()
)

/**
 * 游戏本体日志的正则表达式匹配，一般情况下需要过滤显示
 */
val GAME_LOG = Regex(
    """
    (?x)                                             # 启用扩展模式：忽略空白并允许使用 '#' 注释                                          
    ^
    \[Python]\s
    \[(\d{4}-\d{2}-\d{2}\s\d{2}:\d{2}:\d{2},\d{3})]  # 时间戳: yyyy-MM-dd HH:mm:ss,SSS
    \s
    (.*)                                             # 剩余部分（含若干 [..]）
    $
    """.trimIndent()
)

const val PYTHON_HEADER = "[Python]"

class LogFilteredProcessHandler(commandLine: GeneralCommandLine, val options: MCRunConfigurationOptions) :
    KillableProcessHandler(commandLine), AnsiEscapeDecoder.ColoredTextAcceptor {
    private val myAnsiEscapeDecoder = AnsiEscapeDecoder()
    private val buffers = mutableMapOf<Key<*>, StringBuilder>()

    override fun getCharset(): Charset {
        return StandardCharsets.UTF_8
    }

    override fun startNotify() {
        super.startNotify()

        // 启动进程时，按照规范标准开始打印，并添加一些额外系统信息
        val timeStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss,SSS"))
        val header = "$BOLD$CYAN[$timeStr] [INFO] [System] "

        myAnsiEscapeDecoder.escapeText(
            "${header}开始运行游戏$RESET\n",
            ProcessOutputTypes.STDOUT, this
        )

        myAnsiEscapeDecoder.escapeText(
            "${header}日志记录模式：${options.logLevel.displayName}$RESET\n",
            ProcessOutputTypes.STDOUT, this
        )

        myAnsiEscapeDecoder.escapeText(
            "${header}启动器路径：${options.gameExecutablePath}$RESET\n",
            ProcessOutputTypes.STDOUT, this
        )

        val worldDir = PathUtils.worldsDir()
        if (worldDir != null) {
            val fullWorldPath = worldDir.resolve(options.worldFolderName).toAbsolutePath().toString()
            myAnsiEscapeDecoder.escapeText(
                "${header}世界存档路径：${fullWorldPath}$RESET\n",
                ProcessOutputTypes.STDOUT, this
            )
        }

        val includedModsText = if (options.includedModDirs.isEmpty()) {
            "无"
        } else {
            options.includedModDirs.joinToString(", ")
        }
        myAnsiEscapeDecoder.escapeText(
            "${header}包含组件目录：$includedModsText$RESET\n",
            ProcessOutputTypes.STDOUT, this
        )
    }

    override fun notifyTextAvailable(text: String, outputType: Key<*>) {
        val buf = buffers.getOrPut(outputType) { StringBuilder() }
        buf.append(text)

        val newlineIndex = buf.indexOf("\n")
        if (newlineIndex == -1) {
            return
        }

        // 取出一整行，并去除首尾换行
        var line = buf.substring(0, newlineIndex + 1).trim()
        // 移除已处理部分（包括换行符）
        buf.delete(0, newlineIndex + 1)

        // System 级别的日志，不能着色，直接输出
        if (outputType == ProcessOutputTypes.SYSTEM) {
            myAnsiEscapeDecoder.escapeText(line + "\n", outputType, this)
            return
        }

        // 依据日志等级处理
        line = if (options.logLevel == LogLevel.VERBOSE) {
            handleVerboseLog(line) ?: return
        } else {
            handleNormalLog(line) ?: return
        }

        if (line.isEmpty()) {
            return
        }
        myAnsiEscapeDecoder.escapeText(line + "\n", outputType, this)
    }

    // 进程结束时把缓冲区中剩余没有换行的部分也处理一次
    override fun notifyProcessTerminated(exitCode: Int) {
        try {
            for ((outputType, sb) in buffers) {
                if (sb.isNotEmpty()) {
                    var line = sb.toString().trim()
                    myAnsiEscapeDecoder.escapeText(line + "\n", outputType, this)
                }
            }
        } finally {
            buffers.clear()
            super.notifyProcessTerminated(exitCode)

            // 进程结束时，打印退出信息
            myAnsiEscapeDecoder.escapeText(
                "${BOLD}${RED}游戏进程已退出，退出代码：$exitCode$RESET\n",
                ProcessOutputTypes.STDOUT, this
            )
        }
    }

    override fun coloredTextAvailable(text: String, attributes: Key<*>) {
        super.notifyTextAvailable(text, attributes)
    }

    /**
     * 普通情况下，仅显示 Python 输出的日志内容
     */
    fun handleNormalLog(line: String): String? {
        val matchResult = GAME_LOG.find(line)
        if (matchResult == null) {
            // 如果是普通的 [Python] 开头的日志，剔除头部后返回
            if (line.startsWith(PYTHON_HEADER)) {
                val trimLine = line.substring(PYTHON_HEADER.length, line.length).trim()
                val coloredLevel = getColoredLog(trimLine)
                return "$coloredLevel$trimLine$RESET"
            }
            return null
        }

        val rest = matchResult.groupValues[2]
        // 提取所有中括号内的 token
        val bracketRe = Regex("""\[(.*?)]""")
        val tokens = bracketRe.findAll(rest).map { it.groupValues[1] }.toList()

        val module = tokens.getOrNull(1) ?: ""
        // 剔除引擎噪声
        if (module == "Engine") {
            return null
        }

        // 将所有中括号内容去掉，剩下的就是消息
        val message = rest.replace(bracketRe, "").trim()
        // 去除 get_cls 噪声
        if (message == "get_cls" || message == "get_cls success!!!") {
            return null
        }

        val level = tokens.getOrNull(0) ?: ""
        var coloredLevel = getColoredLog(level)
        // 如果是 Developer 那么，打印成灰色
        if (module == "Developer") {
            coloredLevel = DARK_GRAY
        }
        val trimLine = line.substring(PYTHON_HEADER.length, line.length).trim()

        return "$coloredLevel$trimLine$RESET"
    }

    fun handleVerboseLog(line: String): String? {
        val sysLogMatch = SYS_LOG.find(line)
        if (sysLogMatch != null) {
            val level = sysLogMatch.groupValues[2]
            val coloredLevel = getColoredLog(level)
            return "$coloredLevel$line$RESET"
        }

        val matchResult = GAME_LOG.find(line)
        if (matchResult != null) {
            val rest = matchResult.groupValues[2]
            // 提取所有中括号内的 token
            val bracketRe = Regex("""\[(.*?)]""")
            val tokens = bracketRe.findAll(rest).map { it.groupValues[1] }.toList()
            val level = tokens.getOrNull(0) ?: ""
            val coloredLevel = getColoredLog(level)
            return "$coloredLevel$line$RESET"
        }

        var coloredLevel = getColoredLog(line)
        // 开头会刷 NO LOG FILE，着暗灰色
        if (line.startsWith("NO LOG FILE!")) {
            coloredLevel = DARK_GRAY
        }
        return "$coloredLevel$line$RESET"
    }

    fun getColoredLog(line: String): String {
        return when {
            line.contains("ERROR", ignoreCase = false) -> RED
            line.contains("WARN", ignoreCase = false) -> YELLOW
            line.contains("SUC", ignoreCase = false) -> GREEN
            line.contains("INFO", ignoreCase = false) -> GRAY
            line.contains("VERBOSE", ignoreCase = false) -> DARK_GRAY
            else -> RESET
        }
    }
}
