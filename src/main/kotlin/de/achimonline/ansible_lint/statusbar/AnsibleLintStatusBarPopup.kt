package de.achimonline.ansible_lint.statusbar

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.ListPopup
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.impl.status.EditorBasedStatusBarPopup
import de.achimonline.ansible_lint.bundle.AnsibleLintBundle.message
import de.achimonline.ansible_lint.command.file.AnsibleLintCommandFileConfig
import de.achimonline.ansible_lint.settings.AnsibleLintSettingsState
import kotlin.io.path.pathString

class AnsibleLintStatusBarPopup(project: Project) : EditorBasedStatusBarPopup(project, false) {
    private val actionGroup: DefaultActionGroup = DefaultActionGroup()

    init {
        actionGroup.add(
            AnsibleLintStatusBarActions.CreateConfig(
                message(
                    "statusbar.action.text",
                    AnsibleLintCommandFileConfig.DEFAULT
                ),
                project,
                AnsibleLintCommandFileConfig.DEFAULT
            )
        )
        actionGroup.add(
            AnsibleLintStatusBarActions.CreateConfig(
                message(
                    "statusbar.action.text",
                    AnsibleLintCommandFileConfig.ALTERNATIVE
                ),
                project,
                AnsibleLintCommandFileConfig.ALTERNATIVE
            )
        )
    }

    override fun ID(): String {
        return "AnsibleLint"
    }

    override fun getWidgetState(file: VirtualFile?): WidgetState {
        val settingsState = ApplicationManager.getApplication().getService(AnsibleLintSettingsState::class.java)

        return if (!isYamlFile(file) ||
                   !settingsState.settings.onlyRunWhenConfigFilePresent ||
                   AnsibleLintCommandFileConfig(project).locate() != null) {
            WidgetState.HIDDEN
        } else {
            WidgetState(
                message("statusbar.widget.tooltip"),
                message("statusbar.widget.text"),
                true
            )
        }
    }

    override fun createPopup(context: DataContext): ListPopup {
        @Suppress("DialogTitleCapitalization")
        return JBPopupFactory.getInstance().createActionGroupPopup(
            message("statusbar.popup.title"),
            actionGroup,
            context,
            JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
            false
        )
    }

    override fun createInstance(project: Project): StatusBarWidget {
        return AnsibleLintStatusBarPopup(project)
    }

    override fun isEnabledForFile(file: VirtualFile?): Boolean {
        return isYamlFile(file)
    }

    private fun isYamlFile(file: VirtualFile?): Boolean {
        if (file != null) {
            try {
                val filePath = file.toNioPath().pathString

                return listOf(".yaml", ".yml").any {
                    filePath.lowercase().endsWith(it)
                }
            } catch (_: UnsupportedOperationException) {
                // do nothing
            }
        }

        return false
    }
}
