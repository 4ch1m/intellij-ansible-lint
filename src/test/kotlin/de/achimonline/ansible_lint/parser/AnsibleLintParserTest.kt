package de.achimonline.ansible_lint.parser

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class AnsibleLintParserTest {
    private fun getFile(name: String): File = File(javaClass.getResource("/${name}")!!.file)

    @Test
    fun parse() {
        val json = getFile("linter_output.json").readText()

        val parsed = AnsibleLintParser.parse(json)

        assertEquals(3, parsed.size)

        assertEquals("issue", parsed[0].type)
        assertEquals("name[casing]", parsed[0].check_name)
        assertEquals(1, parsed[0].categories.size)
        assertEquals("https://ansible-lint.readthedocs.io/rules/name/", parsed[0].url)
        assertEquals("major", parsed[0].severity)
        assertEquals("warning", parsed[0].level)
        assertEquals("All names should start with an uppercase letter.", parsed[0].description)
        assertEquals("877a87298cbbb525c454904f68eb41007a92b47aa47572da599cf6c5eec70554", parsed[0].fingerprint)
        assertEquals("test.yml", parsed[0].location.path)
        assertEquals(1, parsed[0].getLine())
        assertEquals(Integer.MIN_VALUE, parsed[0].getColumn())
        assertEquals("", parsed[0].content.body)

        assertEquals(2, parsed[1].categories.size)
        assertEquals("Task/Handler: test", parsed[1].content.body)

        assertEquals(666, parsed[2].getLine())
        assertEquals(42, parsed[2].getColumn())
    }
}
