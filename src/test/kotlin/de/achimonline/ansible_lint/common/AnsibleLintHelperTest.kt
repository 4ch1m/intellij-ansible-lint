package de.achimonline.ansible_lint.common

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import org.jetbrains.jps.model.serialization.PathMacroUtil
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.*
import java.io.File
import java.nio.file.Path
import kotlin.io.path.pathString

@RunWith(MockitoJUnitRunner::class)
class AnsibleLintHelperTest {
    @Mock
    internal lateinit var project: Project

    @Mock
    internal lateinit var editorPsiFile: PsiFile

    @Mock
    internal lateinit var editorFile: VirtualFile

    @Mock
    internal lateinit var projectRootManager: ProjectRootManager

    @Mock
    internal lateinit var projectRootManagerContentRoot: VirtualFile

    @Mock
    internal lateinit var projectRootManagerContentRootChild: VirtualFile

    @Mock
    internal lateinit var projectRootManagerContentRootPath: Path

    private var projectBasePath = "${File.separator}home${File.separator}achim${File.separator}testproject"

    @Before
    fun setUp() {
        Mockito.mockStatic(ProjectRootManager::class.java, Mockito.CALLS_REAL_METHODS).use {
            it.`when`<Any> { ProjectRootManager.getInstance(project) }.thenReturn(projectRootManager)
        }

        whenever(projectRootManager.contentRoots).thenReturn(arrayOf(projectRootManagerContentRoot))
    }

    @Test
    fun createTempFile() {
        val fileName = "test.yml"
        val intermediateDirectory = "tasks"
        val filePath = "${projectBasePath}${File.separator}${intermediateDirectory}${File.separator}${fileName}"
        val fileContent = "testContent"

        whenever(editorPsiFile.virtualFile) doReturn editorFile
        whenever(editorFile.path) doReturn filePath
        whenever(editorFile.name) doReturn fileName

        val tempFolderAndFile = AnsibleLintHelper.createTempFolderAndFile(projectBasePath, editorPsiFile, fileContent)

        assertNotNull(tempFolderAndFile.first)

        assertTrue(tempFolderAndFile.second.path.startsWith(tempFolderAndFile.first.path))
        assertTrue(tempFolderAndFile.second.path.endsWith("${File.separator}${intermediateDirectory}${File.separator}${fileName}"))

        assertEquals(fileContent, tempFolderAndFile.second.readText())
    }

    @Test
    fun getProjectBasePath_viaProjectRootManager() {
        whenever(projectRootManagerContentRoot.children).thenReturn(arrayOf(projectRootManagerContentRootChild))
        whenever(projectRootManagerContentRootChild.name).thenReturn(PathMacroUtil.DIRECTORY_STORE_NAME)
        whenever(projectRootManagerContentRoot.toNioPath()).thenReturn(projectRootManagerContentRootPath)
        whenever(projectRootManagerContentRootPath.pathString).thenReturn(projectBasePath)

        val projectBasePath = AnsibleLintHelper.getProjectBasePath(project)

        assertEquals(projectBasePath, projectBasePath)

        verify(project, never()).basePath
    }

    @Test
    fun getProjectBasePath_viaBasePathProperty() {
        whenever(projectRootManagerContentRoot.children).thenReturn(emptyArray())
        whenever(project.basePath).thenReturn(projectBasePath)

        val projectBasePath = AnsibleLintHelper.getProjectBasePath(project)

        assertEquals(projectBasePath, projectBasePath)

        verify(project, times(1)).basePath
    }
}
