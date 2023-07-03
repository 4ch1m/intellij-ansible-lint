package de.achimonline.ansible_lint.common

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.jps.model.serialization.PathMacroUtil
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.mockStatic
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
        mockStatic(ProjectRootManager::class.java, Mockito.CALLS_REAL_METHODS).use { mockedProjectRootManager ->
            mockedProjectRootManager.`when`<Any> { ProjectRootManager.getInstance(project) }.doReturn(projectRootManager)
        }

        whenever(projectRootManager.contentRoots).doReturn(arrayOf(projectRootManagerContentRoot))
    }

    @Test
    fun getProjectBasePath_viaProjectRootManager() {
        whenever(projectRootManagerContentRoot.children).doReturn(arrayOf(projectRootManagerContentRootChild))
        whenever(projectRootManagerContentRootChild.name).doReturn(PathMacroUtil.DIRECTORY_STORE_NAME)
        whenever(projectRootManagerContentRoot.toNioPath()).doReturn(projectRootManagerContentRootPath)
        whenever(projectRootManagerContentRootPath.pathString).doReturn(projectBasePath)

        val projectBasePath = AnsibleLintHelper.getProjectBasePath(project)

        assertEquals(projectBasePath, projectBasePath)

        verify(project, never()).basePath
    }

    @Test
    fun getProjectBasePath_viaBasePathProperty() {
        whenever(projectRootManagerContentRoot.children).doReturn(emptyArray())
        whenever(project.basePath).doReturn(projectBasePath)

        val projectBasePath = AnsibleLintHelper.getProjectBasePath(project)

        assertEquals(projectBasePath, projectBasePath)

        verify(project, times(1)).basePath
    }
}
