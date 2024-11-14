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
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope
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
import com.example.my_launcher.notes.Notes
import com.example.my_launcher.notes.NotesPage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.lang.reflect.Method
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/* TODO Launcher
- bug: when installed a completely new app and updating the lists. The hitbox for button J broke when the app was alone in J (could be the letters hitboxes being incorrect or commpletely off)
- refresh app list after install
- Fix app select background (currently grey)
- set text color dynamically depending on background color
- blur background when list is open (https://source.android.com/docs/core/display/window-blurs) (theme: <item name="android:backgroundDimAmount">0</item>) (https://proandroiddev.com/creating-dynamic-background-blur-with-jetpack-compose-in-android-c53bef7fb98a)
- make it swipeable to open the status bar by using permission EXPAND_STATUS_BAR (use setExpandNotificationDrawer(true))
- Handle back button event, BackHandler { }
*/

/* TODO Notes
- bug: when file is moved, need to refresh directory to move again (problem is that the file doesn't exist)
- bug: moving folder to root when it's already in root deletes the folder.
- bug: found issue where I want to copy folder up a level or two.
- bug: found issue where I can't copy file from a folder to a parent to that folder (aka move it up the hierarchy)
*/

/* TODO look into permissions
<!-- A launcher app should be available all the time. -->
<uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS"/>

<uses-permission android:name="android.permission.ACCESS_RESTRICTED_SETTINGS" />

<uses-permission android:name="android.permission.EXPAND_STATUS_BAR" />
<uses-permission android:name="android.permission.STATUS_BAR"/>
<uses-permission android:name="android.permission.STATUS_BAR_SERVICE"/>
<uses-permission android:name="android.permission.BIND_DEVICE_ADMIN" />

<!-- Useful permissions lists
https://stuff.mit.edu/afs/sipb/project/android/docs/reference/android/Manifest.permission.html#BIND_ACCESSIBILITY_SERVICE
https://manifestdestiny.reveb.la/permissions/android.permission.system_overlay_window/index.html
-->

<uses-permission android:name="android.permission.BROADCAST_CLOSE_SYSTEM_DIALOGS" />
<uses-permission android:name="android.permission.CONTROL_REMOTE_APP_TRANSITION_ANIMATIONS"/>
<uses-permission android:name="android.permission.VIBRATE"/>
<uses-permission android:name="android.permission.START_TASKS_FROM_RECENTS"/>
<uses-permission android:name="android.permission.REMOVE_TASKS"/>
<uses-permission android:name="android.permission.WRITE_SECURE_SETTINGS"/>
<uses-permission android:name="android.permission.MANAGE_ACTIVITY_TASKS"/>
<uses-permission android:name="android.permission.INTERNAL_SYSTEM_WINDOW"/> could be used for systembar partial open
<uses-permission android:name="android.permission.STOP_APP_SWITCHES"/>
<uses-permission android:name="android.permission.READ_FRAME_BUFFER"/>
<uses-permission android:name="android.permission.MANAGE_ACCESSIBILITY"/>
<uses-permission android:name="android.permission.MONITOR_INPUT"/>
<uses-permission android:name="android.permission.ALLOW_SLIPPERY_TOUCHES"/>
<uses-permission android:name="android.permission.ACCESS_SHORTCUTS"/>
<uses-permission android:name="android.permission.SYSTEM_APPLICATION_OVERLAY" />
<!-- Permission required to access profiles which are otherwise hidden from being visible via APIs, e.g. private profile.-->
<uses-permission android:name="android.permission.ACCESS_HIDDEN_PROFILES_FULL" />
*/

/* Inspiration
https://www.youtube.com/watch?v=aVg3RkfNtqE
https://medium.com/@muhammadzaeemkhan/top-9-open-source-android-launchers-you-need-to-try-56c5f975e2f8
https://github.com/markusfisch/PieLauncher/tree/master
*/

@OptIn(ExperimentalFoundationApi::class)
class MainActivity: ComponentActivity() {
    private val customScope = CoroutineScope(AndroidUiDispatcher.Main)
    private lateinit var receiver: BroadcastReceiver

    private var date: String = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date())
    private var apps = mutableStateListOf<ApplicationInformation>()
    private var alphabet = mutableStateListOf<String>()
    private lateinit var lazyScroll: LazyListState

    private lateinit var dragState: AnchoredDraggableState<AnimatedContentTransitionScope. SlideDirection>
    private lateinit var dragState2: AnchoredDraggableState<AnimatedContentTransitionScope. SlideDirection>

    private lateinit var widgetHost: AppWidgetHost
    private lateinit var widgetManager: AppWidgetManager
    private var widgetId: Int = 0
    private lateinit var duoWidget: AppWidgetProviderInfo
    private lateinit var options: Bundle
    private lateinit var hostView: AppWidgetHostView

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
                hostView = widgetHost.createView(applicationContext, widgetId, duoWidget)
                hostView.setAppWidget(widgetId, duoWidget)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        @SuppressLint("SourceLockedOrientationActivity")
        this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        registerReceiver()
        val textColor = Color.White
        date = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date())
        var packages = getPackages()
        createAppList()
        createDuolingoWidget()
        val notes = Notes()
        notes.updateFiles()
        requestPermissions()

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
                duoWidget.updatePeriodMillis.toLong() // every 15 min
                hostView = widgetHost.createView(applicationContext, widgetId, duoWidget)
                hostView.setAppWidget(widgetId, duoWidget)
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
            dragState = remember {
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
            dragState2 = remember {
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
                hostView = hostView,
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
                enabled.value = dragState.requireOffset().roundToInt() == -screenWidth.roundToInt() && dragState2.requireOffset().roundToInt() == 0
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
                updateFiles = notes::updateFiles,
                saveFile = notes::saveFile,
                saveFileOverride = notes::overrideFile,
                readFile = notes::readFile,
                saveFolder = notes::saveFolder,
                moveFile = notes::moveFile,
                deleteFiles = notes::deleteFileAndChildren,
                files= notes.files,
                rootFolderName = notes.rootFolderName,
                rootPath = "/storage/emulated/0/${notes.rootFolderName}"
            )
        }
    }

    private fun requestPermissions() {
        if (Environment.isExternalStorageManager()) {
            return
        }

        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).also {
            it.data = Uri.parse("package:${packageName}")
        }
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        widgetHost.stopListening()
        widgetHost.deleteAppWidgetId(widgetId) // needed?
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

        if (!widgetManager.bindAppWidgetIdIfAllowed(widgetId, duoWidget.provider)) {
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, duoWidget.provider)
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_OPTIONS, options)

            requestWidgetPermissionsLauncher.launch(intent)
        }

        hostView = widgetHost.createView(applicationContext, widgetId, duoWidget)
        hostView.setAppWidget(widgetId, duoWidget)
    }

    private fun registerReceiver() {
        val filters = IntentFilter()
        receiver = object: BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
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

                    Intent.ACTION_APPLICATION_PREFERENCES, // intent for adjusting app preferences. recommended for all apps with settings
                    Intent.ACTION_PACKAGE_CHANGED, // broadcast a package has been changed
                    Intent.ACTION_PACKAGE_INSTALL, // deprecated broadcast trigger download and install of package
                    -> { }

                    Intent.ACTION_MEDIA_BUTTON -> {
                        println("ACTION_MEDIA_BUTTON")
                        println(intent.extras)
                    }

                    Intent.ACTION_CLOSE_SYSTEM_DIALOGS -> {
                        createAppList()
                        customScope.launch {
                            lazyScroll.scrollToItem(0)
                            dragState.updateAnchors(DraggableAnchors {
                                Start at 0f
                                End at 0f
                            })

                            dragState.updateAnchors(DraggableAnchors {
                                Start at 0f
                                End at -1080f
                            })

                            dragState2.updateAnchors(DraggableAnchors {
                                Start at 0f
                                End at 0f
                            })

                            dragState2.updateAnchors(DraggableAnchors {
                                Start at 0f
                                End at -2340f
                            })
                        }
                    }

                    Intent.ACTION_DATE_CHANGED,
                    Intent.ACTION_TIMEZONE_CHANGED,
                    Intent.ACTION_TIME_CHANGED -> {
                        date = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date())
                    }
                }
            }
        }

        registerReceiver(receiver, filters, RECEIVER_NOT_EXPORTED)
    }
}