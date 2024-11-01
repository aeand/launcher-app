package com.example.my_launcher

/*
Box(
    modifier = Modifier.fillMaxSize()
) {
    val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "public.txt")
    val file1 = File(Environment.getExternalStorageDirectory(), "/Thoughts/public.txt")
    val file2 = File(applicationContext.getExternalFilesDir(null), "")

    Text(
        modifier = Modifier
            .align(Alignment.Center)
            .offset(0.dp, (-100).dp)
            .clickable {
                /*    //WRITE PRIVATE
                    val folder: File? = context.getExternalFilesDir("GeeksForGeeks")
                    val file = File(folder, "gfg.txt")
                    writeTextData(file, message.value, context)
                    message.value = ""
                    Toast.makeText(context, "Data saved privately", Toast.LENGTH_SHORT).show()
                */

                /*
                println(applicationContext.filesDir.toString() + "/Thoughts")
                println(Environment.getExternalStorageDirectory().path + "/Thoughts")
                println(applicationContext.getExternalFilesDir(null))

                val pathExternal = context.getFilesDir()
                val pathInternal = context.getExternalFilesDir(null)
                val pathIWant = Environment.getExternalStorageDirectory().path + "/Thoughts"
                println("1 $pathExternal")
                println("2 $pathInternal")
                println("3 $pathIWant")
                */

                /*if (checkPermissions()) {
                    println("data: ${readFile(file1)}")
                }
                else {
                    println("bad")
                }*/
            },
        text = "attempt",
        color = Color.White,
    )

    Text(
        modifier = Modifier
            .align(Alignment.Center)
            .size(100.dp)
            .offset((-50).dp, 0.dp)
            .clickable {
                writeFile(file, "Polyphemus")
            },
        text = "save",
        color = Color.White,
    )

    Text(
        modifier = Modifier
            .align(Alignment.Center)
            .size(100.dp)
            .offset(50.dp, 0.dp)
            .clickable {
                println(readFile(file))
                writeFile(file, "Odysseys")
            },
        text = "read",
        color = Color.White,
    )
}
*/

/*
private fun writeFile(file: File, data: String) {
    var fileOutputStream: FileOutputStream? = null
    try {
        fileOutputStream = FileOutputStream(file)
        fileOutputStream.write(data.toByteArray())
    } catch (e: Exception) {
        println("error $e")
    } finally {
        if (fileOutputStream != null) {
            try {
                fileOutputStream.close()
            } catch (e: IOException) {
                println("error $e")
            }
        }
    }
}

private fun readFile(file: File): String {
    var fileInputStream: FileInputStream? = null
    try {
        fileInputStream = FileInputStream(file)
        var i = -1
        val buffer = StringBuffer()
        while (fileInputStream.read().also {
                i = it
            } != -1) {
            buffer.append(i.toChar())
        }
        return buffer.toString()
    } catch (e: java.lang.Exception) {
        println("error $e")
    } finally {
        if (fileInputStream != null) {
            try {
                fileInputStream!!.close()
            } catch (e: IOException) {
                println("error $e")
            }
        }
    }

    return ""
}
*/

/*
private fun requestPermissions() {
    ActivityCompat.requestPermissions(
        this,
        arrayOf(
            Manifest.permission.MANAGE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
        ),
        0 //23
    )
}

private fun checkPermissions(): Boolean {
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
        requestPermissions()
        if (!shouldShowRequestPermissionRationale("android.permission.READ_EXTERNAL_STORAGE")) {
            println("failed read show rational")
        }

        println("failed read")
        return false
    }
    else if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
        requestPermissions()
        if (!shouldShowRequestPermissionRationale("android.permission.WRITE_EXTERNAL_STORAGE")) {
            println("failed write rational")
        }

        println("failed write")
        return false
    }
    else {
        println("success!")
        return true
    }
}
*/

/*
// Request code for creating a PDF document.
private val CREATE_FILE = 1
private fun createFile(pickerInitialUri: Uri) {
    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = "application/pdf"
        putExtra(Intent.EXTRA_TITLE, "invoice.pdf")

        // Optionally, specify a URI for the directory that should be opened in
        // the system file picker before your app creates the document.
        putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri)
    }
    startActivityForResult(intent, CREATE_FILE)
}

// Request code for selecting a PDF document.
private val PICK_PDF_FILE = 2
fun openFile(pickerInitialUri: Uri) {
    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = "application/pdf"

        // Optionally, specify a URI for the file that should appear in the
        // system file picker when it loads.
        putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri)
    }

    startActivityForResult(intent, PICK_PDF_FILE)
}

fun openDirectory(pickerInitialUri: Uri, yourRequestCode: Int) {
    // Choose a directory using the system's file picker.
    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
        // Optionally, specify a URI for the directory that should be opened in
        // the system file picker when it loads.
        putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri)
    }

    startActivityForResult(intent, yourRequestCode)
}
*/