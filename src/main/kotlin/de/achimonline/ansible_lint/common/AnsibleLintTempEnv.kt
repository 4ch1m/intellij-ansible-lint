package de.achimonline.ansible_lint.common

import com.intellij.psi.PsiFile
import de.achimonline.ansible_lint.common.AnsibleLintHelper.Companion.createTempDirectory
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * 'ansible-lint' needs full path information of the file to be linted, so it can derive
 * the "file kind" (playbook, galaxy, tasks, vars, etc.).
 * But since we can't operate/pass the original file(path) to the linter
 * (see [de.achimonline.ansible_lint.annotator.AnsibleLintAnnotator.doAnnotate]), we need
 * to replicate the same (parent-)directory structure of the original file for the temp-file.
 *
 * Additionally, we have to make sure that any specific content-folders ("roles", etc.) are resolvable
 * when linting the temporary file.
 * The same goes for static YAML-resources (pulled by 'ansible-lint' via 'import_playbook', 'import_tasks', etc.).
 *
 * For this we simply create symlinks to the original files/folders.
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
        val normalizedProjectBasePath = Paths.get(projectBasePath).normalize().toString();
        val normalizedFileToLint = Paths.get(fileToLint.virtualFile.path).normalize().toString();

        val relativeFilePath = normalizedFileToLint.removePrefix(normalizedProjectBasePath)
        val parentDirectories = relativeFilePath.split(File.separator).dropLast(1)

        directory = createTempDirectory().toFile()
        directory.deleteOnExit()

        val directoryPathPlusIntermediateDirectories =
            "${directory.absolutePath}${File.separator}${parentDirectories.joinToString(File.separator)}"

        File(directoryPathPlusIntermediateDirectories).mkdirs()

        file = File("${directoryPathPlusIntermediateDirectories}${File.separator}${fileToLint.virtualFile.name}")
        file.createNewFile()
        file.writeText(fileContent)

        symlinks = createSymlinks(normalizedProjectBasePath, directory.absolutePath, file.absolutePath)
    }

    private fun createSymlinks(
        projectBasePath: String,
        symlinkBasePath: String,
        lintedFilePath: String
    ): List<Path> {
        val symlinks = mutableListOf<Path>()

        File(projectBasePath).walkTopDown().forEach {
            if (it.absolutePath == lintedFilePath) return@forEach

            if ((it.isDirectory && specialFolders.contains(it.name)) ||
                (it.isFile && yamlFileExtensions.contains(it.extension.lowercase()))) {

                val symlinkPath = "${symlinkBasePath}${it.absolutePath.removePrefix(projectBasePath)}"

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

        return symlinks.toList()
    }

    fun purge(): Boolean {
        return try {
            // non-empty dirs can't be deleted;
            // so we clean all temp-files/-dirs manually
            Files.walk(directory.toPath())
                .sorted(Comparator.reverseOrder())
                .forEach {
                    Files.delete(it)
                }
            true
        } catch (_: Exception) {
            false
        }
    }

    companion object {
        private val specialFolders = listOf(
            "roles",
            "library",
            "filter_plugins"
        )

        private val yamlFileExtensions = listOf(
            "yml",
            "yaml"
        )
    }
}
