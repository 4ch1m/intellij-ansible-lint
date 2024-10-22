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
import de.achimonline.ansible_lint.annotator.AnsibleLintAnnotator.ApplicableInformation
import de.achimonline.ansible_lint.annotator.AnsibleLintAnnotator.CollectedInformation
import de.achimonline.ansible_lint.annotator.actions.*
import de.achimonline.ansible_lint.bundle.AnsibleLintBundle.message
import de.achimonline.ansible_lint.command.AnsibleLintCommandLine
import de.achimonline.ansible_lint.command.AnsibleLintCommandLineUnix
import de.achimonline.ansible_lint.command.AnsibleLintCommandLineWSL
import de.achimonline.ansible_lint.command.AnsibleLintCommandLineWSL.WSLPathException
import de.achimonline.ansible_lint.command.file.AnsibleLintCommandFileConfig
import de.achimonline.ansible_lint.command.file.AnsibleLintCommandFileIgnore
import de.achimonline.ansible_lint.common.AnsibleLintHelper
import de.achimonline.ansible_lint.common.AnsibleLintTempEnv
import de.achimonline.ansible_lint.notification.AnsibleLintNotification
import de.achimonline.ansible_lint.parser.AnsibleLintItem
import de.achimonline.ansible_lint.parser.AnsibleLintParser
import de.achimonline.ansible_lint.settings.AnsibleLintConfigurable
import de.achimonline.ansible_lint.settings.AnsibleLintSettings
import de.achimonline.ansible_lint.settings.AnsibleLintSettingsState
import java.io.File
import java.nio.file.Paths
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

        if (!settingsState.settings.lintFilesInsideExcludedPaths) {
            val relativeDirectoryPath = Paths.get(relativeFilePath).parent

            if (relativeDirectoryPath != null) {
                if (AnsibleLintCommandFileConfig(project).getExcludePaths().any { relativeDirectoryPath.pathString.startsWith(it) }) {
                    return ApplicableInformation()
                }
            }
        }

        val ignores = AnsibleLintCommandFileIgnore(project).parse()

        /**
        NOTE:

        Creating a temporary file (with the current editor content) seems to be the only way to
        speed up (re-)linting after changes occurred.

        check:
        - [How to trigger ExternalAnnotator running immediately after saving the code change?](https://intellij-support.jetbrains.com/hc/en-us/community/posts/360004284939-How-to-trigger-ExternalAnnotator-running-immediately-after-saving-the-code-change-)
        - [Only trigger externalAnnotator when the file system is in sync](https://intellij-support.jetbrains.com/hc/en-us/community/posts/115000337510-Only-trigger-externalAnnotator-when-the-file-system-is-in-sync)

         That's why this workaround has to be implemented/used. :-(
         */
        val tempEnv = AnsibleLintTempEnv(
            projectBasePath = projectBasePath,
            fileToLint = collectedInformation.file,
            fileContent = collectedInformation.editor.document.text
        )

        try {
            val ansibleLintCommandLine: AnsibleLintCommandLine = if (settingsState.settings.useWsl) {
                AnsibleLintCommandLineWSL(settingsState.settings, projectBasePath)
            } else {
                AnsibleLintCommandLineUnix(settingsState.settings)
            }

            val lintProcess = ansibleLintCommandLine.createLintProcess(
                    workingDirectory = projectBasePath,
                    projectDirectory = tempEnv.directory.path,
                    configFile = configFile?.absolutePath,
                    yamlFilePath = tempEnv.file.path
                )

            val lintProcessResult = AnsibleLintCommandLine.ProcessResult(lintProcess)

            if (AnsibleLintCommandLine.SUCCESS_RETURN_CODES.contains(lintProcessResult.rc)) {
                return ApplicableInformation(
                    settings = settingsState.settings,
                    lintItems = AnsibleLintParser.parse(lintProcessResult.stdout),
                    lintIgnores = ignores[relativeFilePath]?.toSet() ?: emptySet()
                )
            } else {
                LOG.error("Unexpected exit code from 'ansible-lint': ${lintProcessResult.rc}; ${lintProcessResult.stdout}|${lintProcessResult.stderr}")

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
        } catch (wslPathException: WSLPathException) {
            LOG.error("WSL Exception: ${wslPathException.message}")
        } catch (executionException: ExecutionException) {
            LOG.error("Exception while executing 'ansible-lint': ${executionException.message}")
        } catch (interruptedException: InterruptedException) {
            LOG.error("'ansible-lint' got interrupted during execution: ${interruptedException.message}")
        } finally {
            if (!tempEnv.purge()) {
                LOG.warn("Failed to remove temporary folder [${tempEnv.directory}].")
            }
        }

        return ApplicableInformation()
    }

    override fun apply(file: PsiFile, applicableInformation: ApplicableInformation, holder: AnnotationHolder) {
        applicableInformation.lintItems.forEach { lintItem ->
            val itemIsIgnored = applicableInformation.lintIgnores.contains(lintItem.ruleId)

            // skip ignored items; if specified via settings
            if (!applicableInformation.settings.visualizeIgnoredRules && itemIsIgnored) return@forEach

            val document: Document = PsiDocumentManager.getInstance(file.project).getDocument(file)!!

            val line: Int = lintItem.startLine - 1
            val startOffset: Int = document.getLineStartOffset(line)
            val endOffset: Int = document.getLineEndOffset(line)
            val text = document.getText(TextRange(startOffset, endOffset))
            val startOffsetDelta = text.length - text.trimStart().length

            var annotationBuilder = holder.newAnnotation(
                if (itemIsIgnored) HighlightSeverity.WEAK_WARNING else lintItem.severity,
                getAnnotationMessage(lintItem, itemIsIgnored)
            )
                .range(TextRange(startOffset + startOffsetDelta, endOffset))
                .withFix(AnsibleLintAnnotatorOpenUrlAction(lintItem.helpUri))
                .withFix(AnsibleLintAnnotatorNoQAAction(line, lintItem.ruleId))
                .withFix(AnsibleLintAnnotatorClipboardAction(lintItem.ruleId))
                .withFix(AnsibleLintAnnotatorSkipListAction(lintItem.ruleId))

            if (!itemIsIgnored) {
                annotationBuilder = annotationBuilder.withFix(AnsibleLintAnnotatorIgnoreFileAction(lintItem.ruleId))
            }

            annotationBuilder.create()
        }
    }

    fun getAnnotationMessage(ansibleLintItem: AnsibleLintItem, ignored: Boolean): String {
        return (
            (if (ignored) "${message("annotation.ignored-prefix")} " else "") +
            ("${ansibleLintItem.description} ") +
            (if (ansibleLintItem.message.isNotEmpty()) "| ${ansibleLintItem.message} " else "") +
            (if (ansibleLintItem.helpText.isNotEmpty()) "(${ansibleLintItem.helpText}) " else "") +
            ("| ${message("annotation.rule-id-prefix")} ${ansibleLintItem.ruleId}") +
            (if (ansibleLintItem.tags.isNotEmpty()) " <${ansibleLintItem.tags.joinToString(",")}>" else "")
        )
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
