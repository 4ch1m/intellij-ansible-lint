package de.achimonline.ansible_lint.annotator

import com.intellij.lang.annotation.HighlightSeverity
import de.achimonline.ansible_lint.parser.AnsibleLintItem
import org.junit.Test

import org.junit.Assert.*

class AnsibleLintAnnotatorTest {
    @Test
    fun getAnnotationHighlightSeverity() {
        val ansibleLintAnnotator = AnsibleLintAnnotator()

        assertEquals(HighlightSeverity.ERROR, ansibleLintAnnotator.getAnnotationHighlightSeverity(AnsibleLintItem(severity = "blocker")))
        assertEquals(HighlightSeverity.ERROR, ansibleLintAnnotator.getAnnotationHighlightSeverity(AnsibleLintItem(severity = "critical")))
        assertEquals(HighlightSeverity.WARNING, ansibleLintAnnotator.getAnnotationHighlightSeverity(AnsibleLintItem(severity = "major")))
        assertEquals(HighlightSeverity.WEAK_WARNING, ansibleLintAnnotator.getAnnotationHighlightSeverity(AnsibleLintItem(severity = "minor")))
        assertEquals(HighlightSeverity.INFORMATION, ansibleLintAnnotator.getAnnotationHighlightSeverity(AnsibleLintItem(severity = "info")))

        assertEquals(HighlightSeverity.ERROR, ansibleLintAnnotator.getAnnotationHighlightSeverity(AnsibleLintItem(severity = " Blocker ")))
        assertEquals(HighlightSeverity.ERROR, ansibleLintAnnotator.getAnnotationHighlightSeverity(AnsibleLintItem(severity = " Critical ")))
        assertEquals(HighlightSeverity.WARNING, ansibleLintAnnotator.getAnnotationHighlightSeverity(AnsibleLintItem(severity = " Major ")))
        assertEquals(HighlightSeverity.WEAK_WARNING, ansibleLintAnnotator.getAnnotationHighlightSeverity(AnsibleLintItem(severity = " Minor ")))
        assertEquals(HighlightSeverity.INFORMATION, ansibleLintAnnotator.getAnnotationHighlightSeverity(AnsibleLintItem(severity = " Info ")))

        assertEquals(HighlightSeverity.INFORMATION, ansibleLintAnnotator.getAnnotationHighlightSeverity(AnsibleLintItem(severity = " ")))
        assertEquals(HighlightSeverity.INFORMATION, ansibleLintAnnotator.getAnnotationHighlightSeverity(AnsibleLintItem(severity = "UNKNOWN")))
    }
}
