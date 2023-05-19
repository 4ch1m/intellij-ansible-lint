package de.achimonline.ansible_lint.command

import io.github.z4kn4fein.semver.Version
import org.junit.Assert.*
import org.junit.Test

class AnsibleLintCommandLineTest {
    @Test
    fun getVersions() {
        listOf(
            Triple(
                "ansible-lint 6.14.3 using ansible 2.14.1",
                Version(6, 14, 3),
                Version(2, 14, 1)
            ),
            Triple(
                "ansible-lint 6.16.1 using ansible-core:2.14.1 ruamel-yaml:0.17.21 ruamel-yaml-clib:0.2.7",
                Version(6, 16, 1),
                Version(2, 14, 1)
            )
        ).forEach { (versionString, expectedAnsibleLintVersion, expectedAnsibleVersion) ->
            val parsedVersions = AnsibleLintCommandLine.getVersions(versionString)

            assertNotNull(parsedVersions)

            if (parsedVersions != null) {
                val (actualAnsibleLintVersion, actualAnsibleVersion) = parsedVersions

                assertEquals(expectedAnsibleLintVersion, actualAnsibleLintVersion)
                assertEquals(expectedAnsibleVersion, actualAnsibleVersion)
            }
        }
    }

    @Test
    fun getVersions_fail() {
        assertNull(AnsibleLintCommandLine.getVersions("test"))
    }
}
