package com.example.my_launcher

import android.R.attr.maxHeight
import android.R.attr.maxWidth
import android.R.attr.minHeight
import android.annotation.SuppressLint
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Companion.End
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Companion.Start
import androidx.compose.animation.core.tween
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.lang.reflect.Method
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/* TODO Launcher
- bug: when installed a completely new app and updating the lists.
The hitbox for button J broke when the app was alone in J (could be the letters hitboxes being incorrect or commpletely off)
- refresh app list after install
- Fix app select background (currently grey)
- set text color dynamically depending on background color
- blur background when list is open
- make home button open the wallpaper view
- make it swipeable to open the status bar by using permission EXPAND_STATUS_BAR (use setExpandNotificationDrawer(true))
- Handle back button event, BackHandler { }
*/

/* TODO Notes
- press and hold to delete/move notes in directory
- store notes in folder in root
*/

/* Intent list that would be useful
- Intent.ACTION_CREATE_REMINDER for implementing reminders
- Intent.ACTION_CREATE_NOTE for implementing notes
- Intent.ACTION_SEARCH for implementing search
- Intent.ACTION_MEDIA_BUTTON for detecting volume? buttons
- Intent.ACTION_SHUTDOWN for detecting shutdown
- Intent.ACTION_SET_WALLPAPER for setting wallpapers
- Intent.ACTION_LAUNCH_CAPTURE_CONTENT_ACTIVITY_FOR_NOTE for potential screenshot previews or similar
- Intent.ACTION_VIEW_PERMISSION_USAGE for checking permissions
- Intent.CATEGORY_CAR_DOCK for drivvler
- Intent.CATEGORY_CAR_MODE for drivvler
- Intent.CATEGORY_HOME for opening home activity
- Intent.CATEGORY_INFO for fetching more info from app
- Intent.CATEGORY_LAUNCHER for opening launcher, maybe my app
- Intent.CATEGORY_LEANBACK_LAUNCHER for opening leanback launcher, maybe my app
- Intent.CATEGORY_PREFERENCE opening preferences
- Intent.CATEGORY_SECONDARY_HOME for opening home?
*/

/* Inspiration
https://www.youtube.com/watch?v=aVg3RkfNtqE
https://medium.com/@muhammadzaeemkhan/top-9-open-source-android-launchers-you-need-to-try-56c5f975e2f8
*/

class MainActivity: ComponentActivity() {
    private val customScope = CoroutineScope(AndroidUiDispatcher.Main)
    private lateinit var receiver: BroadcastReceiver

    private var date: String = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date())
    private var apps = mutableStateListOf<ApplicationInformation>()
    private var alphabet = mutableStateListOf<String>()
    private lateinit var lazyScroll: LazyListState

    private lateinit var widgetHost: AppWidgetHost
    private lateinit var widgetManager: AppWidgetManager
    private var widgetId: Int = 0
    private lateinit var duoWidget: AppWidgetProviderInfo
    private lateinit var options: Bundle
    private lateinit var hostView: MutableState<AppWidgetHostView>

    private var files = mutableStateListOf<CustomFile>()

    class CustomFile(
        val file: File,
        val children: MutableList<CustomFile>?,
        val indent: Int,
        var hidden: Boolean,
    )

    class ApplicationInformation {
        var label: String? = null
        var packageName: String? = null
        var icon: Drawable? = null
        var hidden: Boolean? = null
    }

    private var requestWidgetPermissionsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        println(result)
        if (result.resultCode == RESULT_OK) {
            println("onActivityResult: ${widgetManager.bindAppWidgetIdIfAllowed(widgetId, duoWidget.provider, options)}")
            if (widgetManager.bindAppWidgetIdIfAllowed(widgetId, duoWidget.provider, options)) {
                hostView = mutableStateOf(widgetHost.createView(applicationContext, widgetId, duoWidget))
                hostView.value.setAppWidget(widgetId, duoWidget)
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        val list = listOf(
            Intent.ACTION_APPLICATION_LOCALE_CHANGED, // broadcast some app locale has changed
            Intent.ACTION_APPLICATION_PREFERENCES, // intent for adjusting app preferences. recommended for all apps with settings
            Intent.ACTION_APPLICATION_RESTRICTIONS_CHANGED, // broadcast app restrictions changed
            Intent.ACTION_BATTERY_CHANGED, // sticky broadcast Has charging state, level, and more info
            Intent.ACTION_BATTERY_LOW, // broadcast battery is low
            Intent.ACTION_BATTERY_OKAY, // broadcast battery is okay after being low
            Intent.ACTION_BOOT_COMPLETED, // broadcast permissions only once after the user has finished booting
            Intent.ACTION_CAMERA_BUTTON, // broadcast camera button is pressed. Broadcast includes EXTRA_KEY_EVENT
            Intent.ACTION_CHOOSER, // opens an activity picker. Alternative to the standard picker
            Intent.ACTION_CLOSE_SYSTEM_DIALOGS, // broadcast user action to dismiss temporary system dialog
            Intent.ACTION_CONFIGURATION_CHANGED, // broadcast orientation, locale, etc has changed. UI will be rebuilt aka. system will stop and start app
            Intent.ACTION_DATE_CHANGED, // broadcast date has changed
            Intent.ACTION_DOCK_EVENT, // sticky broadcast changes in physical docking state
            Intent.ACTION_DREAMING_STARTED, // broadcast system started dreaming
            Intent.ACTION_DREAMING_STOPPED, // broadcast system stopped dreaming
            Intent.ACTION_HEADSET_PLUG, // broadcast wired headset plugged in or out
            Intent.ACTION_INPUT_METHOD_CHANGED, // broadcast input method changed
            Intent.ACTION_LAUNCH_CAPTURE_CONTENT_ACTIVITY_FOR_NOTE, // Use with startActivityForResult to start a system activity that captures content on the screen to take a screenshot and present it to the user for editing
            Intent.ACTION_LOCALE_CHANGED, // broadcast device locale changed
            Intent.ACTION_LOCKED_BOOT_COMPLETED, // broadcast when device has booted but still is in locked state
            Intent.ACTION_MANAGED_PROFILE_UNLOCKED, // broadcast received by primary user when a managed profile is unlocked. There are more profile related actions
            Intent.ACTION_MANAGE_NETWORK_USAGE, // shows settings for managing network data usage
            Intent.ACTION_MANAGE_PACKAGE_STORAGE, // broadcast Indicates low memory condition notification acknowledged by user
            Intent.ACTION_MANAGE_UNUSED_APPS, // opens UI to handle unused apps
            Intent.ACTION_MEDIA_BAD_REMOVAL, // broadcast SD card removed from slot but mount point was not unmounted
            Intent.ACTION_MEDIA_BUTTON, // media button was pressed. contains EXTRA_KEY_EVENT
            Intent.ACTION_MY_PACKAGE_REPLACED, // broadcast a new version of your app has been installed over an existing one
            Intent.ACTION_MY_PACKAGE_SUSPENDED, // broadcast Sent to a package that has been suspended by the system
            Intent.ACTION_MY_PACKAGE_UNSUSPENDED, // broadcast Sent to a package that has been unsuspended
            Intent.ACTION_PACKAGES_SUSPENDED, // broadcast packages have been suspended
            Intent.ACTION_PACKAGES_UNSUSPENDED, // broadcast Packages have been unsuspended
            Intent.ACTION_PACKAGE_ADDED, // broadcast new package installed. contains data with name
            Intent.ACTION_PACKAGE_CHANGED, // broadcast a package has been changed
            Intent.ACTION_PACKAGE_DATA_CLEARED, // broadcast user has cleared the data of a package
            Intent.ACTION_PACKAGE_FIRST_LAUNCH, // broadcast app launched for the first time
            Intent.ACTION_PACKAGE_FULLY_REMOVED, // broadcast package has been completely removed from device
            Intent.ACTION_PACKAGE_INSTALL, // deprecated broadcast trigger download and install of package
            Intent.ACTION_PACKAGE_NEEDS_VERIFICATION, // broadcast to the system package verifier when a package needs to be verified
            Intent.ACTION_PACKAGE_REMOVED, // broadcast app has been removed
            Intent.ACTION_PACKAGE_REPLACED, // broadcast new version of an application package has been installed
            Intent.ACTION_PACKAGE_RESTARTED, // broadcast user has restarted a package
            Intent.ACTION_PACKAGE_VERIFIED, // broadcast Sent to the system package verifier when a package is verified
            Intent.ACTION_PASTE, // create item in given container, initializing it from the current clipboard
            Intent.ACTION_POWER_CONNECTED, // broadcast External power has been connected to the device. will wake device
            Intent.ACTION_POWER_DISCONNECTED, // broadcast External power has been removed from device
            Intent.ACTION_POWER_USAGE_SUMMARY, // shows power usage info to user
            Intent.ACTION_PROVIDER_CHANGED, // broadcast providers content changed
            Intent.ACTION_SCREEN_OFF, // broadcast device goes to sleep and becomes non-interactive
            Intent.ACTION_SCREEN_ON, // broadcast device wakes up and becomes interactive
            Intent.ACTION_SEARCH, // perform a search
            Intent.ACTION_SEARCH_LONG_PRESS, // start action associated with long pressing on a search key
            Intent.ACTION_SET_WALLPAPER, // show settings for choosing wallpaper
            Intent.ACTION_SHOW_APP_INFO, // will show app information
            Intent.ACTION_SHUTDOWN, // broadcast device is shutting down
            Intent.ACTION_TIMEZONE_CHANGED, // broadcast timezone has changed. includes EXTRA_TIMEZONE
            //Intent.ACTION_TIME_CHANGED, // broadcast The time was set
            //Intent.ACTION_TIME_TICK, // broadcast time has changed. sent every minute. only receiver
            Intent.ACTION_UID_REMOVED, // broadcast a uid has been removed from the system. includes EXTRA_UID and EXTRA_REPLACING
            Intent.ACTION_UMS_CONNECTED, // deprecated broadcast the device has entered USB mass storage mode
            Intent.ACTION_UMS_DISCONNECTED, // deprecated broadcast the device has exited USB mass storage mode
            //Intent.ACTION_UNARCHIVE_PACKAGE // broadcast sent to the responsible installer. archived package when unarchival is requested
            Intent.ACTION_VIEW_PERMISSION_USAGE, // launch UI to show information about the usage of a given permission group
            Intent.ACTION_VIEW_PERMISSION_USAGE_FOR_PERIOD, // launch UI to show info about the usage of a given permission group in a given period
            Intent.ACTION_WALLPAPER_CHANGED, // deprecated broadcast the current system wallpaper has changed
            Intent.ACTION_WEB_SEARCH, // perform a web search
            //Intent.CAPTURE_CONTENT_FOR_NOTE_BLOCKED_BY_ADMIN // a response code used with EXTRA_CAPTURE_CONTENT_FOR_NOTE_STATUS_CODE
            //Intent.CAPTURE_CONTENT_FOR_NOTE_FAILED // A response code used with EXTRA_CAPTURE_CONTENT_FOR_NOTE_STATUS_CODE to indicate that something went wrong
            //Intent.CAPTURE_CONTENT_FOR_NOTE_SUCCESS // A response code used with EXTRA_CAPTURE_CONTENT_FOR_NOTE_STATUS_CODE to indicate that the request was a success
            //Intent.CAPTURE_CONTENT_FOR_NOTE_USER_CANCELED // A response code used with EXTRA_CAPTURE_CONTENT_FOR_NOTE_STATUS_CODE to indicate that user canceled the content capture flow
            //Intent.CAPTURE_CONTENT_FOR_NOTE_WINDOW_MODE_UNSUPPORTED // A response code used with EXTRA_CAPTURE_CONTENT_FOR_NOTE_STATUS_CODE to indicate that the intent action ACTION_LAUNCH_CAPTURE_CONTENT_ACTIVITY_FOR_NOTE was started by an activity that is running in a non-supported window mode
            Intent.CATEGORY_ACCESSIBILITY_SHORTCUT_TARGET, // the accessibility shortcut is global gesture for users with disabilities to trigger an important for them accessibility
            Intent.CATEGORY_ALTERNATIVE, // Set if the activity should be considered as an alternative action to the data the user is currently viewing
            Intent.CATEGORY_APP_BROWSER, // Used with ACTION_MAIN to launch the browser application
            Intent.CATEGORY_BROWSABLE, // Activities that can be safely invoked from a browser must support this category
            Intent.CATEGORY_CAR_DOCK, // An activity to run when device is inserted into a car dock
            Intent.CATEGORY_CAR_MODE, // Used to indicate that the activity can be used in a car environment
            Intent.CATEGORY_HOME, // This is the home activity, that is the first activity that is displayed when the device boots
            Intent.CATEGORY_INFO, // Provides information about the package it is in; typically used if a package does not contain a CATEGORY_LAUNCHER to provide a front-door to the user without having to be shown in the all apps list
            Intent.CATEGORY_LAUNCHER, // Should be displayed in the top-level launcher
            Intent.CATEGORY_LEANBACK_LAUNCHER, // Indicates an activity optimized for Leanback mode, and that should be displayed in the Leanback launcher
            Intent.CATEGORY_PREFERENCE, // This activity is a preference panel
            Intent.CATEGORY_SAMPLE_CODE, // To be used as a sample code example (not part of the normal user experience)
            Intent.CATEGORY_SECONDARY_HOME, // The home activity shown on secondary displays that support showing home activities
            // Skipped out on the EXTRA_
            Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT.toString(), // This flag is not normally set by application code, but set for you by the system as described in the launchMode documentation for the singleTask mode
            Intent.FLAG_ACTIVITY_CLEAR_TOP.toString(), // too long
            //Intent.FILL_IN_CLIP_DATA // Use with fillIn to allow the current ClipData to be overwritten, even if it is already set.
        )

        val filters = IntentFilter()
        list.forEach {
            filters.addAction(it)
        }

        receiver = object: BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                list.forEach {
                    when (intent.action) {
                        it -> {
                            println("Intent: " + it.split("android.intent.action.")[1])
                        }
                    }
                }

                when (intent.action) {
                    Intent.ACTION_APPLICATION_LOCALE_CHANGED,
                    Intent.ACTION_MY_PACKAGE_REPLACED,
                    Intent.ACTION_PACKAGE_ADDED,
                    Intent.ACTION_PACKAGE_FULLY_REMOVED,
                    Intent.ACTION_PACKAGE_REMOVED,
                    Intent.ACTION_PACKAGE_REPLACED,
                    Intent.ACTION_UID_REMOVED -> { // for uninstall
                        createAppList()
                    }

                    Intent.ACTION_CLOSE_SYSTEM_DIALOGS -> {
                        createAppList()
                        customScope.launch {
                            lazyScroll.scrollToItem(0)
                        }
                    }

                    Intent.ACTION_DATE_CHANGED,
                    Intent.ACTION_TIMEZONE_CHANGED -> {
                        date = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date())
                    }
                }
            }
        }

        registerReceiver(receiver, filters, RECEIVER_NOT_EXPORTED)

        val textColor = Color.White
        date = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date())
        var packages = getPackages()
        createAppList()
        createDuolingoWidget()
        updateFiles()

        setContent {
            val isDarkMode = isSystemInDarkTheme()
            val context = LocalContext.current as ComponentActivity
            DisposableEffect(isDarkMode) {
                context.enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.dark(Color.Transparent.toArgb()),
                    navigationBarStyle = SystemBarStyle.dark(Color.Transparent.toArgb()),
                )

                onDispose { }
            }

            LaunchedEffect(true) {
                customScope.launch {
                    delay(900000) // every 15 min

                    hostView.value = widgetHost.createView(applicationContext, widgetId, duoWidget)
                    hostView.value.setAppWidget(widgetId, duoWidget)
                }
            }

            Text(
                modifier = Modifier
                    .padding(start = 19.dp, top = 30.dp),
                text = date,
                fontSize = 11.sp,
                fontWeight = FontWeight(600),
                color = textColor,
            )

            val screenWidth = 1080f
            val screenHeight = 2340f
            val topBar = 32f
            val bottomBar = 48f

            val decayAnimationSpec = rememberSplineBasedDecay<Float>()
            val dragState = remember {
                AnchoredDraggableState(
                    initialValue = Start,
                    anchors = DraggableAnchors {
                        Start at 0f
                        End at -screenWidth
                    },
                    positionalThreshold = { d -> d * 0.9f},
                    velocityThreshold = { Float.POSITIVE_INFINITY },
                    snapAnimationSpec = tween(),
                    decayAnimationSpec = decayAnimationSpec
                )
            }
            val dragState2 = remember {
                AnchoredDraggableState(
                    initialValue = Start,
                    anchors = DraggableAnchors {
                        Start at 0f
                        End at -screenHeight
                    },
                    positionalThreshold = { d -> d * 0.9f},
                    velocityThreshold = { Float.POSITIVE_INFINITY },
                    snapAnimationSpec = tween(),
                    decayAnimationSpec = decayAnimationSpec
                )
            }

            val appDrawerClosed = dragState2.requireOffset().roundToInt() == 0

            Box(
                modifier = Modifier
                    .anchoredDraggable(
                        state = dragState,
                        enabled = appDrawerClosed,
                        orientation = Orientation.Horizontal
                    )
                    .anchoredDraggable(
                        state = dragState2,
                        enabled = true,
                        orientation = Orientation.Vertical
                    )
                    .fillMaxSize()
                    .offset {
                        IntOffset(
                            dragState
                                .requireOffset()
                                .roundToInt(),
                            dragState2
                                .requireOffset()
                                .roundToInt()
                        )
                    }
            )

            // create app list and alphabet list when scrolled down to bottom
            LaunchedEffect(dragState2.requireOffset().roundToInt() == -screenHeight.roundToInt()) {
                val i = Intent(Intent.ACTION_MAIN, null)
                i.addCategory(Intent.CATEGORY_LAUNCHER)
                val pk: List<ResolveInfo> = packageManager.queryIntentActivities(i, PackageManager.GET_META_DATA)
                if (packages.size != pk.size || packages.toSet() != pk.toSet()) {
                    createAppList()
                    packages = pk
                }
            }

            lazyScroll = rememberLazyListState()

            AppDrawer(
                modifier = Modifier
                    .offset { IntOffset(0, dragState2.requireOffset().roundToInt() + screenHeight.roundToInt()) },
                lazyScroll = lazyScroll,
                hostView = hostView.value,
                alphabet = alphabet,
                apps = apps,
                customScope = customScope,
                launchApp = ::launchApp,
                hideApp = ::hideApp,
                uninstallApp = ::uninstallApp,
                textColor = textColor,
                bottomBar = bottomBar
            )

            val error = remember {
                mutableStateOf(false)
            }

            val enabled = remember {
                mutableStateOf(false)
            }

            LaunchedEffect(dragState.requireOffset().roundToInt() == -screenWidth.roundToInt(), dragState2.requireOffset().roundToInt() != 0) {
                enabled.value = dragState.requireOffset().roundToInt() == -screenWidth.roundToInt()
                        && dragState2.requireOffset().roundToInt() == 0
            }

            NotesPage(
                Modifier
                    .padding(top = topBar.dp, bottom = bottomBar.dp)
                    .offset {
                        IntOffset(
                            dragState
                                .requireOffset()
                                .roundToInt() + screenWidth.roundToInt(),
                            dragState2
                                .requireOffset()
                                .roundToInt()
                        )
                    },
                error = error,
                enabled = enabled,
                textColor = textColor,
                saveFile = ::saveFile,
                saveFileOverride = ::overrideFile,
                readFile = ::readFile,
                saveFolder = ::saveFolder,
                moveFile = ::moveFile,
                files = files,
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        widgetHost.stopListening()
        widgetHost.deleteAppWidgetId(widgetId)
    }

    private fun saveFolder(name: String, path: String = "", showToast: Boolean = true) {
        val folder = File(applicationContext.getExternalFilesDir(null), path + name)

        if (!folder.exists()) {
            if (!folder.mkdir()) {
                println("ERROR Cannot create a directory!")
            } else {
                folder.mkdirs()
            }
        }

        updateFiles()

        if (showToast)
            Toast.makeText(applicationContext, "Folder saved", Toast.LENGTH_SHORT).show()
    }

    private fun overrideFile(name: String, folder: String, content: String, showToast: Boolean = true) {
        val letDirectory = File(applicationContext.getExternalFilesDir(null), folder)
        letDirectory.mkdirs()
        val file = File(letDirectory, "$name.txt")
        file.writeText(content)

        updateFiles()

        if (showToast)
            Toast.makeText(applicationContext, "Note overridden", Toast.LENGTH_SHORT).show()
    }

    private fun saveFile(name: String, folder: String = "", content: String, showToast: Boolean = true): Boolean {
        val letDirectory = File(applicationContext.getExternalFilesDir(null), folder)
        letDirectory.mkdirs()
        val file = File(letDirectory, "$name.txt")
        if (file.exists()) {
            return false
        }

        file.writeText(content)

        updateFiles()

        if (showToast)
            Toast.makeText(applicationContext, "Note saved", Toast.LENGTH_SHORT).show()

        return true
    }

    private fun readFile(file: File): String {
        return FileInputStream(file).bufferedReader().use {
            it.readText()
        }
    }

    private fun moveFile(sourceFileName: String, sourceFilePath: String, targetFile: CustomFile): Boolean {
        val sourceFile = files.find { it.file.name == sourceFileName &&  it.file.path == sourceFilePath } ?: return false
        val rootPath = "/storage/emulated/0/Android/data/com.example.my_launcher/files/"

        if (sourceFile.file.isFile) {
            if (targetFile.file.isFile) {
                val targetFilePath = targetFile.file.path.replace("/${targetFile.file.name}", "")

                if (sourceFile.file.path.replace("/${sourceFile.file.name}", "") == targetFilePath) {
                    println("file and file have the same name")
                    return false
                }

                val filesInPath = File(applicationContext.getExternalFilesDir(null), targetFile.file.path.replace(rootPath, "")).listFiles()
                if (filesInPath == null) {
                    println("found no files in ${rootPath + targetFile.file.path}")
                    return false
                }

                filesInPath.forEach {
                    if (sourceFile.file.name == it.name && it.isFile) {
                        println("path has file with the same name as source")
                        return false
                    }
                }

                try {
                    println("attempt copy to: $targetFilePath/$sourceFileName")
                    sourceFile.file.copyTo(File("$targetFilePath/$sourceFileName"))
                    try {
                        println("attempt delete")
                        sourceFile.file.delete()
                        Toast.makeText(applicationContext, "Note moved", Toast.LENGTH_SHORT).show()
                        updateFiles()
                        return true
                    } catch (e: Exception) {
                        println("delete failed $e")
                        return false
                    }
                } catch (e: Exception) {
                    println("copy failed $e")
                    return false
                }
            }
            else if (targetFile.file.isDirectory) {
                targetFile.children?.forEach {
                    if (sourceFile.file.name == it.file.name && it.file.isFile) {
                        println("found file with same name as source file")
                        return false
                    }
                }

                try {
                    println("attempt copy to: ${targetFile.file.path}/${sourceFile.file.name}")
                    sourceFile.file.copyTo(File("${targetFile.file.path}/${sourceFile.file.name}"))
                    try {
                        println("attempt delete")
                        sourceFile.file.delete()
                        Toast.makeText(applicationContext, "Note moved", Toast.LENGTH_SHORT).show()
                        updateFiles()
                        return true
                    } catch (e: Exception) {
                        println("delete failed $e")
                        return false
                    }
                } catch (e: Exception) {
                    println("copy failed $e")
                    return false
                }
            }
            else {
                println("target file is not file or folder")
                return false
            }
        }
        else if (sourceFile.file.isDirectory) {
            if (targetFile.file.isFile) {
                val targetFilePath = targetFile.file.path.replace("/${targetFile.file.name}", "")

                val filesInPath = File(applicationContext.getExternalFilesDir(null), targetFile.file.path.replace(rootPath, "")).listFiles()
                if (filesInPath == null) {
                    println("found no files in ${rootPath + targetFile.file.path}")
                    return false
                }

                filesInPath.forEach {
                    if (sourceFile.file.name == it.name && it.isDirectory) {
                        println("path has folder with the same name as source")
                        return false
                    }
                }

                try {
                    println("attempt copy to: $targetFilePath/${sourceFile.file.name}")
                    sourceFile.file.copyTo(File("$targetFilePath/${sourceFile.file.name}"))
                    try {
                        println("attempt delete")
                        sourceFile.file.delete()
                        Toast.makeText(applicationContext, "Note moved", Toast.LENGTH_SHORT).show()
                        updateFiles()
                        return true
                    } catch (e: Exception) {
                        println("delete failed $e")
                        return false
                    }
                } catch (e: Exception) {
                    println("copy failed $e")
                    return false
                }
            }
            else if (targetFile.file.isDirectory) {
                targetFile.children?.forEach {
                    if (sourceFile.file.name == it.file.name && it.file.isDirectory) {
                        println("target folder contains folder with the same name as source folder")
                    }
                }

                try {
                    println("attempt copy to: ${targetFile.file.path}/${sourceFile.file.name}")
                    sourceFile.file.copyTo(File("${targetFile.file.path}/${sourceFile.file.name}"))
                    try {
                        println("attempt delete")
                        sourceFile.file.delete()
                        Toast.makeText(applicationContext, "Note moved", Toast.LENGTH_SHORT).show()
                        updateFiles()
                        return true
                    } catch (e: Exception) {
                        println("delete failed $e")
                        return false
                    }
                } catch (e: Exception) {
                    println("copy failed $e")
                    return false
                }
            }
            else {
                println("target file is not file or folder")
                return false
            }
        }
        else {
            println("source file is not file or folder")
            return false
        }
    }

    private fun updateFiles() {
        files.clear()
        getFiles().forEach {
            files.add(it)
        }
    }

    private fun getFiles(path: String = ""): MutableList<CustomFile> {
        val files = File(applicationContext.getExternalFilesDir(null), path).listFiles()
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

    private fun launchApp(packageName: String?) {
        if (packageName == null)
            return

        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        startActivity(launchIntent)
    }

    private fun hideApp(packageName: String?) {
        val app = apps.find { it.packageName?.lowercase() == packageName?.lowercase() }
        apps.find { it.packageName?.lowercase() == packageName?.lowercase() }?.hidden = !app?.hidden!!
    }

    private fun uninstallApp(packageName: String?) {
        if (packageName == null)
            return

        val packageIntent = Intent(Intent.ACTION_MAIN, null)
        packageIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        val packages: List<ResolveInfo> = packageManager.queryIntentActivities(packageIntent, PackageManager.GET_META_DATA)
        val app = packages.find { it.activityInfo.packageName.lowercase() == packageName.lowercase() }

        if (app == null)
            return

        val intent = Intent(Intent.ACTION_DELETE)
        intent.data = Uri.parse("package:${app.activityInfo.packageName}")
        startActivity(intent)
    }

    @SuppressLint("WrongConstant")
    private fun setExpandNotificationDrawer(expand: Boolean) {
        try {
            val statusBarService = applicationContext.getSystemService("statusbar")
            val methodName = if (expand) "expandNotificationsPanel" else "collapsePanels"
            val statusBarManager: Class<*> = Class.forName("android.app.StatusBarManager")
            val method: Method = statusBarManager.getMethod(methodName)
            method.invoke(statusBarService)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getPackages(): List<ResolveInfo> {
        val intent = Intent(Intent.ACTION_MAIN, null)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        return packageManager.queryIntentActivities(intent, PackageManager.GET_META_DATA)
    }

    private fun createAppList() {
        val packages = getPackages()

        val appList = mutableListOf<ApplicationInformation>()
        packages.forEach { app ->
            val appInfo = ApplicationInformation()
            appInfo.label = app.loadLabel(packageManager).toString()
            appInfo.packageName = app.activityInfo.packageName
            appInfo.icon = app.loadIcon(packageManager)
            val previousApp = apps.find { it.packageName?.lowercase() == appInfo.packageName!!.lowercase() }
            if (previousApp != null)
                appInfo.hidden = previousApp.hidden
            else
                appInfo.hidden = false

            appList.add(appInfo)
        }
        appList.sortWith { a, b ->
            a.label?.uppercase()!!.compareTo(b.label?.uppercase()!!)
        }

        apps.clear()
        appList.forEach {
            apps.add(it)
        }

        createAlphabetList(apps)
    }

    private fun createAlphabetList(apps: List<ApplicationInformation>) {
        val tempAlphabet = "1234567890qwertyuiopasdfghjklzxcvbnm".split("").dropLast(1).toMutableList()
        val alphabetList = tempAlphabet.subList(1, tempAlphabet.size)
        alphabetList.sortWith { a, b ->
            a.compareTo(b)
        }
        alphabetList.add("å")
        alphabetList.add("ä")
        alphabetList.add("ö")

        val removeLetters = mutableListOf<String>()
        alphabetList.forEach { letter ->
            var result = false
            apps.forEach { app ->
                if (!result) {
                    if (app.label != null && app.label!![0].uppercaseChar() == letter.toCharArray()[0].uppercaseChar()) {
                        result = true
                    }
                }
            }

            if (!result) {
                removeLetters.add(letter)
            }
        }

        removeLetters.forEach { letter ->
            alphabetList.remove(letter)
        }

        alphabet.clear()
        alphabetList.forEach {
            alphabet.add(it)
        }
    }

    private fun createDuolingoWidget() {
        widgetHost = AppWidgetHost(applicationContext, 0)
        widgetHost.startListening()
        widgetManager = AppWidgetManager.getInstance(applicationContext)
        duoWidget = widgetManager.installedProviders.find {
            it.activityInfo.name.contains("com.duolingo.streak.streakWidget.MediumStreakWidgetProvider")
        }!!
        widgetId = widgetHost.allocateAppWidgetId()

        options = Bundle()
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, maxWidth)
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, minHeight)
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, maxWidth)
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, maxHeight)

        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, duoWidget.provider)
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_OPTIONS, options)

        if (!widgetManager.bindAppWidgetIdIfAllowed(widgetId, duoWidget.provider)) {
            requestWidgetPermissionsLauncher.launch(intent)
        }

        hostView = mutableStateOf(widgetHost.createView(applicationContext, widgetId, duoWidget))
        hostView.value.setAppWidget(widgetId, duoWidget)
    }
}