package de.achimonline.ansible_lint.settings

import com.intellij.execution.ExecutionException
import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.ColorUtil
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import de.achimonline.ansible_lint.bundle.AnsibleLintBundle.message
import de.achimonline.ansible_lint.command.AnsibleLintCommandLine
import de.achimonline.ansible_lint.command.file.AnsibleLintCommandFileConfig
import de.achimonline.ansible_lint.command.file.AnsibleLintCommandFileIgnore
import de.achimonline.ansible_lint.common.AnsibleLintHelper
import javax.swing.JButton
import javax.swing.JEditorPane

class AnsibleLintConfigurable : BoundConfigurable(message("settings.display.name")) {
    private val settings
        get() = AnsibleLintSettingsState.instance.settings

    private lateinit var testButton: JButton
    private lateinit var testStatus: JEditorPane

    private val heartIcon = IconLoader.getIcon("/icons/heart-solid.svg", AnsibleLintConfigurable::class.java)

    override fun createPanel(): DialogPanel {
        return panel {
            group(message("settings.group.executable")) {
                row {
                    textField()
                        .label(message("settings.group.executable.command"))
                        .comment(message("settings.group.executable.command.comment"))
                        .bindText(settings::executable)

                    testButton = button(message("settings.group.executable.test")) {
                        executeTest()
                    }.component
                }
                row {
                    testStatus = comment("").component
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
                            message("settings.group.integration.only-run-when-config-file-present.comment",
                                AnsibleLintCommandFileConfig.DEFAULT,
                                AnsibleLintCommandFileConfig.ALTERNATIVE))
                        .bindSelected(settings::onlyRunWhenConfigFilePresent)

                    comment("<icon src='AllIcons.General.Information'>&nbsp;${message("settings.group.integration.only-run-when-config-file-present.recommended")}")
                }
                row {
                    checkBox(message("settings.group.integration.visualize-ignored-rules"))
                        .comment(message("settings.group.integration.visualize-ignored-rules.comment",
                            AnsibleLintCommandFileIgnore.DEFAULT,
                            AnsibleLintCommandFileIgnore.ALTERNATIVE))
                        .bindSelected(settings::visualizeIgnoredRules)
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
        testStatus.text = ""

        try {
            val projectBasePath = AnsibleLintHelper.getProjectBasePath(ProjectUtil.getActiveProject()!!)
            val versionCheckProcess = AnsibleLintCommandLine(settings).createVersionCheckProcess(projectBasePath)
            val output = AnsibleLintCommandLine.getOutput(versionCheckProcess)

            if (versionCheckProcess.exitValue() == 0 && output.first.contains("ansible-lint")) {
                testStatus.text = updateTestStatus(true, output.first.split("\n")[0].trim())
            } else {
                testStatus.text = updateTestStatus(
                    false,
                    message("settings.group.executable.test.exit-value", versionCheckProcess.exitValue())
                )
            }
        } catch (executionException: ExecutionException) {
            testStatus.text = updateTestStatus(false, message("settings.group.executable.test.exception"))
        } catch (interruptedException: InterruptedException) {
            testStatus.text = updateTestStatus(false, message("settings.group.executable.test.interrupted"))
        } finally {
            testButton.isEnabled = true
        }
    }

    private fun updateTestStatus(success: Boolean, text: String): String {
        return """
            <icon src='${if (success) "AllIcons.General.InspectionsOK" else "AllIcons.General.InspectionsError"}'>
            &nbsp;
            <span style='color: ${ColorUtil.toHtmlColor(if (success) SimpleTextAttributes.GRAY_ATTRIBUTES.fgColor else SimpleTextAttributes.ERROR_ATTRIBUTES.fgColor)}'>
                $text
            </span>
        """.trimIndent()
    }
}
