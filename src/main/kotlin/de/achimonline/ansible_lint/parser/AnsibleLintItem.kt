package de.achimonline.ansible_lint.parser

import com.intellij.lang.annotation.HighlightSeverity

data class AnsibleLintItem(
    val ruleId: String,
    val description: String,
    val message: String,
    val startLine: Int,
    val endLine: Int? = null,
    val startColumn: Int? = null,
    val endColumn: Int? = null,
    val helpText: String,
    val helpUri: String,
    val severity: HighlightSeverity = HighlightSeverity.INFORMATION,
    val tags: Set<String> = emptySet()
)
