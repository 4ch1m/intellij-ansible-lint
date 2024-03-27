package de.achimonline.ansible_lint.command.file

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import de.achimonline.ansible_lint.common.AnsibleLintHelper
import java.io.File

class AnsibleLintCommandFileIgnore(project: Project) : AnsibleLintCommandFile(
    project,
    listOf(DEFAULT, ALTERNATIVE)
) {
    companion object {
        const val DEFAULT = ".ansible-lint-ignore"
        val ALTERNATIVE = ".config${File.separator}ansible-lint-ignore.txt"
    }

    fun addRule(psiFile: PsiFile, rule: String) {
        val projectBasePath = AnsibleLintHelper.getProjectBasePath(project)
        val relativeFilePath = psiFile.virtualFile.path
            .removePrefix(projectBasePath)
            .removePrefix(File.separator)

        val ignoreFile = locateOrCreate()
        val ignoreFileLines = ignoreFile.readLines().toMutableList()

        if (!ignoreFileLines.any { it.startsWith("$relativeFilePath ") && it.contains(rule) }) {
            ignoreFileLines += "$relativeFilePath $rule"

            ignoreFile.writeText("${ignoreFileLines.joinToString(System.lineSeparator())}${System.lineSeparator()}")
        }
    }

    override fun initialContent(): String {
        return """
            # created by 'IntelliJ Ansible Lint Plugin'
        """.trimIndent()
    }

    fun parse(): MutableMap<String, MutableSet<String>> {
        val ignores = mutableMapOf<String, MutableSet<String>>()

        locate()?.readLines()?.forEach {
            val trimmedLine = it.trim()

            if (trimmedLine != "" && !trimmedLine.startsWith("#")) {
                val splitLine = trimmedLine.split("#").first().trim().split(" ")

                if (splitLine.size == 2) {
                    val (path, rule) = splitLine
                    val rules = ignores[path] ?: mutableSetOf()
                    rules.add(rule)
                    ignores[path] = rules
                }
            }
        }

        return ignores
    }
}
