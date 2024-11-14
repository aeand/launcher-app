package com.example.my_launcher.notes

import androidx.compose.runtime.mutableStateListOf
import com.example.my_launcher.MainActivity.CustomFile
import java.io.File
import java.io.FileInputStream

class Notes {
    var files = mutableStateListOf<CustomFile>()
    var rootFolderName = "Notes"
    private var root = "/storage/emulated/0/${rootFolderName}"

    fun saveFolder(name: String, path: String = "") {
        val folder = File(root, path + name) // applicationContext.getExternalFilesDir(null)

        if (!folder.exists()) {
            if (!folder.mkdir()) {
                println("error: Cannot create a directory!")
            } else {
                folder.mkdirs()
            }
        }

        updateFiles()
    }

    fun overrideFile(name: String, folder: String, content: String) {
        val letDirectory = File(root, folder)
        letDirectory.mkdirs()
        val file = File(letDirectory, "$name.txt")
        file.writeText(content)

        updateFiles()
    }

    fun saveFile(name: String, folder: String = "", content: String): Boolean {
        val letDirectory = File(root, folder)
        letDirectory.mkdirs()
        val file = File(letDirectory, "$name.txt")
        if (file.exists()) {
            return false
        }

        file.writeText(content)

        updateFiles()

        return true
    }

    fun readFile(file: File): String {
        return FileInputStream(file).bufferedReader().use {
            it.readText()
        }
    }

    fun moveFile(sourceFilePaths: String, targetFile: CustomFile) {
        val pathList = mutableListOf<String>()
        if (sourceFilePaths.contains("_-middle-_")) {
            sourceFilePaths.split("_-middle-_").forEach { path ->
                pathList.add(path)
            }
        }
        else {
            pathList.add(sourceFilePaths)
        }

        val sourceFileList = mutableListOf<CustomFile>()
        pathList.forEach { path ->
            val file = files.find { file ->
                file.file.name == path.takeLastWhile { s -> s != '/' } &&  file.file.path == path
            }
            if (file != null) {
                sourceFileList.add(file)
            }
        }

        sourceFileList.forEach { sourceFile ->
            if (sourceFile.file.path == targetFile.file.path) {
                println("error: file is targeting the source file")
                return@forEach
            }

            if (root == targetFile.file.path && targetFile.file.path == sourceFile.file.path.replace("/${sourceFile.file.name}", "")) {
                println("error: file is already in root")
                return@forEach
            }

            if (sourceFile.file.isFile) {
                if (targetFile.file.isFile) {
                    val targetFilePath = targetFile.file.path.replace("/${targetFile.file.name}", "")

                    if (sourceFile.file.name == targetFile.file.name) {
                        println("error: file with that name already exists")
                        return@forEach
                    }

                    if (sourceFile.file.path.replace("/${sourceFile.file.name}", "") == targetFilePath) {
                        println("error: file and file have the same path")
                        return@forEach
                    }

                    val filesInPath = File(root, targetFile.file.path.replace(root, "").replace(targetFile.file.name, "")).listFiles()
                    if (filesInPath == null) {
                        println("error: found no files in ${root + targetFile.file.path.replace(root, "").replace(targetFile.file.name, "")}")
                        return@forEach
                    }

                    for (file in filesInPath) {
                        if (sourceFile.file.name == file.name && file.isFile) {
                            println("error: path has file with the same name as source")
                            return@forEach
                        }
                    }

                    copyFile(sourceFile, "$targetFilePath/${sourceFile.file.name}")
                    if (canDeleteFile(sourceFile, targetFile, targetFile.file.path))
                        deleteFile(sourceFile)
                    updateFiles()
                }
                else if (targetFile.file.isDirectory) {
                    val listOfFilesInDir = targetFile.file.listFiles()
                    if (listOfFilesInDir != null) {
                        for (file in listOfFilesInDir) {
                            if (sourceFile.file.name == file.name && file.isFile) {
                                println("error: found file with same name as source file")
                                return@forEach
                            }
                        }
                    }

                    copyFile(sourceFile, "${targetFile.file.path}/${sourceFile.file.name}")
                    if (canDeleteFile(sourceFile, targetFile, targetFile.file.path))
                        deleteFile(sourceFile)
                    updateFiles()
                }
                else {
                    println("error: target file is not file or folder ${sourceFile.file.exists()} ${targetFile.file.exists()}")
                    return@forEach
                }
            }
            else if (sourceFile.file.isDirectory) {
                if (targetFile.file.isFile) {
                    println("dir to file")
                    val targetFilePath = targetFile.file.path.replace("/${targetFile.file.name}", "")

                    val filesInPath = File(root, targetFile.file.path.replace(root, "").replace(targetFile.file.name, "")).listFiles()
                    if (filesInPath == null) {
                        println("error: found no files in ${root + targetFile.file.path.replace(root, "").replace(targetFile.file.name, "")}")
                        return@forEach
                    }

                    for (file in filesInPath) {
                        if (sourceFile.file.name == file.name && file.isDirectory) {
                            println("error: path has folder with the same name as source")
                            return@forEach
                        }
                    }

                    copyFile(sourceFile, "$targetFilePath/${sourceFile.file.name}")
                    if (canDeleteFile(sourceFile, targetFile, targetFilePath))
                        deleteFile(sourceFile)
                    updateFiles()
                }
                else if (targetFile.file.isDirectory) {
                    if (targetFile.children != null) {
                        for (file in targetFile.children) {
                            if (sourceFile.file.name == file.file.name && file.file.isDirectory) {
                                println("error: folder with that name already exists")
                                return@forEach
                            }
                        }
                    }

                    copyFile(sourceFile, "${targetFile.file.path}/${sourceFile.file.name}")
                    if (canDeleteFile(sourceFile, targetFile, targetFile.file.path))
                        deleteFile(sourceFile)
                    updateFiles()
                }
                else {
                    println("error: target file is not file or folder ${sourceFile.file.exists()} ${targetFile.file.exists()}")
                    return@forEach
                }
            }
            else {
                println("error: source file is not file or folder ${sourceFile.file.exists()} ${targetFile.file.exists()}")
                return@forEach
            }
        }
    }

    private fun copyFile(file: CustomFile, path: String) {
        if (file.file.isFile) {
            try {
            file.file.copyTo(File(path))
            } catch (e: Exception) {
                println("error: file copy failed $e")
            }
        }
        else if (file.file.isDirectory) {
            try {
                file.file.copyRecursively(File(path))
            } catch (e: Exception) {
                println("error: folder copy failed $e")
            }
        }
    }

    private fun canDeleteFile(sourceFile: CustomFile, targetFile: CustomFile, targetPath: String): Boolean {
        if (sourceFile.file.isFile) {
            return sourceFile.file.exists() && File(targetPath + "/${sourceFile.file.name}").exists()
        }
        else if (sourceFile.file.isDirectory) {
            if (!sourceFile.file.exists() || !File(targetPath + "/${sourceFile.file.name}").exists()) {
                println("error: source file and/or target file doesn't exist")
                return false
            }

            if (sourceFile.children != null) {
                val targetChildren = getFiles(targetPath.replace(root, ""))
                for (sourceChild in sourceFile.children) {
                    var foundCopy = false
                    val relativePath = sourceChild.file.path.replace(sourceFile.file.path, "")

                    for (copiedChild in targetChildren) {
                        val relativePath2 = copiedChild.file.path.replace("${targetFile.file.path}/${sourceFile.file.name}", "")

                        if (sourceChild.file.name == copiedChild.file.name && relativePath == relativePath2) {
                            foundCopy = true
                            break
                        }
                    }

                    if (!foundCopy) {
                        println("error: didn't find corresponding child to: ${sourceChild.file.name}")
                        return false
                    }
                }

                return true
            }
            else {
                return sourceFile.file.exists() && File(targetPath + "/${sourceFile.file.name}").exists()
            }
        }
        else {
            updateFiles()
            return false
        }
    }

    fun deleteFile(file: CustomFile) {
        try {
            if (file.file.isFile) {
                file.file.delete()
            }
            else if (file.file.isDirectory) {
                file.file.deleteRecursively()
            }
        } catch (e: Exception) {
            println("error: delete file failure $e")
        }

        updateFiles()
    }

    fun updateFiles() {
        files.clear()
        getFiles().forEach {
            if (it.file.exists())
                files.add(it)
        }
    }

    private fun getFiles(path: String = ""): MutableList<CustomFile> {
        val files = File(root, path).listFiles()
        val directoryLevel = path.count { it == '/' } + 1

        files?.sortWith { a, b ->
            a.name.uppercase().compareTo(b.name.uppercase())
            a.isFile.compareTo(b.isFile)
        }

        val result = mutableListOf<CustomFile>()
        files?.forEach { file ->
            var children: MutableList<CustomFile>? = null
            if (!file.isFile) {
                children = getFiles("$path/${file.name}")
            }
            result.add(CustomFile(file, children, directoryLevel, false))
            children?.forEach { result.add(it) }
        }

        return result
    }
}