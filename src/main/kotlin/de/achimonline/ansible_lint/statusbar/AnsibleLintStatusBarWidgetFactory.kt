package de.achimonline.ansible_lint.statusbar

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.ui.GotItTooltip
import de.achimonline.ansible_lint.bundle.AnsibleLintBundle.message
import java.net.URL

class AnsibleLintStatusBarWidgetFactory : StatusBarWidgetFactory {
    override fun getId(): String {
        return "AnsibleLint"
    }

    override fun getDisplayName(): String {
        return message("statusbar.display.name")
    }

    override fun isAvailable(project: Project): Boolean {
        return true
    }

    override fun createWidget(project: Project): StatusBarWidget {
        val ansibleLintStatusBarPopup = AnsibleLintStatusBarPopup(project)

        val gotItTooltip = createGotItTooltip()
        gotItTooltip.show(ansibleLintStatusBarPopup.component, GotItTooltip.TOP_MIDDLE)

        return ansibleLintStatusBarPopup
    }

    override fun disposeWidget(widget: StatusBarWidget) {
    }

    override fun canBeEnabledOn(statusBar: StatusBar): Boolean {
        return true
    }

    private fun createGotItTooltip(): GotItTooltip {
        return GotItTooltip(
            "de.achimonline.ansible_lint.status_bar_popup",
            message("gotitpopup.statusbar.text")
        )
            .withIcon(AllIcons.General.Information)
            .withBrowserLink(
                message("gotitpopup.statusbar.browserlink"),
                URL("https://github.com/4ch1m/intellij-ansible-lint#setup")
            )
    }
}
