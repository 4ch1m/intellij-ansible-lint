package de.achimonline.ansible_lint.command

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.wsl.WSLDistribution
import de.achimonline.ansible_lint.settings.AnsibleLintSettings
import java.nio.file.Paths
import kotlin.io.path.Path
import kotlin.io.path.pathString

class AnsibleLintCommandLineWSL(private val settings: AnsibleLintSettings = AnsibleLintSettings(), projectBasePath: String) : AnsibleLintCommandLine() {
    inner class WSLPathException(path: String) : Exception("Unable to create WSL-path for: $path")

    private val wslDistribution = WSLDistribution(projectBasePath)

    override fun createVersionCheckProcess(): Process {
        return GeneralCommandLine()
            .withEnvironment(System.getenv())
            .withExePath(WSL_EXE)
            .withParameters(listOf(
                settings.executable,
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
            settings.executable,
            "-q",
            "--parseable",
            "--format", "sarif"
        )

        if (configFile != null) {
            parameters.addAll(
                listOf(
                    "--config-file",
                    wslDistribution.getWslPath(Path(configFile)) ?: throw WSLPathException(configFile)
                )
            )
        }

        if (settings.offline) {
            parameters.add("--offline")
        }

        /* make sure we resolve symlinks: */
        val realProjectDirPath = Paths.get(projectDirectory).toRealPath().pathString

        parameters.addAll(
            listOf(
                "--project-dir",
                wslDistribution.getWslPath(Path(realProjectDirPath)) ?: throw WSLPathException(realProjectDirPath)
            )
        )

        parameters.add(wslDistribution.getWslPath(Path(yamlFilePath)) ?: throw WSLPathException(yamlFilePath))

        return GeneralCommandLine()
            .withEnvironment(System.getenv())
            .withWorkDirectory(workingDirectory)
            .withExePath(WSL_EXE)
            .withParameters(parameters)
            .createProcess()
    }

    companion object {
        const val WSL_EXE = "wsl.exe"
    }
}
