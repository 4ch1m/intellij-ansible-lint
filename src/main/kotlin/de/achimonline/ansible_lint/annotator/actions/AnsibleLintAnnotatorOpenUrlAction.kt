package de.achimonline.ansible_lint.annotator.actions

import com.intellij.codeInsight.intention.PriorityAction.Priority
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import de.achimonline.ansible_lint.bundle.AnsibleLintBundle.message

class AnsibleLintAnnotatorOpenUrlAction(private val url: String) : AnsibleLintAnnotatorAction(message("action.show-detailed-information-online")) {
    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        BrowserUtil.browse(url)
    }

    override fun getPriority(): Priority {
        return Priority.TOP
    }
}
