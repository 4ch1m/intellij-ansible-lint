package de.achimonline.ansible_lint.command

import com.intellij.execution.configurations.GeneralCommandLine
import de.achimonline.ansible_lint.settings.AnsibleLintSettings
import java.nio.file.Paths
import kotlin.io.path.pathString

class AnsibleLintCommandLine(private val settings: AnsibleLintSettings = AnsibleLintSettings()) {
    fun createVersionCheckProcess(workingDirectory: String): Process {
        return GeneralCommandLine()
            .withEnvironment(System.getenv())
            .withWorkDirectory(workingDirectory)
            .withExePath(settings.executable)
            .withParameters(listOf(
                "--nocolor",
                "--version"
            ))
            .createProcess()
    }

    fun createLintProcess(
        workingDirectory: String,
        projectDirectory: String,
        yamlFilePath: String
    ): Process {
        val parameters = mutableListOf(
            "-q",
            "--parseable",
            "--format", "json",
        )

        val configFile = AnsibleLintConfigFile.get(projectDirectory)
        if (configFile != null) {
            parameters.addAll(listOf(
                "--config-file", configFile
            ))
        }

        if (settings.offline) {
            parameters.add("--offline")
        }

        parameters.addAll(listOf(
            "--project-dir",
            /* make sure we resolve symlinks: */
            Paths.get(projectDirectory).toRealPath().pathString
        ))

        parameters.add(yamlFilePath)

        return GeneralCommandLine()
            .withEnvironment(System.getenv())
            .withWorkDirectory(workingDirectory)
            .withExePath(settings.executable)
            .withParameters(parameters)
            .createProcess()
    }

    companion object {
        val SUCCESS_RETURN_CODES = listOf(0, 2)

        fun getOutput(process: Process): Pair<String, String> {
            process.waitFor()

            return Pair(
                process.inputStream.bufferedReader().use { it.readText() },
                process.errorStream.bufferedReader().use { it.readText() }
            )
        }
    }
}
