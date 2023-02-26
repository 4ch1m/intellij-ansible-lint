package de.achimonline.ansible_lint.annotator.actions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import de.achimonline.ansible_lint.bundle.AnsibleLintBundle.message

class AnsibleLintAnnotatorNoQAAction(
    private val line: Int,
    private val ruleId: String
) : AnsibleLintAnnotatorAction(message("action.disable-rule-check-using-noqa")) {
    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (editor != null) {
            val lineStartOffset = editor.document.getLineStartOffset(line)
            val lineEndOffset = editor.document.getLineEndOffset(line)
            val lineRange = TextRange(lineStartOffset, lineEndOffset)
            val lineText = editor.document.getText(lineRange)

            val newLineText = if (EXISTING_NO_QA_REGEX.matches(lineText)) {
                "${lineText.trimEnd()} $ruleId"
            } else {
                "${lineText.trimEnd()} # noqa: $ruleId"
            }

            editor.document.replaceString(lineStartOffset, lineEndOffset, newLineText)
        }
    }

    override fun startInWriteAction(): Boolean {
        return true
    }

    companion object {
        val EXISTING_NO_QA_REGEX = "(.*)# noqa:?(.*)$".toRegex()
    }
}
