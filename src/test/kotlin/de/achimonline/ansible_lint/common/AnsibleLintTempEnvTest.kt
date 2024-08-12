package de.achimonline.ansible_lint.common

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.util.io.delete
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.createTempDirectory
import kotlin.io.path.pathString

@RunWith(MockitoJUnitRunner::class)
class AnsibleLintTempEnvTest {
    @Mock
    internal lateinit var editorPsiFile: PsiFile

    @Mock
    internal lateinit var editorFile: VirtualFile

    private lateinit var tempDir: Path

    @Before
    fun setUp() {
        tempDir = createTempDirectory()
    }

    @Test
    fun test() {
        val fileName = "test.yml"
        val intermediateDir = "tasks"

        val sourceDirs = listOf( // Pair -> String: path, Boolean: shouldBeSymlinked
            Pair("foo", false),
            Pair(intermediateDir, false),
            Pair("roles", true),
            Pair("bar", false),
            Pair("bar${File.separator}_roles", false),
            Pair("bar${File.separator}library", true)
        )

        sourceDirs.forEach {
            val (path, _) = it
            File("${tempDir.pathString}${File.separator}${path}").mkdirs()
        }

        val sourceFiles = listOf( // Pair -> String: path, Boolean: shouldBeSymlinked
            Pair("playbook.yml", true),
            Pair("foo${File.separator}bar.yml", true),
            Pair("foo${File.separator}bar.txt", false),
            Pair("foo${File.separator}baz.YaMl", true),
            Pair("${intermediateDir}${File.separator}test.yml", false), // = the actual file to be linted
            Pair("bar${File.separator}_roles${File.separator}test.yml", true)
        )

        sourceFiles.forEach {
            val (path, _) = it

            if (path.contains(File.separator)) {
                File("${tempDir.pathString}${File.separator}${path.split(File.separator).dropLast(1).joinToString(File.separator)}").mkdirs()
            }

            Files.createFile(Paths.get("${tempDir.pathString}${File.separator}${path}"))
        }

        val filePath = "${tempDir.pathString}${File.separator}${intermediateDir}${File.separator}${fileName}"
        val fileContent = "testContent"

        File(filePath).writeText(fileContent)

        whenever(editorPsiFile.virtualFile) doReturn editorFile
        whenever(editorFile.path) doReturn filePath
        whenever(editorFile.name) doReturn fileName

        val tempEnv = AnsibleLintTempEnv(
            tempDir.pathString,
            editorPsiFile,
            fileContent
        )

        assertTrue(tempEnv.directory.exists())
        assertTrue(tempEnv.file.exists())

        assertEquals(
            "${tempEnv.directory.absolutePath}${File.separator}${intermediateDir}${File.separator}${fileName}",
            tempEnv.file.absolutePath
        )

        assertEquals(fileContent, tempEnv.file.readText())

        sourceDirs.plus(sourceFiles).forEach {
            val (path, match) = it

            assertEquals(
                match,
                (tempEnv.symlinks.filter { symlinkPath -> symlinkPath.pathString == "${tempEnv.directory.absolutePath}${File.separator}${path}" }).isNotEmpty(),
                if (match) "[ $path ] not found symlinks" else "[ $path ] should not be in symlinks"
            )
        }
    }

    @After
    fun tearDown() {
        tempDir.delete()
    }
}
