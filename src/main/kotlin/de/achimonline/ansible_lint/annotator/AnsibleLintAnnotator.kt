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
import de.achimonline.ansible_lint.annotator.AnsibleLintAnnotator.*
import de.achimonline.ansible_lint.annotator.actions.*
import de.achimonline.ansible_lint.command.AnsibleLintCommandLine
import de.achimonline.ansible_lint.parser.AnsibleLintItem
import de.achimonline.ansible_lint.parser.AnsibleLintParser
import de.achimonline.ansible_lint.bundle.AnsibleLintBundle.message
import de.achimonline.ansible_lint.command.file.AnsibleLintCommandFileConfig
import de.achimonline.ansible_lint.command.file.AnsibleLintCommandFileIgnore
import de.achimonline.ansible_lint.common.AnsibleLintHelper
import de.achimonline.ansible_lint.notification.AnsibleLintNotification
import de.achimonline.ansible_lint.settings.AnsibleLintConfigurable
import de.achimonline.ansible_lint.settings.AnsibleLintSettings
import de.achimonline.ansible_lint.settings.AnsibleLintSettingsState
import java.io.File
import java.io.IOException
import java.util.concurrent.ExecutionException
import kotlin.io.path.pathString

private val LOG = logger<AnsibleLintAnnotator>()

class AnsibleLintAnnotator : ExternalAnnotator<CollectedInformation, ApplicableInformation>() {
    private val ansibleLintNotification: AnsibleLintNotification = AnsibleLintNotification()

    override fun collectInformation(file: PsiFile, editor: Editor, hasErrors: Boolean): CollectedInformation {
        return CollectedInformation(
            editor,
            file,
            hasErrors
        )
    }

    override fun doAnnotate(collectedInformation: CollectedInformation): ApplicableInformation {
        if (collectedInformation.hasErrors) return ApplicableInformation()

        val project = collectedInformation.editor.project!!
        val settingsState = ApplicationManager.getApplication().getService(AnsibleLintSettingsState::class.java)
        val configFile = AnsibleLintCommandFileConfig(project).locate()

        if (settingsState.settings.onlyRunWhenConfigFilePresent && configFile == null) return ApplicableInformation()

        val projectBasePath = AnsibleLintHelper.getProjectBasePath(project)
        val relativeFilePath = collectedInformation.file.virtualFile.toNioPath().pathString.removePrefix(projectBasePath).removePrefix(File.separator)
        val ignores = AnsibleLintCommandFileIgnore(project).parse()

        /**
            NOTE:
            Creating a temporary file (with the current editor content) seems to be the only way to
            speed up (re-)linting after changes occurred.

            check:
            - [How to trigger ExternalAnnotator running immediately after saving the code change?](https://intellij-support.jetbrains.com/hc/en-us/community/posts/360004284939-How-to-trigger-ExternalAnnotator-running-immediately-after-saving-the-code-change-)
            - [Only trigger externalAnnotator when the file system is in sync](https://intellij-support.jetbrains.com/hc/en-us/community/posts/115000337510-Only-trigger-externalAnnotator-when-the-file-system-is-in-sync)
         */
        val tempFolderAndFile = AnsibleLintHelper.createTempFolderAndFile(
            projectBasePath = projectBasePath,
            file = collectedInformation.file,
            content = collectedInformation.editor.document.text
        )

        try {
            val lintProcess = AnsibleLintCommandLine(settingsState.settings).createLintProcess(
                workingDirectory = projectBasePath,
                projectDirectory = tempFolderAndFile.first.path,
                configFile = configFile?.absolutePath,
                yamlFilePath = tempFolderAndFile.second.path
            )

            val output = AnsibleLintCommandLine.getOutput(lintProcess)

            if (AnsibleLintCommandLine.SUCCESS_RETURN_CODES.contains(lintProcess.exitValue())) {
                return ApplicableInformation(
                    settings = settingsState.settings,
                    lintItems = AnsibleLintParser.parse(output.first),
                    lintIgnores = ignores[relativeFilePath]?.toSet() ?: emptySet()
                )
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

        return ApplicableInformation()
    }

    override fun apply(file: PsiFile, applicableInformation: ApplicableInformation, holder: AnnotationHolder) {
        applicableInformation.lintItems.forEach { lintItem ->
            val itemIsIgnored = applicableInformation.lintIgnores.contains(lintItem.check_name)

            // skip ignored items; if specified via settings
            if (!applicableInformation.settings.visualizeIgnoredRules && itemIsIgnored) return@forEach

            // skip items with no line information
            if (lintItem.getLine() == Int.MIN_VALUE) return@forEach

            val document: Document = PsiDocumentManager.getInstance(file.project).getDocument(file)!!

            val line: Int = lintItem.getLine() - 1
            val startOffset: Int = document.getLineStartOffset(line)
            val endOffset: Int = document.getLineEndOffset(line)
            val text = document.getText(TextRange(startOffset, endOffset))
            val startOffsetDelta = if (lintItem.getColumn() != Int.MIN_VALUE) lintItem.getColumn() else (text.length - text.trimStart().length)

            var annotationBuilder = holder.newAnnotation(
                if (itemIsIgnored) HighlightSeverity.WEAK_WARNING else getAnnotationHighlightSeverity(lintItem),
                getAnnotationMessage(lintItem, itemIsIgnored)
            )
                .range(TextRange(startOffset + startOffsetDelta, endOffset))
                .withFix(AnsibleLintAnnotatorOpenUrlAction(lintItem.url))
                .withFix(AnsibleLintAnnotatorNoQAAction(line, lintItem.check_name))
                .withFix(AnsibleLintAnnotatorClipboardAction(lintItem.check_name))
                .withFix(AnsibleLintAnnotatorSkipListAction(lintItem.check_name))

            if (!itemIsIgnored) {
                annotationBuilder = annotationBuilder.withFix(AnsibleLintAnnotatorIgnoreFileAction(lintItem.check_name))
            }

            annotationBuilder.create()
        }
    }

    fun getAnnotationHighlightSeverity(ansibleLintItem: AnsibleLintItem): HighlightSeverity {
        when (ansibleLintItem.severity.lowercase().trim()) {
            /**
                based on:
                - [_remap_severity](https://github.com/ansible/ansible-lint/blob/36725c71243a30a9cb63456783362ba71668a76a/src/ansiblelint/formatters/__init__.py#L182)
                - [_remap_severity](https://github.com/ansible/ansible-lint/blob/8b842129750f5dc789a53b4e9372f6b4f82264ce/src/ansiblelint/formatters/__init__.py#L180)
            */
            "blocker" -> return HighlightSeverity.ERROR
            "critical" -> return HighlightSeverity.ERROR
            "major" -> return HighlightSeverity.ERROR
            "minor" -> return HighlightSeverity.WARNING
        }

        return HighlightSeverity.INFORMATION
    }

    fun getAnnotationMessage(ansibleLintItem: AnsibleLintItem, ignored: Boolean): String {
        return """
            ${if (ignored) "${message("annotation.ignored-prefix")} " else ""}
            ${ansibleLintItem.description}
            ${if (ansibleLintItem.content.body.isNotEmpty()) "(${ansibleLintItem.content.body})" else ""}
            |
            Rule: ${ansibleLintItem.check_name}
        """.trimIndent()
    }

    data class CollectedInformation(
        val editor: Editor,
        val file: PsiFile,
        val hasErrors: Boolean
    )

    data class ApplicableInformation(
        val settings: AnsibleLintSettings = AnsibleLintSettings(),
        val lintItems: List<AnsibleLintItem> = emptyList(),
        val lintIgnores: Set<String> = emptySet()
    )
}
