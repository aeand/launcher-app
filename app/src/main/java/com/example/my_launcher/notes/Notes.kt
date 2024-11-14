package com.example.my_launcher.notes

import androidx.compose.runtime.mutableStateListOf
import com.example.my_launcher.MainActivity.CustomFile
import java.io.File
import java.io.FileInputStream

class Notes {
    var files = mutableStateListOf<CustomFile>()
    var rootFolderName = "Notes"

    fun saveFolder(name: String, path: String = "") {
        val folder = File("/storage/emulated/0/${rootFolderName}", path + name) // applicationContext.getExternalFilesDir(null)

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
        val letDirectory = File("/storage/emulated/0/${rootFolderName}", folder)
        letDirectory.mkdirs()
        val file = File(letDirectory, "$name.txt")
        file.writeText(content)

        updateFiles()
    }

    fun saveFile(name: String, folder: String = "", content: String): Boolean {
        val letDirectory = File("/storage/emulated/0/${rootFolderName}", folder)
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

        val rootPath = "/storage/emulated/0/Android/data/com.example.my_launcher/files/"

        sourceFileList.forEach { sourceFile ->
            if (sourceFile.file.path == targetFile.file.path) {
                println("error: file is targeting the source file")
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

                    val filesInPath = File("/storage/emulated/0/${rootFolderName}", targetFile.file.path.replace(rootPath, "").replace(targetFile.file.name, "")).listFiles()
                    if (filesInPath == null) {
                        println("error: found no files in ${rootPath + targetFile.file.path.replace(rootPath, "").replace(targetFile.file.name, "")}")
                        return@forEach
                    }

                    for (file in filesInPath) {
                        if (sourceFile.file.name == file.name && file.isFile) {
                            println("error: path has file with the same name as source")
                            return@forEach
                        }
                    }

                    try {
                        sourceFile.file.copyTo(File("$targetFilePath/${sourceFile.file.name}"))

                        try {
                            sourceFile.file.delete()
                            updateFiles()
                            return@forEach
                        } catch (e: Exception) {
                            println("error: delete failed $e")
                            return@forEach
                        }
                    } catch (e: Exception) {
                        println("error: copy failed $e")
                        return@forEach
                    }
                }
                else if (targetFile.file.isDirectory) {
                    if (targetFile.children != null) {
                        for (file in targetFile.children) {
                            if (sourceFile.file.name == file.file.name && file.file.isFile) {
                                println("error: found file with same name as source file")
                                return@forEach
                            }
                        }
                    }

                    try {
                        sourceFile.file.copyTo(File("${targetFile.file.path}/${sourceFile.file.name}"))
                        try {
                            sourceFile.file.delete()
                            updateFiles()
                            return@forEach
                        } catch (e: Exception) {
                            println("error: delete failed $e")
                            return@forEach
                        }
                    } catch (e: Exception) {
                        println("error: copy failed $e")
                        return@forEach
                    }
                }
                else {
                    println("error: target file is not file or folder ${sourceFile.file.exists()} ${targetFile.file.exists()}")
                    return@forEach
                }
            }
            else if (sourceFile.file.isDirectory) {
                if (targetFile.file.isFile) {
                    val targetFilePath = targetFile.file.path.replace("/${targetFile.file.name}", "")

                    val filesInPath = File("/storage/emulated/0/${rootFolderName}", targetFile.file.path.replace(rootPath, "").replace(targetFile.file.name, "")).listFiles()
                    if (filesInPath == null) {
                        println("error: found no files in ${rootPath + targetFile.file.path.replace(rootPath, "").replace(targetFile.file.name, "")}")
                        return@forEach
                    }

                    for (file in filesInPath) {
                        if (sourceFile.file.name == file.name && file.isDirectory) {
                            println("error: path has folder with the same name as source")
                            return@forEach
                        }
                    }

                    copyFolderAndChildren(sourceFile, targetFilePath)
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

                    copyFolderAndChildren(sourceFile, targetFile.file.path)
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

    fun copyFolderAndChildren(sourceFile: CustomFile, targetPath: String) {
        try {
            sourceFile.file.copyRecursively(File("$targetPath/${sourceFile.file.name}"))

            try {
                sourceFile.file.deleteRecursively()
            } catch (e: Exception) {
                println("error: delete failed $e")
            }
        } catch (e: Exception) {
            println("error: copy failed $e")
        }

        updateFiles()
    }

    fun deleteFileAndChildren(sourceFile: CustomFile) {
        try {
            sourceFile.file.deleteRecursively()
        } catch (e: Exception) {
            println("error: delete failed $e")
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

    fun getFiles(path: String = ""): MutableList<CustomFile> {
        val files = File("/storage/emulated/0/${rootFolderName}", path).listFiles()
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