package de.achimonline.ansible_lint.annotator

import com.intellij.execution.process.ProcessNotCreatedException
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import de.achimonline.ansible_lint.annotator.actions.AnsibleLintAnnotatorClipboardAction
import de.achimonline.ansible_lint.annotator.actions.AnsibleLintAnnotatorNoQAAction
import de.achimonline.ansible_lint.annotator.actions.AnsibleLintAnnotatorOpenUrlAction
import de.achimonline.ansible_lint.command.AnsibleLintCommandLine
import de.achimonline.ansible_lint.parser.AnsibleLintItem
import de.achimonline.ansible_lint.parser.AnsibleLintParser
import de.achimonline.ansible_lint.bundle.AnsibleLintBundle.message
import de.achimonline.ansible_lint.command.AnsibleLintConfigFile
import de.achimonline.ansible_lint.common.AnsibleLintHelper
import de.achimonline.ansible_lint.notification.AnsibleLintNotification
import de.achimonline.ansible_lint.settings.AnsibleLintConfigurable
import de.achimonline.ansible_lint.settings.AnsibleLintSettingsState
import java.io.IOException
import java.util.concurrent.ExecutionException

private val LOG = logger<AnsibleLintAnnotator>()

class AnsibleLintAnnotator : ExternalAnnotator<AnsibleLintAnnotator.CollectedInformation, List<AnsibleLintItem>>() {
    private val ansibleLintNotification: AnsibleLintNotification = AnsibleLintNotification()

    override fun collectInformation(file: PsiFile, editor: Editor, hasErrors: Boolean): CollectedInformation {
        return CollectedInformation(
            editor,
            file,
            hasErrors
        )
    }

    override fun doAnnotate(collectedInformation: CollectedInformation): List<AnsibleLintItem> {
        if (collectedInformation.hasErrors) return emptyList()

        val project = collectedInformation.editor.project!!
        val settingsState = ApplicationManager.getApplication().getService(AnsibleLintSettingsState::class.java)

        if (settingsState.settings.onlyRunWhenConfigFilePresent && !AnsibleLintConfigFile.exists(project)) {
            return emptyList()
        }

        /**
            NOTE:
            Creating a temporary file (with the current editor content) seems to be the only way to
            speed up (re-)linting after changes occurred.

            check:
            - [How to trigger ExternalAnnotator running immediately after saving the code change?](https://intellij-support.jetbrains.com/hc/en-us/community/posts/360004284939-How-to-trigger-ExternalAnnotator-running-immediately-after-saving-the-code-change-)
            - [Only trigger externalAnnotator when the file system is in sync](https://intellij-support.jetbrains.com/hc/en-us/community/posts/115000337510-Only-trigger-externalAnnotator-when-the-file-system-is-in-sync)
         */
        val tempFolderAndFile = AnsibleLintHelper.createTempFolderAndFile(
            project,
            collectedInformation.file,
            collectedInformation.editor.document.text
        )

        try {
            val lintProcess = AnsibleLintCommandLine(settingsState.settings).createLintProcess(
                project.basePath!!,
                tempFolderAndFile.first.path,
                tempFolderAndFile.second.path
            )

            val output = AnsibleLintCommandLine.getOutput(lintProcess)

            if (AnsibleLintCommandLine.SUCCESS_RETURN_CODES.contains(lintProcess.exitValue())) {
                return AnsibleLintParser.parse(output.first)
            } else {
                LOG.error("Unexpected exit code from 'ansible-lint': ${lintProcess.exitValue()}; ${output.first}|${output.second}")

                ansibleLintNotification.notifyWarning(project, message("notifications.warning.unexpected-return-code"))
            }
        } catch (processNotCreatedException: ProcessNotCreatedException) {
            LOG.error("Unable to create 'ansible-lint' process. [${processNotCreatedException.message}]")

            ansibleLintNotification.notifyError(
                project,
                message("notifications.error.unable-to-execute"),
                object : NotificationAction(message("notifications.action.settings")) {
                    override fun actionPerformed(e: AnActionEvent, notification: Notification) {
                        ShowSettingsUtil.getInstance().showSettingsDialog(project, AnsibleLintConfigurable::class.java)
                    }
                }
            )
        } catch (executionException: ExecutionException) {
            LOG.error("Exception while executing 'ansible-lint': ${executionException.message}")
        } catch (interruptedException: InterruptedException) {
            LOG.error("'ansible-lint' got interrupted during execution: ${interruptedException.message}")
        } finally {
            try {
                tempFolderAndFile.first.deleteRecursively()
            } catch (_: IOException) {
                LOG.warn("Unable to delete temp-folder [${tempFolderAndFile.first.path}].")
            }
        }

        return emptyList()
    }

    override fun apply(file: PsiFile, annotationResult: List<AnsibleLintItem>, holder: AnnotationHolder) {
        annotationResult.forEach {
            if (it.getLine() != Int.MIN_VALUE) {
                val document: Document = PsiDocumentManager.getInstance(file.project).getDocument(file)!!

                val line: Int = it.getLine() - 1
                val startOffset: Int = document.getLineStartOffset(line)
                val endOffset: Int = document.getLineEndOffset(line)
                val text = document.getText(TextRange(startOffset, endOffset))
                val startOffsetDelta = if (it.getColumn() != Int.MIN_VALUE) it.getColumn() else (text.length - text.trimStart().length)

                holder.newAnnotation(
                    getAnnotationHighlightSeverity(it),
                    """
                        ${it.description}
                        ${if (it.content.body.isNotEmpty()) "(${it.content.body})" else ""}
                        |
                        Rule: ${it.check_name}
                    """.trimIndent()
                )
                    .range(TextRange(startOffset + startOffsetDelta, endOffset))
                    .withFix(AnsibleLintAnnotatorOpenUrlAction(it.url))
                    .withFix(AnsibleLintAnnotatorNoQAAction(line, it.check_name))
                    .withFix(AnsibleLintAnnotatorClipboardAction(it.check_name))
                    .create()
            }
        }
    }

    fun getAnnotationHighlightSeverity(ansibleLintItem: AnsibleLintItem): HighlightSeverity {
        when (ansibleLintItem.severity.lowercase().trim()) {
            /**
                based on:
                - [_remap_severity](https://github.com/ansible/ansible-lint/blob/36725c71243a30a9cb63456783362ba71668a76a/src/ansiblelint/formatters/__init__.py#L182)
            */
            "blocker" -> return HighlightSeverity.ERROR
            "critical" -> return HighlightSeverity.ERROR
            "major" -> return HighlightSeverity.WARNING
            "minor" -> return HighlightSeverity.WEAK_WARNING
        }

        return HighlightSeverity.INFORMATION
    }

    data class CollectedInformation(
        val editor: Editor,
        val file: PsiFile,
        val hasErrors: Boolean
    )
}
