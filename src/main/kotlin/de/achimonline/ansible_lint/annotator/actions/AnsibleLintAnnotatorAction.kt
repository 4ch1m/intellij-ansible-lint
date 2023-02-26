package de.achimonline.ansible_lint.annotator.actions

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.PriorityAction
import com.intellij.codeInsight.intention.PriorityAction.Priority
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

abstract class AnsibleLintAnnotatorAction(private val text: String) : IntentionAction, PriorityAction {
    override fun getFamilyName(): String {
        return "AnsibleLint"
    }

    override fun getText(): String {
        return text
    }

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        return true
    }

    override fun startInWriteAction(): Boolean {
        return false
    }

    override fun getPriority(): Priority {
        return Priority.NORMAL
    }
}
