package de.achimonline.ansible_lint.command.file

import com.intellij.openapi.project.Project
import java.io.File

class AnsibleLintCommandFileConfig(project: Project) : AnsibleLintCommandFile(
    project,
    listOf(DEFAULT, ALTERNATIVE)
) {
    companion object {
        const val DEFAULT = ".ansible-lint"
        val ALTERNATIVE = ".config${File.separator}ansible-lint.yml"
    }

    override fun initialContent(): String {
        return """
            ---
            # created by 'IntelliJ Ansible Lint Plugin'
            #
            # check this website for detailed configuration options:
            #    https://ansible-lint.readthedocs.io/configuring/#ansible-lint-configuration
            
            profile: null
        """.trimIndent()
    }
}
