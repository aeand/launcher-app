package se.anton.my_launcher

import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FileManager(context: Context) {
    private var rootFile: File? = context.getExternalFilesDir("")

    init {
        if (rootFile != null) {
            rootFile!!.mkdirs()
        } else {
            rootFile = File("/storage/emulated/0/Android/data/com.example.my_launcher/files/")
        }

        val logs = File(rootFile, "logs.txt")
        logs.createNewFile()
        logs.appendText("\n${SimpleDateFormat("y-M-d H:m:s", Locale.getDefault()).format(Date())}")

        File(rootFile, "hidden_packages.txt").createNewFile()
    }

    fun hidePackage(packageName: String) {
        val file = File(rootFile, "hidden_packages.txt")
        if (!file.exists()) {
            file.createNewFile()
        }

        val fileContent = FileInputStream(file).bufferedReader().use { it.readText() }
        if (!fileContent.contains(packageName)) {
            file.appendText(if (fileContent.isEmpty()) packageName else ",$packageName")
        }
    }

    fun showPackage(packageName: String) {
        val file = File(rootFile, "hidden_packages.txt")
        if (!file.exists()) {
            file.createNewFile()
        }

        val fileContent = FileInputStream(file).bufferedReader().use { it.readText() }
        if (fileContent.contains(packageName)) {
            file.writeText(
                fileContent.split(",")
                    .filter { it != packageName }
                    .joinToString(",") { it }
            )
        }
    }

    fun getHiddenPackages(): List<String> {
        val file = File(rootFile, "hidden_packages.txt")
        if (!file.exists()) {
            file.createNewFile()
        }

        val fileContent = FileInputStream(file).bufferedReader().use { it.readText() }
        return fileContent.split(",")
    }

    fun validateHiddenPackages(packages: List<String>) {
        val file = File(rootFile, "hidden_packages.txt")
        if (!file.exists()) {
            file.createNewFile()
        }

        val fileContent = FileInputStream(file).bufferedReader().use { it.readText() }
        file.writeText(
            fileContent.split(",")
                .filter { packages.contains(it) }
                .joinToString(",") { it }
        )
    }
}