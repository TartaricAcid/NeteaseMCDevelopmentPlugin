package com.github.tartaricacid.mcshelper.run

import com.github.tartaricacid.mcshelper.gui.MCSettingsEditor
import com.github.tartaricacid.mcshelper.log.LogFilteredProcessHandler
import com.github.tartaricacid.mcshelper.options.MCRunConfigurationOptions
import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.ConsoleView
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project

class MCRunConfiguration(project: Project, factory: ConfigurationFactory?, name: String?) :
    RunConfigurationBase<MCRunConfigurationOptions?>(project, factory, name) {
    public override fun getOptions(): MCRunConfigurationOptions {
        return super.getOptions() as MCRunConfigurationOptions
    }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration?> {
        return MCSettingsEditor()
    }

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? {
        return object : CommandLineState(environment) {
            @Throws(ExecutionException::class)
            override fun startProcess(): ProcessHandler {
                val commandLine = ConfigRunTask.run(project, options)
                val processHandler = LogFilteredProcessHandler(commandLine, options)
                ProcessTerminatedListener.attach(processHandler)
                return processHandler
            }

            override fun createConsole(executor: Executor): ConsoleView? {
                val consoleView = super.createConsole(executor)
                consoleView?.addMessageFilter(PythonErrorFilter(project))
                return consoleView
            }
        }
    }
}