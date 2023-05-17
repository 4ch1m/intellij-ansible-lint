package de.achimonline.ansible_lint.common

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.util.io.delete
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import java.io.File
import java.nio.file.Path
import kotlin.io.path.createTempDirectory
import kotlin.io.path.pathString

@RunWith(MockitoJUnitRunner::class)
class AnsibleLintTempEnvTest {
    @Mock
    internal lateinit var editorPsiFile: PsiFile

    @Mock
    internal lateinit var editorFile: VirtualFile

    internal lateinit var testDir: Path

    @Before
    fun setUp() {
        testDir = createTempDirectory()
    }

    @Test
    fun test() {
        val fileName = "test.yml"
        val intermediateDirectory = "tasks"

        listOf(
            "${testDir.pathString}${File.separator}foo",
            "${testDir.pathString}${File.separator}${intermediateDirectory}",
            "${testDir.pathString}${File.separator}roles",
            "${testDir.pathString}${File.separator}bar",
            "${testDir.pathString}${File.separator}bar${File.separator}_roles",
            "${testDir.pathString}${File.separator}bar${File.separator}library",
        ).forEach {
            File(it).mkdirs()
        }

        val filePath = "${testDir.pathString}${File.separator}${intermediateDirectory}${File.separator}${fileName}"
        val fileContent = "testContent"

        File(filePath).writeText(fileContent)

        whenever(editorPsiFile.virtualFile) doReturn editorFile
        whenever(editorFile.path) doReturn filePath
        whenever(editorFile.name) doReturn fileName

        val tempEnv = AnsibleLintTempEnv(
            testDir.pathString,
            editorPsiFile,
            fileContent
        )

        assertTrue(tempEnv.directory.exists())
        assertTrue(tempEnv.file.exists())

        assertTrue(tempEnv.file.path.startsWith(tempEnv.directory.path))
        assertTrue(tempEnv.file.path.endsWith("${File.separator}${intermediateDirectory}${File.separator}${fileName}"))

        assertEquals(fileContent, tempEnv.file.readText())

        assertEquals(2, tempEnv.symlinks.size)
    }

    @After
    fun tearDown() {
        testDir.delete()
    }
}
