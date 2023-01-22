package de.achimonline.ansible_lint.common

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import java.io.File

@RunWith(MockitoJUnitRunner::class)
class AnsibleLintHelperTest {
    @Mock
    internal lateinit var project: Project

    @Mock
    internal lateinit var psiFile: PsiFile

    @Mock
    internal lateinit var virtualFile: VirtualFile

    @Test
    fun createTempFile() {
        val basePath = "${File.separator}home${File.separator}achim${File.separator}testproject"
        val fileName = "test.yml"
        val intermediateDirectory = "tasks"
        val filePath = "${basePath}${File.separator}${intermediateDirectory}${File.separator}${fileName}"
        val fileContent = "testContent"

        whenever(project.basePath) doReturn basePath
        whenever(psiFile.virtualFile) doReturn virtualFile
        whenever(virtualFile.path) doReturn filePath
        whenever(virtualFile.name) doReturn fileName

        val tempFolderAndFile = AnsibleLintHelper.createTempFolderAndFile(project, psiFile, fileContent)

        assertTrue(tempFolderAndFile.first.path.endsWith("${File.separator}${intermediateDirectory}"))
        assertTrue(tempFolderAndFile.second.path.endsWith("${File.separator}${intermediateDirectory}${File.separator}${fileName}"))

        assertEquals(fileContent, tempFolderAndFile.second.readText())
    }
}
