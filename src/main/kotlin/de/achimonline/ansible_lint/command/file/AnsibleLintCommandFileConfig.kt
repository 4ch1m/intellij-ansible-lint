package de.achimonline.ansible_lint.command.file

import com.charleskorn.kaml.*
import com.intellij.openapi.project.Project
import java.io.File

class AnsibleLintCommandFileConfig(project: Project) : AnsibleLintCommandFile(
    project,
    listOf(DEFAULT, ALTERNATIVE)
) {
    companion object {
        const val DEFAULT = ".ansible-lint"
        val ALTERNATIVE = ".config${File.separator}ansible-lint.yml"

        const val YAML_DEFAULT_INDENT = "  "
        const val YAML_SKIP_LIST_NODE = "skip_list"
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

    fun addRuleToSkipList(rule: String) {
        val configFile = locateOrCreate()
        val configText = configFile.readText()
        val configYamlNode = Yaml.default.parseToYamlNode(configText)

        val skipList = configYamlNode.yamlMap.get<YamlList>(YAML_SKIP_LIST_NODE)

        if (skipList != null && skipList.items.isNotEmpty()) {
            if (skipList.items.any {
                    it.yamlScalar.content.trim().trim('"').trim('\'') == rule
                }) {
                return
            } else {
                val newConfigText = mutableListOf<String>()

                configText.trimEnd().split(System.lineSeparator()).forEach { configLine ->
                    newConfigText.add(configLine)

                    if (configLine.startsWith("${YAML_SKIP_LIST_NODE}:")) {
                        // try to determine the YAML indent-level of existing entries ...
                        val firstSkippedRule = skipList.items[0].yamlScalar.path.segments.firstOrNull {
                            it is YamlPathSegment.MapElementValue
                        }
                        val yamlIndent = if (firstSkippedRule != null) {
                            " ".repeat(firstSkippedRule.location.column - 1)
                        } else {
                            YAML_DEFAULT_INDENT
                        }

                        newConfigText.add("${yamlIndent}- $rule")
                    }
                }

                newConfigText.add("") // NOTE: intentional new line at the end

                configFile.writeText(newConfigText.joinToString(System.lineSeparator()))
            }
        } else {
            configFile.writeText(
                listOf(
                    configText.trimEnd(),
                    "",
                    "${YAML_SKIP_LIST_NODE}:",
                    "${YAML_DEFAULT_INDENT}- $rule",
                    "" // NOTE: intentional new line at the end
                ).joinToString(System.lineSeparator())
            )
        }
    }
}
