package de.achimonline.ansible_lint.command

import io.github.z4kn4fein.semver.Version
import io.github.z4kn4fein.semver.VersionFormatException

abstract class AnsibleLintCommandLine {
    class ProcessResult(process: Process) {
        val stdout = process.inputStream.bufferedReader().use { it.readText() }
        val stderr = process.errorStream.bufferedReader().use { it.readText() }

        init {
            process.waitFor()
        }

        val rc = process.exitValue()
    }

    abstract fun createVersionCheckProcess(): Process

    abstract fun createLintProcess(
        workingDirectory: String,
        projectDirectory: String,
        configFile: String?,
        yamlFilePath: String
    ): Process

    companion object {
        val MIN_EXECUTABLE_VERSION = Version(6, 14, 3)
        val SUCCESS_RETURN_CODES = listOf(0, 2)

        private val VERSIONS_REGEX = "ansible-lint (.*) using (ansible |ansible-core:)([^ ]*)".toRegex()

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
