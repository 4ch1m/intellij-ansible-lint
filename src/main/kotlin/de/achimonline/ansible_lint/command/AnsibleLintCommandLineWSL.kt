package de.achimonline.ansible_lint.command

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.wsl.WSLDistribution
import com.intellij.execution.wsl.WSLDistribution.WSL_EXE
import com.intellij.execution.wsl.WslDistributionManager
import de.achimonline.ansible_lint.settings.AnsibleLintSettings
import java.nio.file.Paths
import kotlin.io.path.Path
import kotlin.io.path.pathString

class AnsibleLintCommandLineWSL(private val settings: AnsibleLintSettings = AnsibleLintSettings()) : AnsibleLintCommandLine() {
    inner class WSLPathException(path: String) : Exception("Unable to create WSL-path for: $path")

    private val wslDistributionManager = WslDistributionManager.getInstance()

    private var wslDistribution: WSLDistribution

    init {
        var wslDistributionIdToUse = wslDistributionManager.installedDistributions.first().id

        if (settings.wslDistributionId != null) {
            if (wslDistributionManager.installedDistributions.firstOrNull { it.id == settings.wslDistributionId } != null) {
                wslDistributionIdToUse = settings.wslDistributionId!!
            }
        }

        wslDistribution = WSLDistribution(wslDistributionIdToUse)
    }

    override fun createVersionCheckProcess(): Process {
        return GeneralCommandLine()
            .withEnvironment(System.getenv())
            .withExePath(WSL_EXE)
            .withParameters(listOf(
                "--distribution", wslDistribution.id,
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
            "--distribution", wslDistribution.id,
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
}
