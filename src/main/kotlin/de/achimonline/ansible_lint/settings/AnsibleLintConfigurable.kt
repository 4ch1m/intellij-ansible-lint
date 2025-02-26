package de.achimonline.ansible_lint.settings

import com.intellij.execution.ExecutionException
import com.intellij.execution.wsl.WslDistributionManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.util.SystemInfo
import com.intellij.ui.ColorUtil
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.*
import de.achimonline.ansible_lint.bundle.AnsibleLintBundle.message
import de.achimonline.ansible_lint.command.AnsibleLintCommandLine
import de.achimonline.ansible_lint.command.AnsibleLintCommandLineUnix
import de.achimonline.ansible_lint.command.AnsibleLintCommandLineWSL
import de.achimonline.ansible_lint.command.file.AnsibleLintCommandFileConfig
import de.achimonline.ansible_lint.command.file.AnsibleLintCommandFileIgnore
import de.achimonline.ansible_lint.settings.AnsibleLintConfigurable.TestState.*
import javax.swing.JButton
import javax.swing.JEditorPane

private val LOG = logger<AnsibleLintConfigurable>()

class AnsibleLintConfigurable : BoundConfigurable(message("settings.display.name")) {
    private val settings
        get() = AnsibleLintSettingsState.instance.settings

    private lateinit var testButton: JButton
    private lateinit var testStatusText: JEditorPane
    private lateinit var useWsl: Cell<JBCheckBox>

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
                    useWsl = checkBox(message("settings.group.executable.use-wsl"))
                        .comment(message("settings.group.executable.use-wsl.comment"))
                        .enabled(SystemInfo.isWindows)
                        .bindSelected(settings::useWsl)

                    comment("<icon src='AllIcons.General.Warning'>&nbsp;${message("settings.group.executable.use-wsl.experimental")}")
                }

                indent {
                    row {
                        label(message("settings.group.executable.use-wsl.distribution"))

                        if (SystemInfo.isWindows) {
                            val installedWslDistributions =  WslDistributionManager.getInstance().installedDistributions

                            if (settings.wslDistributionId == null) {
                                settings.wslDistributionId = installedWslDistributions.first().id
                            }

                            comboBox(installedWslDistributions.map { it.msId })
                                .bindItem(settings::wslDistributionId.toNullableProperty())
                        } else {
                            comboBox(emptyList())
                        }
                    }
                }.visibleIf(useWsl.selected)

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
                    text(
                        message(
                            "settings.feedback",
                            "https://paypal.me/AchimSeufert",
                            "https://github.com/4ch1m/intellij-ansible-lint",
                            "https://plugins.jetbrains.com/plugin/20905-ansible-lint"
                        )
                    )
                }
            }
        }
    }

    private fun executeTest() {
        apply() // fetch bindings

        testButton.isEnabled = false
        testStatusText.text = ""

        try {
            val versionCheckProcess = if (settings.useWsl) {
                AnsibleLintCommandLineWSL(settings).createVersionCheckProcess()
            } else {
                AnsibleLintCommandLineUnix(settings).createVersionCheckProcess()
            }

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
