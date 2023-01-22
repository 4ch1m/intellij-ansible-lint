package de.achimonline.ansible_lint.command

import com.intellij.openapi.project.Project
import java.io.File
import java.nio.file.Paths
import kotlin.io.path.pathString

class AnsibleLintConfigFile {
    companion object {
        const val CONFIG_FILE_NAME = ".ansible-lint"
        const val CONFIG_FILE_NAME_ALTERNATIVE = "ansible-lint.yml"
        const val CONFIG_DIR_NAME = ".config"

        private val CONFIG_FILE_INIT_TEXT = """
            ---
            # created by 'IntelliJ Ansible Lint Plugin'
            #
            # check this website for detailed configuration options:
            #    https://ansible-lint.readthedocs.io/configuring/#ansible-lint-configuration
            
            profile: null
        """.trimIndent()

        fun exists(project: Project): Boolean {
            return listOf(
                CONFIG_FILE_NAME,
                "$CONFIG_DIR_NAME${File.separator}$CONFIG_FILE_NAME_ALTERNATIVE"
            ).any {
                File("${project.basePath}${File.separator}${it}").exists()
            }
        }

        fun get(directory: String): String? {
            val path = listOf(
                CONFIG_FILE_NAME,
                "$CONFIG_DIR_NAME${File.separator}$CONFIG_FILE_NAME_ALTERNATIVE"
            ).firstOrNull {
                File("${directory}${File.separator}${it}").exists()
            }

            if (path != null) {
                return Paths.get(path).toRealPath().pathString
            }

            return null
        }

        fun create(project: Project, inConfigDir: Boolean): File {
            if (inConfigDir) {
                val configDir = File("${project.basePath}${File.separator}$CONFIG_DIR_NAME")

                if (!configDir.exists()) {
                    configDir.mkdir()
                }

                val configFileAlternative = File("${project.basePath}${File.separator}$CONFIG_DIR_NAME$CONFIG_FILE_NAME_ALTERNATIVE")

                if (!configFileAlternative.exists()) {
                    configFileAlternative.createNewFile()
                    configFileAlternative.writeText(CONFIG_FILE_INIT_TEXT)
                }

                return configFileAlternative
            } else {
                val configFile = File("${project.basePath}${File.separator}$CONFIG_FILE_NAME")

                if (!configFile.exists()) {
                    configFile.createNewFile()
                    configFile.writeText(CONFIG_FILE_INIT_TEXT)
                }

                return configFile
            }
        }
    }
}
