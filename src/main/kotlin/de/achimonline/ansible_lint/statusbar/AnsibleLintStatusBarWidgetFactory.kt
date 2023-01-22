package de.achimonline.ansible_lint.statusbar

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import de.achimonline.ansible_lint.bundle.AnsibleLintBundle.message

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
        return AnsibleLintStatusBarPopup(project)
    }

    override fun disposeWidget(widget: StatusBarWidget) {
    }

    override fun canBeEnabledOn(statusBar: StatusBar): Boolean {
        return true
    }
}
