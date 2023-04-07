package de.achimonline.ansible_lint.parser

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.diagnostic.logger
import de.achimonline.ansible_lint.parser.sarif.Result.Level
import de.achimonline.ansible_lint.parser.sarif.Sarif

private val LOG = logger<AnsibleLintParser>()

class AnsibleLintParser {
    companion object {
        private val mapper = jacksonObjectMapper()

        fun parse(json: String): List<AnsibleLintItem> {
            val sarif: Sarif = mapper.readValue(json)

            if (sarif.runs.isNullOrEmpty()) {
                LOG.warn("Parser returned empty list of 'runs'.")
                return emptyList()
            }

            val lintItems = mutableListOf<AnsibleLintItem>()

            val run = sarif.runs.first() // ansible-lint generates only one "run" object

            run.results.forEach { result ->
                try {
                    val region = result.locations.first().physicalLocation.region
                    val rule = run.tool.driver.rules.first { rule -> rule.id == result.ruleId }

                    lintItems.add(
                        AnsibleLintItem(
                            ruleId = result.ruleId,
                            description = rule.shortDescription.text,
                            message = result.message.text,
                            startLine = region.startLine,
                            endLine = region.endLine,
                            startColumn = region.startColumn,
                            endColumn = region.endColumn,
                            helpText = rule.help.text,
                            helpUri = rule.helpUri.toString(),
                            severity = sarifSeverityMapper(result.level),
                            tags = rule.properties.tags
                        )
                    )
                } catch (exception: Exception) {
                    LOG.error(exception)
                }
            }

            return lintItems
        }

        /**
        based on:
        - [SarifFormatter#_to_sarif_level](https://github.com/ansible/ansible-lint/blob/eda1aba7ee679b9ddfdc9860dc6ed3d58292ce26/src/ansiblelint/formatters/__init__.py#L303-L306)
         */
        fun sarifSeverityMapper(level: Level): HighlightSeverity {
            return when (level) {
                Level.ERROR -> HighlightSeverity.ERROR
                Level.WARNING -> HighlightSeverity.WARNING

                else -> {
                    HighlightSeverity.INFORMATION
                }
            }
        }
    }
}
