package de.achimonline.ansible_lint.parser

import com.intellij.lang.annotation.HighlightSeverity
import de.achimonline.ansible_lint.parser.sarif.ReportingConfiguration.Level
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class AnsibleLintParserTest {
    private fun getFile(name: String): File = File(javaClass.getResource("/${name}")!!.file)

    @Test
    fun parse() {
        val json = getFile("sarif_result.json").readText()

        val parsed = AnsibleLintParser.parse(json)

        assertEquals(6, parsed.size)

        val ansibleLintItem = parsed.first()

        assertEquals("name[casing]", ansibleLintItem.ruleId)
        assertEquals("All names should start with an uppercase letter.", ansibleLintItem.description)
        assertEquals("All names should start with an uppercase letter.", ansibleLintItem.message)
        assertEquals(1, ansibleLintItem.startLine)
        assertNull(ansibleLintItem.endLine)
        assertNull(ansibleLintItem.startColumn)
        assertNull(ansibleLintItem.endColumn)
        assertEquals("All tasks and plays should have a distinct name for readability and for ``--start-at-task`` to work", ansibleLintItem.helpText)
        assertEquals("https://ansible.readthedocs.io/projects/lint/rules/name/", ansibleLintItem.helpUri)
        assertEquals(HighlightSeverity.WARNING, ansibleLintItem.severity)
    }

    @Test
    fun sarifSeverityMapper() {
        assertEquals(HighlightSeverity.ERROR, AnsibleLintParser.sarifSeverityMapper(Level.ERROR))
        assertEquals(HighlightSeverity.WARNING, AnsibleLintParser.sarifSeverityMapper(Level.WARNING))
        assertEquals(HighlightSeverity.INFORMATION, AnsibleLintParser.sarifSeverityMapper(Level.NOTE))
        assertEquals(HighlightSeverity.INFORMATION, AnsibleLintParser.sarifSeverityMapper(Level.NONE))
    }
}
