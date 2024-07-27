package de.achimonline.ansible_lint.command.file

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import org.junit.Before
import org.junit.Test

import org.junit.Assert.*
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.mockStatic
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import java.io.File
import kotlin.io.path.createTempDirectory

@RunWith(MockitoJUnitRunner::class)
class AnsibleLintCommandFileConfigTest {
    @Mock
    internal lateinit var project: Project

    @Mock
    internal lateinit var projectRootManager: ProjectRootManager

    @Mock
    internal lateinit var contentRoot: VirtualFile

    private lateinit var projectDir: File
    private lateinit var configFile: File

    @Before
    fun setUp() {
        projectDir = createTempDirectory().toFile()
        projectDir.deleteOnExit()

        configFile = File("${projectDir}${File.separator}${AnsibleLintCommandFileConfig.DEFAULT}")

        mockStatic(ProjectRootManager::class.java, Mockito.CALLS_REAL_METHODS).use { mockedProjectRootManager ->
            mockedProjectRootManager.`when`<Any> { ProjectRootManager.getInstance(project) }.doReturn(projectRootManager)
        }

        whenever(projectRootManager.contentRoots).doReturn(arrayOf(contentRoot))
        whenever(contentRoot.children).doReturn(emptyArray())
        whenever(project.basePath).doReturn(projectDir.absolutePath)
    }

    @Test
    fun addRuleToSkipList_noExistingConfigFile() {
        val ansibleLintCommandFileConfig = AnsibleLintCommandFileConfig(project)
        val testRule = "testRule"

        AnsibleLintCommandFileConfig(project).addRuleToSkipList(testRule)

        assertEquals(
            listOf(
                ansibleLintCommandFileConfig.initialContent().trimEnd(),
                "",
                "${AnsibleLintCommandFileConfig.YAML_SKIP_LIST_NODE}:",
                "${AnsibleLintCommandFileConfig.YAML_DEFAULT_INDENT}- $testRule",
                ""
            ).joinToString(System.lineSeparator()), configFile.readText()
        )
    }

    @Test
    fun addRuleToSkipList_existingConfigFileNoSkipList() {
        val testRule = "testRule"

        configFile.writeText("""
            ---
            # .ansible-lint
            
            profile: null # min, basic, moderate,safety, shared, production
            
            exclude_paths:
              - .cache/ # implicit unless exclude_paths is defined in config
              - .github/
              - test/fixtures/formatting-before/
              - test/fixtures/formatting-prettier/
            
            # Offline mode disables installation of requirements.yml and schema refreshing
            offline: true
            
        """.trimIndent())

        AnsibleLintCommandFileConfig(project).addRuleToSkipList(testRule)

        assertEquals("""
            ---
            # .ansible-lint
            
            profile: null # min, basic, moderate,safety, shared, production
            
            exclude_paths:
              - .cache/ # implicit unless exclude_paths is defined in config
              - .github/
              - test/fixtures/formatting-before/
              - test/fixtures/formatting-prettier/
            
            # Offline mode disables installation of requirements.yml and schema refreshing
            offline: true

            skip_list:
              - testRule
            
        """.trimIndent(), configFile.readText())
    }

    @Test
    fun addRuleToSkipList_existingConfigFileExistingSkipList() {
        configFile.writeText("""
            ---
            # .ansible-lint
            
            profile: null # min, basic, moderate,safety, shared, production
            
            exclude_paths:
              - .cache/ # implicit unless exclude_paths is defined in config
              - .github/
              - test/fixtures/formatting-before/
              - test/fixtures/formatting-prettier/
                        
            skip_list:
              - testRule1
              - "testRule2"
              - testRule3
              - 'testRule4'
              -     testRule5

            # Offline mode disables installation of requirements.yml and schema refreshing
            offline: true
            
        """.trimIndent())

        AnsibleLintCommandFileConfig(project).addRuleToSkipList("testRule1")
        AnsibleLintCommandFileConfig(project).addRuleToSkipList("testRule2")
        AnsibleLintCommandFileConfig(project).addRuleToSkipList("testRule4")
        AnsibleLintCommandFileConfig(project).addRuleToSkipList("testRule5")
        AnsibleLintCommandFileConfig(project).addRuleToSkipList("testRule666")

        assertEquals("""
            ---
            # .ansible-lint
            
            profile: null # min, basic, moderate,safety, shared, production
            
            exclude_paths:
              - .cache/ # implicit unless exclude_paths is defined in config
              - .github/
              - test/fixtures/formatting-before/
              - test/fixtures/formatting-prettier/
                        
            skip_list:
              - testRule666
              - testRule1
              - "testRule2"
              - testRule3
              - 'testRule4'
              -     testRule5

            # Offline mode disables installation of requirements.yml and schema refreshing
            offline: true
            
        """.trimIndent(), configFile.readText())
    }

    @Test
    fun getExcludePaths() {
        configFile.writeText("""
            ---
            # .ansible-lint
            
            profile: null # min, basic, moderate,safety, shared, production
            
            exclude_paths:
              - test/dir1
              - /test/dir2
              - test/dir3/
              - /test/dir4/
              -   test/dir5

            # Offline mode disables installation of requirements.yml and schema refreshing
            offline: true
            
        """.trimIndent())

        val excludePaths = AnsibleLintCommandFileConfig(project).getExcludePaths()

        assertEquals(5, excludePaths.size)

        listOf(
            "test/dir1",
            "/test/dir2",
            "test/dir3",
            "/test/dir4",
            "test/dir5"
        ).forEach {
            assertTrue(excludePaths.contains(it))
        }
    }
}
