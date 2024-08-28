package de.achimonline.ansible_lint.common

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import java.nio.file.Path
import java.nio.file.attribute.FileAttribute
import kotlin.io.path.pathString

class AnsibleLintHelper {
    companion object {
        fun createTempDirectory(
            directory: Path? = null,
            prefix: String = "intellij_ansible_lint_",
            vararg attributes: FileAttribute<*>
        ): Path {
            return kotlin.io.path.createTempDirectory(
                directory = directory,
                prefix = prefix,
                attributes = attributes
            )
        }

        fun getProjectBasePath(project: Project): String {
            ProjectRootManager.getInstance(project).contentRoots.forEach { contentRoot ->
                contentRoot.children.forEach { child ->
                    if (
                        child.name == ".idea" ||
                        child.name.endsWith(".ipr")
                    ) {
                        return contentRoot.toNioPath().pathString
                    }
                }
            }

            return project.basePath!!
        }
    }
}
