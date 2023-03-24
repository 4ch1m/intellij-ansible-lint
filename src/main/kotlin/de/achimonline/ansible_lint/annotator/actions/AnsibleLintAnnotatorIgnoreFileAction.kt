package de.achimonline.ansible_lint.annotator.actions

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.intention.PriorityAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import de.achimonline.ansible_lint.bundle.AnsibleLintBundle.message
import de.achimonline.ansible_lint.command.file.AnsibleLintCommandFileIgnore

class AnsibleLintAnnotatorIgnoreFileAction(private val rule: String) :
    AnsibleLintAnnotatorAction(message("action.add-rule-id-to-ignore-file")) {
    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        file?.let {
            AnsibleLintCommandFileIgnore(project).addRule(it, rule)
            DaemonCodeAnalyzer.getInstance(project).restart()
        }
    }

    override fun getPriority(): PriorityAction.Priority {
        return PriorityAction.Priority.HIGH
    }
}
