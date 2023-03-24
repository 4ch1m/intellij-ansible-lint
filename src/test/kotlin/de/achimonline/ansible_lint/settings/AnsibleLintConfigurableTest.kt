package de.achimonline.ansible_lint.settings

import com.intellij.icons.AllIcons
import org.junit.Test

import org.junit.Assert.*

class AnsibleLintConfigurableTest {
    @Test
    fun executeTest_icons() {
        // make sure the icons (which are being used in an HTML-strings) are still available
        assertNotNull(AllIcons.General.Information)
        assertNotNull(AllIcons.General.InspectionsOK)
        assertNotNull(AllIcons.General.NotificationWarning)
        assertNotNull(AllIcons.General.NotificationError)
    }
}
