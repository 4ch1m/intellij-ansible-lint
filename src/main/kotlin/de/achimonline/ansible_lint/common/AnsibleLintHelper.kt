package de.achimonline.ansible_lint.common

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.io.path.pathString

class AnsibleLintHelper {
    companion object {
        /**
            'ansible-lint' needs full path information of the file to be linted, so it can derive
            the "file kind" (playbook, galaxy, tasks, vars, etc.).
            But since we can't operate/pass the original file(path) the linter
            (see [de.achimonline.ansible_lint.annotator.AnsibleLintAnnotator.doAnnotate]), we need
            to replicate the same parent-directory structure of the original file for the temp-file.
         */
        fun createTempFolderAndFile(
            project: Project,
            file: PsiFile,
            content: String
        ): Pair<File, File> {
            val originalPathWithoutProjectBasePath = file.virtualFile.path.removePrefix(project!!.basePath!!)
            val parentDirectories = originalPathWithoutProjectBasePath.split(File.separator).dropLast(1)

            val tempDirectoryBasePath = createTempDirectory().pathString
            val tempDirectoryBasePathWithSubDirectories = "${tempDirectoryBasePath}${File.separator}${parentDirectories.joinToString(File.separator)}"
            val tempDirectory = File(tempDirectoryBasePathWithSubDirectories)
            tempDirectory.mkdirs()

            val tempFile = File("${tempDirectoryBasePathWithSubDirectories}${File.separator}${file.virtualFile.name}")
            tempFile.createNewFile()
            tempFile.writeText(content)

            return Pair(tempDirectory, tempFile)
        }
    }
}
