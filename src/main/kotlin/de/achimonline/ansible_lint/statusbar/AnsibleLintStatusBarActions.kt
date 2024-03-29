package de.achimonline.ansible_lint.statusbar

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import de.achimonline.ansible_lint.bundle.AnsibleLintBundle.message
import de.achimonline.ansible_lint.command.file.AnsibleLintCommandFileConfig
import de.achimonline.ansible_lint.notification.AnsibleLintNotification

class AnsibleLintStatusBarActions {
    class CreateConfig(
        text: String,
        private val project: Project,
        private val filePath: String
    ) : AnAction(text) {
        override fun actionPerformed(event: AnActionEvent) {
            val configFile = AnsibleLintCommandFileConfig(project).locateOrCreate(filePath)
            val virtualConfigFile = VfsUtil.findFileByIoFile(configFile, true)

            FileEditorManager
                .getInstance(project)
                .openTextEditor(OpenFileDescriptor(project, virtualConfigFile!!), true)

            AnsibleLintNotification().notifyInformation(project, message("action.info.config-file-created"))
        }
    }
}
