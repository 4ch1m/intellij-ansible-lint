package de.achimonline.ansible_lint.common

import com.intellij.ide.highlighter.ProjectFileType.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.PsiFile
import org.jetbrains.jps.model.serialization.PathMacroUtil.*
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.io.path.pathString

class AnsibleLintHelper {
    companion object {
        fun getProjectBasePath(project: Project): String {
            ProjectRootManager.getInstance(project).contentRoots.forEach { contentRoot ->
                contentRoot.children.forEach { child ->
                    // check for ".idea" or "*.ipr"
                    if (
                        child.name == DIRECTORY_STORE_NAME ||
                        child.name.endsWith(DOT_DEFAULT_EXTENSION)
                    ) {
                        return contentRoot.toNioPath().pathString
                    }
                }
            }

            return project.basePath!!
        }

        /**
            'ansible-lint' needs full path information of the file to be linted, so it can derive
            the "file kind" (playbook, galaxy, tasks, vars, etc.).
            But since we can't operate/pass the original file(path) to the linter
            (see [de.achimonline.ansible_lint.annotator.AnsibleLintAnnotator.doAnnotate]), we need
            to replicate the same parent-directory structure of the original file for the temp-file.
         */
        fun createTempFolderAndFile(
            projectBasePath: String,
            file: PsiFile,
            content: String
        ): Pair<File, File> {
            val originalPathWithoutProjectBasePath = file.virtualFile.path.removePrefix(projectBasePath)
            val parentDirectories = originalPathWithoutProjectBasePath.split(File.separator).dropLast(1)

            val tempDirectory = createTempDirectory().toFile()
            val tempDirectoryBasePath = tempDirectory.absolutePath
            val tempDirectoryBasePathWithSubDirectories = "${tempDirectoryBasePath}${File.separator}${parentDirectories.joinToString(File.separator)}"

            File(tempDirectoryBasePathWithSubDirectories).mkdirs()

            val tempFile = File("${tempDirectoryBasePathWithSubDirectories}${File.separator}${file.virtualFile.name}")
            tempFile.createNewFile()
            tempFile.writeText(content)

            return Pair(tempDirectory, tempFile)
        }
    }
}
