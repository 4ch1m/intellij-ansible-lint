package de.achimonline.ansible_lint.command

import io.github.z4kn4fein.semver.Version
import org.junit.Assert.*
import org.junit.Test

class AnsibleLintCommandLineTest {
    @Test
    fun getVersions() {
        val versions = AnsibleLintCommandLine.getVersions("ansible-lint 6.14.3 using ansible 2.14.1")

        assertEquals(
            Version(
                major = 6,
                minor = 14,
                patch = 3
            ),
            versions!!.first
        )

        assertEquals(
            Version(
                major = 2,
                minor = 14,
                patch = 1
            ),
            versions.second
        )
    }

    @Test
    fun getVersions_fail() {
        assertNull(AnsibleLintCommandLine.getVersions("test"))
    }
}
