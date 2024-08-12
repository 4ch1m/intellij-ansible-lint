package de.achimonline.ansible_lint.common

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import kotlin.io.path.pathString

class AnsibleLintHelper {
    companion object {
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
