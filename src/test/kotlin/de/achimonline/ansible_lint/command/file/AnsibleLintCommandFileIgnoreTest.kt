package de.achimonline.ansible_lint.command.file

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import de.achimonline.ansible_lint.common.AnsibleLintHelper.Companion.createTempDirectory
import io.mockk.every
import io.mockk.mockkStatic
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import java.io.File

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

// NOTE: Mockito's "mockStatic" doesn't work anymore after upgrading from 4.x to 5.x; using MockK instead
//        mockStatic(ProjectRootManager::class.java, Mockito.CALLS_REAL_METHODS).use { mockedProjectRootManager ->
//            mockedProjectRootManager.`when`<Any> { ProjectRootManager.getInstance(project) }.doReturn(projectRootManager)
//        }
        mockkStatic(ProjectRootManager::class)
        every { ProjectRootManager.getInstance(project) } returns projectRootManager

        whenever(projectRootManager.contentRoots).doReturn(arrayOf(contentRoot))
        whenever(contentRoot.children).doReturn(emptyArray())
        whenever(project.basePath).doReturn(projectDir.absolutePath)
        whenever(editorPsiFile.virtualFile).doReturn(editorFile)
        whenever(editorFile.path).doReturn("${projectDir.absolutePath}${File.separator}${editorFilePath}")
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
        ignoreFile.writeText("""
            vars/main.yml yaml[braces]
            tasks/main.yml name[casing]
        """.trimIndent()
        )

        AnsibleLintCommandFileIgnore(project).addRule(editorPsiFile, testRule)

        assertEquals("""
            vars/main.yml yaml[braces]
            tasks/main.yml name[casing]
            $editorFilePath $testRule
        """.trimIndent().plus(System.lineSeparator()),
            ignoreFile.readText())
    }

    @Test
    fun addRule_existingIgnoreFileExistingRule() {
        val ignoreFileContent = """
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
        ignoreFile.writeText("""
            # comment
            vars/main.yml yaml[braces]
            tasks/main.yml name[casing] # comment
            tasks/main.yml yaml[braces]

            tasks/test.yml name[casing]  #    comment
            tasks/test.yml # missing rule
            roles/test/tasks/main.yml name[casing]
        """.trimIndent())

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
