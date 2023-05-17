package de.achimonline.ansible_lint.common

import com.intellij.ide.highlighter.ProjectFileType.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import org.jetbrains.jps.model.serialization.PathMacroUtil.*
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
    }
}
