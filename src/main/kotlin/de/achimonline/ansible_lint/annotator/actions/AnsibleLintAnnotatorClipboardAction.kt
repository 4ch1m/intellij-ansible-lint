package de.achimonline.ansible_lint.annotator.actions

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.ide.ClipboardSynchronizer
import com.intellij.ide.CopyPasteManagerEx
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.util.ui.TextTransferable
import de.achimonline.ansible_lint.bundle.AnsibleLintBundle.message

class AnsibleLintAnnotatorClipboardAction(private val ruleId: String) : IntentionAction {
    override fun startInWriteAction(): Boolean {
        return false
    }

    override fun getText(): String {
        return "${AnsibleLintAnnotatorActionOrder.CLIPBOARD.value}: ${message("action.copy-rule-id-to-clipboard")}"
    }

    override fun getFamilyName(): String {
        return "AnsibleLint"
    }

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        return true
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        ClipboardSynchronizer
            .getInstance()
            .setContent(
                TextTransferable(ruleId, ruleId),
                CopyPasteManagerEx.getInstanceEx()
            )
    }
}
