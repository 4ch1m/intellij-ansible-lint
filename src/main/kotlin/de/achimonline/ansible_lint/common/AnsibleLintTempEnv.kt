package de.achimonline.ansible_lint.common

import com.intellij.psi.PsiFile
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.createTempDirectory

/**
 * 'ansible-lint' needs full path information of the file to be linted, so it can derive
 * the "file kind" (playbook, galaxy, tasks, vars, etc.).
 * But since we can't operate/pass the original file(path) to the linter
 * (see [de.achimonline.ansible_lint.annotator.AnsibleLintAnnotator.doAnnotate]), we need
 * to replicate the same parent-directory structure of the original file for the temp-file.
 *
 * Additionally, we have to make sure that any adjacent content-folders ("roles", etc.) are resolvable
 * when linting the temporary file.
 * For this we simply create symlinks to the original folder(s).
 */
class AnsibleLintTempEnv(
    projectBasePath: String,
    fileToLint: PsiFile,
    fileContent: String
) {
    val directory: File
    val file: File
    val symlinks: List<Path>

    init {
        val relativeFilePath = fileToLint.virtualFile.path.removePrefix(projectBasePath)
        val parentDirectories = relativeFilePath.split(File.separator).dropLast(1)

        directory = createTempDirectory().toFile()

        val directoryPathPlusIntermediateDirectories =
            "${directory.absolutePath}${File.separator}${parentDirectories.joinToString(File.separator)}"

        File(directoryPathPlusIntermediateDirectories).mkdirs()

        file = File("${directoryPathPlusIntermediateDirectories}${File.separator}${fileToLint.virtualFile.name}")
        file.createNewFile()
        file.writeText(fileContent)

        symlinks = createSymlinksToSpecificAnsibleFolders(projectBasePath, directory.path)
    }

    private fun createSymlinksToSpecificAnsibleFolders(
        projectBasePath: String,
        symlinkBasePath: String
    ): List<Path> {
        val symlinks = mutableListOf<Path>()

        File(projectBasePath).walkTopDown().forEach {
            if (it.isDirectory) {
                if (specificFolders.contains(it.name)) {
                    val symlinkPath = "${symlinkBasePath}${it.path.removePrefix(projectBasePath)}"

                    // create intermediate dirs for symlink
                    File(symlinkPath.split(File.separator).dropLast(1).joinToString(File.separator)).mkdirs()

                    try {
                        symlinks.add(
                            Files.createSymbolicLink(
                                Paths.get(symlinkPath),
                                it.toPath()
                            )
                        )
                    } catch (_: Exception) {
                        // silently ignore exceptions
                    }
                }
            }
        }

        return symlinks.toList()
    }

    fun purge(): Boolean {
        return try {
            directory.delete()
        } catch (_: Exception) {
            false
        }
    }

    companion object {
        private val specificFolders = listOf(
            "roles",
            "library",
            "filter_plugins"
        )
    }
}
