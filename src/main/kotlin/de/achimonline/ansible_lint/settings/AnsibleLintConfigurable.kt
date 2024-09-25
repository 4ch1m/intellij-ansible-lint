package de.achimonline.ansible_lint.settings

import com.intellij.execution.ExecutionException
import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.ColorUtil
import com.intellij.ui.JBColor
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import de.achimonline.ansible_lint.bundle.AnsibleLintBundle.message
import de.achimonline.ansible_lint.command.AnsibleLintCommandLine
import de.achimonline.ansible_lint.command.file.AnsibleLintCommandFileConfig
import de.achimonline.ansible_lint.command.file.AnsibleLintCommandFileIgnore
import de.achimonline.ansible_lint.common.AnsibleLintHelper
import de.achimonline.ansible_lint.settings.AnsibleLintConfigurable.TestState.*
import javax.swing.JButton
import javax.swing.JEditorPane

private val LOG = logger<AnsibleLintConfigurable>()

class AnsibleLintConfigurable : BoundConfigurable(message("settings.display.name")) {
    private val settings
        get() = AnsibleLintSettingsState.instance.settings

    private lateinit var testButton: JButton
    private lateinit var testStatusText: JEditorPane

    private val heartIcon = IconLoader.getIcon("/icons/heart-solid.svg", AnsibleLintConfigurable::class.java)

    override fun createPanel(): DialogPanel {
        return panel {
            group(message("settings.group.executable")) {
                row {
                    textFieldWithBrowseButton()
                        .resizableColumn()
                        .align(AlignX.FILL)
                        .label(message("settings.group.executable.command"))
                        .comment(message("settings.group.executable.command.comment"))
                        .bindText(settings::executable)

                    testButton = button(message("settings.group.executable.test")) {
                        executeTest()
                    }.component
                }
                row {
                    testStatusText = comment("").component
                }
            }
            group(message("settings.group.options")) {
                row {
                    checkBox(message("settings.group.options.offline"))
                        .comment(message("settings.group.options.offline.comment"))
                        .bindSelected(settings::offline)
                }
            }
            group(message("settings.group.integration")) {
                row {
                    checkBox(message("settings.group.integration.only-run-when-config-file-present"))
                        .comment(
                            message(
                                "settings.group.integration.only-run-when-config-file-present.comment",
                                AnsibleLintCommandFileConfig.DEFAULT,
                                AnsibleLintCommandFileConfig.ALTERNATIVE
                            )
                        )
                        .bindSelected(settings::onlyRunWhenConfigFilePresent)

                    comment("<icon src='AllIcons.General.Information'>&nbsp;${message("settings.group.integration.only-run-when-config-file-present.recommended")}")
                }
                row {
                    checkBox(message("settings.group.integration.visualize-ignored-rules"))
                        .comment(
                            message(
                                "settings.group.integration.visualize-ignored-rules.comment",
                                AnsibleLintCommandFileIgnore.DEFAULT,
                                AnsibleLintCommandFileIgnore.ALTERNATIVE
                            )
                        )
                        .bindSelected(settings::visualizeIgnoredRules)
                }
                row {
                    checkBox(message("settings.group.integration.lint-excluded-paths"))
                        .comment(message("settings.group.integration.lint-excluded-paths.comment"))
                        .bindSelected(settings::lintFilesInsideExcludedPaths)
                }
            }
            group {
                row {
                    icon(heartIcon)
                    text(message("settings.donation", "https://paypal.me/AchimSeufert"))
                }
            }
        }
    }

    private fun executeTest() {
        apply() // fetch bindings

        testButton.isEnabled = false
        testStatusText.text = ""

        try {
            val projectBasePath = AnsibleLintHelper.getProjectBasePath(ProjectUtil.getActiveProject()!!)

            val versionCheckProcess = AnsibleLintCommandLine(settings).createVersionCheckProcess(projectBasePath)
            val versionCheckProcessResult = AnsibleLintCommandLine.ProcessResult(versionCheckProcess)

            val versionString = versionCheckProcessResult
                .stdout
                .split(System.lineSeparator())
                .first()
                .trim()

            if (versionCheckProcessResult.rc == 0 && versionString.contains("ansible-lint")) {
                val versions = AnsibleLintCommandLine.getVersions(versionString)

                if (versions != null) {
                    if (versions.first < AnsibleLintCommandLine.MIN_EXECUTABLE_VERSION) {
                        testStatusText.text = updateTestStatus(
                            WARNING,
                            message(
                                "settings.group.executable.test.old-version",
                                versions.first,
                                AnsibleLintCommandLine.MIN_EXECUTABLE_VERSION
                            )
                        )
                    } else {
                        testStatusText.text = updateTestStatus(SUCCESS, versionString)
                    }
                } else {
                    logExecutableTest("Unable to parse version from: \"${versionString}\"")
                    testStatusText.text = updateTestStatus(
                        WARNING,
                        message("settings.group.executable.test.unable-to-parse-version")
                    )
                }
            } else {
                logExecutableTest("Unexpected return code/result: [${versionCheckProcessResult.rc}] - ${versionCheckProcessResult.stdout} | ${versionCheckProcessResult.stderr}")
                testStatusText.text = updateTestStatus(
                    FAILURE,
                    message("settings.group.executable.test.exit-value", versionCheckProcess.exitValue())
                )
            }
        } catch (executionException: ExecutionException) {
            logExecutableTest("Execution exception: $executionException")
            testStatusText.text = updateTestStatus(
                FAILURE,
                message("settings.group.executable.test.exception")
            )
        } catch (interruptedException: InterruptedException) {
            logExecutableTest("Interrupted exception: $interruptedException")
            testStatusText.text = updateTestStatus(
                FAILURE,
                message("settings.group.executable.test.interrupted")
            )
        } finally {
            testButton.isEnabled = true
        }
    }

    private fun updateTestStatus(state: TestState, text: String): String {
        val icon: String
        val textColor: String

        when (state) {
            SUCCESS -> {
                icon = "AllIcons.General.InspectionsOK"
                textColor = ColorUtil.toHtmlColor(JBColor.GREEN)
            }
            WARNING -> {
                icon = "AllIcons.General.NotificationWarning"
                textColor = ColorUtil.toHtmlColor(JBColor.YELLOW)
            }
            FAILURE -> {
                icon = "AllIcons.General.NotificationError"
                textColor = ColorUtil.toHtmlColor(JBColor.RED)
            }
        }

        return """
            <icon src='${icon}'>
            &nbsp;
            <span style='color: ${textColor}'>${text}</span>
        """.trimIndent()
    }

    private enum class TestState {
        SUCCESS,
        WARNING,
        FAILURE
    }

    private fun logExecutableTest(message: String) {
        LOG.info("Executable test | $message")
    }
}
