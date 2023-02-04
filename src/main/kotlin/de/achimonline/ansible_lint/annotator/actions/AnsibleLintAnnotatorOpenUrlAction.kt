package de.achimonline.ansible_lint.annotator.actions

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import de.achimonline.ansible_lint.bundle.AnsibleLintBundle.message

class AnsibleLintAnnotatorOpenUrlAction(private val url: String) : IntentionAction {
    override fun startInWriteAction(): Boolean {
        return false
    }

    override fun getText(): String {
        return "${AnsibleLintAnnotatorActionOrder.OPEN_URL.value}: ${message("action.show-detailed-information-online")}"
    }

    override fun getFamilyName(): String {
        return "AnsibleLint"
    }

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        return true
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        BrowserUtil.browse(url)
    }
}
