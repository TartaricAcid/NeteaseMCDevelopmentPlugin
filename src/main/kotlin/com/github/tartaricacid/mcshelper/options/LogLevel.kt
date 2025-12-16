package com.github.tartaricacid.mcshelper.options

enum class LogLevel {
    /**
     * 普通级别，此时只会打印 Python 层面的日志
     */
    NORMAL,

    /**
     * 全部级别，此时会打印游戏全部系统日志和 Python 层面的日志
     */
    VERBOSE;

    val code: Int = ordinal
    val displayName: String
        get() = when (this) {
            NORMAL -> "默认"
            VERBOSE -> "全部"
        }
}