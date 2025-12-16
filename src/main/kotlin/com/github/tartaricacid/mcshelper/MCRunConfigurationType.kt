package com.github.tartaricacid.mcshelper

import com.github.tartaricacid.mcshelper.run.MCRunConfiguration
import com.github.tartaricacid.mcshelper.options.MCRunConfigurationOptions
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.SimpleConfigurationType
import com.intellij.icons.AllIcons
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NotNullLazyValue

const val ID: String = "NetEaseMCRunConfiguration"
const val NAME = "网易组件开发"
const val DESCRIPTION = "运行网易我的世界中国版开发包"

val ICON = NotNullLazyValue.createValue { AllIcons.Nodes.Console }

class MCRunConfigurationType internal constructor() : SimpleConfigurationType(ID, NAME, DESCRIPTION, ICON) {
    override fun createTemplateConfiguration(project: Project): RunConfiguration {
        return MCRunConfiguration(project, this, "NetEaseMC")
    }

    override fun getOptionsClass(): Class<out BaseState?>? {
        return MCRunConfigurationOptions::class.java
    }
}
