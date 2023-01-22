package de.achimonline.ansible_lint.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
    name = "de.achimonline.ansible_lint.settings.AnsibleLintSettingsState",
    storages = [Storage("AnsibleLint.xml")]
)
class AnsibleLintSettingsState : PersistentStateComponent<AnsibleLintSettingsState?> {
    var settings = AnsibleLintSettings()

    override fun getState(): AnsibleLintSettingsState {
        return this
    }

    override fun loadState(state: AnsibleLintSettingsState) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        val instance: AnsibleLintSettingsState
            get() = ApplicationManager.getApplication().getService(AnsibleLintSettingsState::class.java)
    }
}
