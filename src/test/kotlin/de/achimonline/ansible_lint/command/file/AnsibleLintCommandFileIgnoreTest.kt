package de.achimonline.ansible_lint.command.file

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.mockStatic
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever
import java.io.File
import kotlin.io.path.createTempDirectory

@RunWith(MockitoJUnitRunner::class)
class AnsibleLintCommandFileIgnoreTest {
    @Mock
    internal lateinit var project: Project

    @Mock
    internal lateinit var editorPsiFile: PsiFile

    @Mock
    internal lateinit var editorFile: VirtualFile

    @Mock
    internal lateinit var projectRootManager: ProjectRootManager

    @Mock
    internal lateinit var contentRoot: VirtualFile

    private val testRule = "fqcn[action-core]"
    private var editorFilePath = "playbook.yml"

    private lateinit var projectDir: File
    private lateinit var ignoreFile: File

    @Before
    fun setUp() {
        projectDir = createTempDirectory().toFile()
        projectDir.deleteOnExit()

        ignoreFile = File("${projectDir}${File.separator}${AnsibleLintCommandFileIgnore.DEFAULT}")

        mockStatic(ProjectRootManager::class.java, Mockito.CALLS_REAL_METHODS).use {
            it.`when`<Any> { ProjectRootManager.getInstance(project) }.thenReturn(projectRootManager)
        }

        whenever(projectRootManager.contentRoots).thenReturn(arrayOf(contentRoot))
        whenever(contentRoot.children).thenReturn(emptyArray())
        whenever(project.basePath).thenReturn(projectDir.absolutePath)
        whenever(editorPsiFile.virtualFile).thenReturn(editorFile)
        whenever(editorFile.path).thenReturn("${projectDir.absolutePath}${File.separator}${editorFilePath}")
    }

    @Test
    fun addRule_emptyIgnoreFile() {
        ignoreFile.writeText("")

        AnsibleLintCommandFileIgnore(project).addRule(editorPsiFile, testRule)

        assertEquals(
            "$editorFilePath $testRule".plus(System.lineSeparator()),
            ignoreFile.readText()
        )
    }

    @Test
    fun addRule_existingIgnoreFile() {
        ignoreFile.writeText(
            """
            vars/main.yml yaml[braces]
            tasks/main.yml name[casing]
            """.trimIndent()
        )

        AnsibleLintCommandFileIgnore(project).addRule(editorPsiFile, testRule)

        assertEquals(
            """
            vars/main.yml yaml[braces]
            tasks/main.yml name[casing]
            $editorFilePath $testRule
            """.trimIndent().plus(System.lineSeparator()),
            ignoreFile.readText()
        )
    }

    @Test
    fun addRule_existingIgnoreFileExistingRule() {
        val ignoreFileContent =
            """
            vars/main.yml yaml[braces]
            $editorFilePath $testRule
            tasks/main.yml name[casing]
            """.trimIndent()

        ignoreFile.writeText(ignoreFileContent)

        AnsibleLintCommandFileIgnore(project).addRule(editorPsiFile, testRule)

        assertEquals(ignoreFileContent, ignoreFile.readText())
    }

    @Test
    fun parse() {
        ignoreFile.writeText(
            """
            # comment
            vars/main.yml yaml[braces]
            tasks/main.yml name[casing] # comment
            tasks/main.yml yaml[braces]

            tasks/test.yml name[casing]  #    comment
            roles/test/tasks/main.yml name[casing]
            """.trimIndent()
        )

        val ignores = AnsibleLintCommandFileIgnore(project).parse()

        assertEquals(4, ignores.size)

        assertEquals(1, ignores["vars/main.yml"]!!.size)
        assertEquals(2, ignores["tasks/main.yml"]!!.size)
        assertEquals(1, ignores["tasks/test.yml"]!!.size)
        assertEquals(1, ignores["roles/test/tasks/main.yml"]!!.size)

        assertEquals("yaml[braces]", ignores["vars/main.yml"]!!.first())
        assertEquals("name[casing]", ignores["tasks/main.yml"]!!.first())
        assertEquals("name[casing]", ignores["tasks/test.yml"]!!.first())
    }
}
