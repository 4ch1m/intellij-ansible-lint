package de.achimonline.ansible_lint.command

import com.intellij.execution.configurations.GeneralCommandLine
import de.achimonline.ansible_lint.settings.AnsibleLintSettings
import java.nio.file.Paths
import kotlin.io.path.pathString

class AnsibleLintCommandLineUnix(private val settings: AnsibleLintSettings = AnsibleLintSettings()) : AnsibleLintCommandLine() {
    override fun createVersionCheckProcess(): Process {
        return GeneralCommandLine()
            .withEnvironment(System.getenv())
            .withExePath(settings.executable)
            .withParameters(listOf(
                "--nocolor",
                "--version"
            ))
            .createProcess()
    }

    override fun createLintProcess(
        workingDirectory: String,
        projectDirectory: String,
        configFile: String?,
        yamlFilePath: String
    ): Process {
        val parameters = mutableListOf(
            "-q",
            "--parseable",
            "--format", "sarif"
        )

        if (configFile != null) {
            parameters.addAll(
                listOf(
                    "--config-file", configFile
                )
            )
        }

        if (settings.offline) {
            parameters.add("--offline")
        }

        parameters.addAll(
            listOf(
                "--project-dir",
                /* make sure we resolve symlinks: */
                Paths.get(projectDirectory).toRealPath().pathString
            )
        )

        parameters.add(yamlFilePath)

        return GeneralCommandLine()
            .withEnvironment(System.getenv())
            .withWorkDirectory(workingDirectory)
            .withExePath(settings.executable)
            .withParameters(parameters)
            .createProcess()
    }
}
