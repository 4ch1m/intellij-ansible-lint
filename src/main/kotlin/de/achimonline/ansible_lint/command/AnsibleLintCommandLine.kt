package de.achimonline.ansible_lint.command

import com.intellij.execution.configurations.GeneralCommandLine
import de.achimonline.ansible_lint.settings.AnsibleLintSettings
import io.github.z4kn4fein.semver.Version
import io.github.z4kn4fein.semver.VersionFormatException
import java.nio.file.Paths
import kotlin.io.path.pathString

class AnsibleLintCommandLine(private val settings: AnsibleLintSettings = AnsibleLintSettings()) {
    fun createVersionCheckProcess(workingDirectory: String): Process {
        return GeneralCommandLine()
            .withEnvironment(System.getenv())
            .withWorkDirectory(workingDirectory)
            .withExePath(settings.executable)
            .withParameters(
                listOf(
                    "--nocolor",
                    "--version"
                )
            )
            .createProcess()
    }

    fun createLintProcess(
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

    companion object {
        val MIN_EXECUTABLE_VERSION = Version(6, 14, 3)
        val SUCCESS_RETURN_CODES = listOf(0, 2)

            private val VERSIONS_REGEX = "ansible-lint (.*) using (ansible |ansible-core:)([^ ]*)".toRegex()

        fun getOutput(process: Process): Pair<String, String> {
            process.waitFor()

            return Pair(
                process.inputStream.bufferedReader().use { it.readText() },
                process.errorStream.bufferedReader().use { it.readText() }
            )
        }

        fun getVersions(versionOutput: String): Pair<Version, Version>? {
            val versions = VERSIONS_REGEX.find(versionOutput)

            return if (versions != null) {
                var (ansibleLint, _, ansible, _) = versions.destructured

                // development-builds of 'ansible-lint' use version numbers that aren't SemVer conform;
                // this workaround is needed to make parsing possible
                if (ansibleLint.contains("dev")) {
                    ansibleLint = ansibleLint.replace("dev", "")
                    ansibleLint += "-dev"
                }

                try {
                    Pair(
                        Version.parse(ansibleLint, strict = false),
                        Version.parse(ansible, strict = false)
                    )
                } catch (versionFormatException: VersionFormatException) {
                    null
                }
            } else {
                null
            }
        }
    }
}
