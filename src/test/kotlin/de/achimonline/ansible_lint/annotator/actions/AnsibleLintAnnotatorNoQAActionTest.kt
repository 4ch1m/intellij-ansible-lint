package de.achimonline.ansible_lint.annotator.actions

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnsibleLintAnnotatorNoQAActionTest {
    @Test
    fun existingNoQARegex() {
        val regex = AnsibleLintAnnotatorNoQAAction.EXISTING_NO_QA_REGEX

        assertTrue(regex.matches("copy: # noqa fqcn[action-core]"))
        assertTrue(regex.matches("copy: # noqa: fqcn[action-core]"))
        assertTrue(regex.matches("copy: # noqa"))
        assertTrue(regex.matches("copy: # noqa:"))

        assertFalse(regex.matches("copy: #noqa: fqcn[action-core]"))
        assertFalse(regex.matches("copy: #    noqa: fqcn[action-core]"))
        assertFalse(regex.matches("copy: #\tnoqa: fqcn[action-core]"))
    }
}
