package de.achimonline.ansible_lint.settings

data class AnsibleLintSettings(
    var executable: String = "ansible-lint",
    var offline: Boolean = false,
    var onlyRunWhenConfigFilePresent: Boolean = true,
    var visualizeIgnoredRules: Boolean = true,
    var lintFilesInsideExcludedPaths: Boolean = false,
    var useWsl: Boolean = false,
    var wslDistributionId: String? = null
)
