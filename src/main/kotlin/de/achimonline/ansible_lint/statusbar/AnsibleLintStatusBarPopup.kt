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
import de.achimonline.ansible_lint.command.AnsibleLintConfigFile
import de.achimonline.ansible_lint.command.AnsibleLintConfigFile.Companion.CONFIG_DIR_NAME
import de.achimonline.ansible_lint.command.AnsibleLintConfigFile.Companion.CONFIG_FILE_NAME
import de.achimonline.ansible_lint.command.AnsibleLintConfigFile.Companion.CONFIG_FILE_NAME_ALTERNATIVE
import de.achimonline.ansible_lint.settings.AnsibleLintSettingsState
import java.io.File
import kotlin.io.path.pathString

class AnsibleLintStatusBarPopup(project: Project) : EditorBasedStatusBarPopup(project, false) {
    private val actionGroup: DefaultActionGroup = DefaultActionGroup()

    init {
        actionGroup.add(
            AnsibleLintStatusBarActions.CreateConfig(
                message(
                    "statusbar.action.text",
                    CONFIG_FILE_NAME
                ),
                project
            )
        )
        actionGroup.add(
            AnsibleLintStatusBarActions.CreateConfig(
                message(
                    "statusbar.action.text",
                    "$CONFIG_DIR_NAME${File.separator}$CONFIG_FILE_NAME_ALTERNATIVE"
                ),
                project,
                true
            )
        )
    }

    override fun ID(): String {
        return "AnsibleLint"
    }

    override fun getWidgetState(file: VirtualFile?): WidgetState {
        val settingsState = ApplicationManager.getApplication().getService(AnsibleLintSettingsState::class.java)

        if (
            !settingsState.settings.onlyRunWhenConfigFilePresent ||
            AnsibleLintConfigFile.exists(project)
        ) {
            return WidgetState.HIDDEN
        } else {
            @Suppress("DialogTitleCapitalization")
            return WidgetState(
                message("statusbar.widget.tooltip"),
                message("statusbar.widget.text"),
                true
            )
        }
    }

    override fun createPopup(context: DataContext?): ListPopup {
        @Suppress("DialogTitleCapitalization")
        return JBPopupFactory.getInstance().createActionGroupPopup(
            message("statusbar.popup.title"),
            actionGroup,
            context!!,
            JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
            false
        )
    }

    override fun createInstance(project: Project): StatusBarWidget {
        return AnsibleLintStatusBarPopup(project)
    }

    override fun isEnabledForFile(file: VirtualFile?): Boolean {
        if (file != null) {
            return listOf(".yaml", ".yml").any {
                file.toNioPath().pathString.lowercase().endsWith(it)
            }
        }

        return false
    }
}
