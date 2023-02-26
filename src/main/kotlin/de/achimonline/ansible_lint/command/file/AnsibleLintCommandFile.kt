package de.achimonline.ansible_lint.command.file

import com.intellij.openapi.project.Project
import de.achimonline.ansible_lint.common.AnsibleLintHelper
import java.io.File

abstract class AnsibleLintCommandFile(
    val project: Project,
    private val filePaths: List<String>
) {
    private val projectBasePath = AnsibleLintHelper.getProjectBasePath(project)

    fun locate(basePath: String = projectBasePath): File? {
        filePaths.forEach {
            val file = File("${basePath}${File.separator}${it}")

            if (file.exists()) {
                return file
            }
        }

        return null
    }

    fun locateOrCreate(creationFilePath: String = filePaths.first()): File {
        var file = locate()

        if (file == null) {
            file = File("${projectBasePath}${File.separator}${creationFilePath}")
            File(file.parent).mkdirs()
            file.createNewFile()
            file.writeText(initialContent())
        }

        return file
    }

    abstract fun initialContent(): String
}
