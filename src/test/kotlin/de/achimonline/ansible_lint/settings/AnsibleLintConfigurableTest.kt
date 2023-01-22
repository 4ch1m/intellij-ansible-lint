package de.achimonline.ansible_lint.settings

import com.intellij.icons.AllIcons
import org.junit.Test

import org.junit.Assert.*

class AnsibleLintConfigurableTest {
    @Test
    fun executeTest_icons() {
        // make sure the icons (which are being used in an HTML-string) are still available
        assertNotNull(AllIcons.General.InspectionsOK)
        assertNotNull(AllIcons.General.InspectionsError)
    }
}
