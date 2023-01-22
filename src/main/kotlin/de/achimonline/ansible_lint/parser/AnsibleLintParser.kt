package de.achimonline.ansible_lint.parser

import com.beust.klaxon.Klaxon
import com.intellij.openapi.diagnostic.logger

private val LOG = logger<AnsibleLintParser>()

class AnsibleLintParser {
    companion object {
        fun parse(json: String): List<AnsibleLintItem> {
            return try {
                Klaxon().parseArray(json) ?: emptyList()
            } catch (exception: Exception) {
                LOG.error(exception)
                emptyList()
            }
        }
    }
}
