package com.example.my_launcher

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
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
import com.example.my_launcher.app_drawer.AppDrawer
import com.example.my_launcher.notes.Notes
import com.example.my_launcher.notes.NotesPage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.lang.reflect.Method
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/* TODO Launcher

FIXES
- fix widget host (error logs and not updating)
- fix app & alphabet list not updating when uninstalling app (look at files in notes)
- bug: when installed a completely new app and updating the lists. The hitbox for button J broke when the app was alone in J (could be the letters hitboxes being incorrect or commpletely off)
- refresh app list after install
- Fix app select background (currently grey)

FEATURES
- set text color dynamically depending on background color
- blur background when list is open (https://source.android.com/docs/core/display/window-blurs) (theme: <item name="android:backgroundDimAmount">0</item>) (https://proandroiddev.com/creating-dynamic-background-blur-with-jetpack-compose-in-android-c53bef7fb98a)
Add a low alpha, blurred background to app list
- open statusbar by swiping down by using permission EXPAND_STATUS_BAR (use setExpandNotificationDrawer(true))
- Handle back button event, BackHandler { }
- create custom dialog for hide/uninstall app

PERFORMANCE
- check out recompositions. reduce them as much as possible

*/

/* Inspiration
https://www.youtube.com/watch?v=aVg3RkfNtqE
https://medium.com/@muhammadzaeemkhan/top-9-open-source-android-launchers-you-need-to-try-56c5f975e2f8
https://github.com/markusfisch/PieLauncher/tree/master
*/

@OptIn(ExperimentalFoundationApi::class)
class MainActivity: ComponentActivity() {
    private lateinit var appDrawer: AppDrawer

    private val customScope = CoroutineScope(AndroidUiDispatcher.Main)
    private lateinit var receiver: BroadcastReceiver

    private var date: String = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date())
    private lateinit var lazyScroll: LazyListState

    private lateinit var dragState: AnchoredDraggableState<AnimatedContentTransitionScope. SlideDirection>
    private lateinit var dragState2: AnchoredDraggableState<AnimatedContentTransitionScope. SlideDirection>

    private var requestWidgetPermissionsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        println(result)
        if (result.resultCode == RESULT_OK) {
            println("onActivityResult: ${appDrawer.widgetManager.bindAppWidgetIdIfAllowed(appDrawer.widgetId, appDrawer.duoWidget.provider, appDrawer.options)}")
            if (appDrawer.widgetManager.bindAppWidgetIdIfAllowed(appDrawer.widgetId, appDrawer.duoWidget.provider, appDrawer.options)) {
                appDrawer.hostView = appDrawer.widgetHost.createView(applicationContext, appDrawer.widgetId, appDrawer.duoWidget)
                appDrawer.hostView.setAppWidget(appDrawer.widgetId, appDrawer.duoWidget)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        @SuppressLint("SourceLockedOrientationActivity")
        this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        appDrawer = AppDrawer(this, applicationContext, packageManager, requestWidgetPermissionsLauncher)

        registerReceiver()
        val textColor = Color.White
        date = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date())
        var packages = appDrawer.getPackages()
        appDrawer.createAppList()
        appDrawer.createDuolingoWidget()
        val notes = Notes()
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
                appDrawer.duoWidget.updatePeriodMillis.toLong() // every 30 min
                appDrawer.hostView = appDrawer.widgetHost.createView(applicationContext, appDrawer.widgetId, appDrawer.duoWidget)
                appDrawer.hostView.setAppWidget(appDrawer.widgetId, appDrawer.duoWidget)
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
                if (dragState2.requireOffset().roundToInt() == -screenHeight.roundToInt()) {
                    val i = Intent(Intent.ACTION_MAIN, null)
                    i.addCategory(Intent.CATEGORY_LAUNCHER)
                    val pk: List<ResolveInfo> = packageManager.queryIntentActivities(i, PackageManager.GET_META_DATA)
                    if (packages.size != pk.size || packages.toSet() != pk.toSet()) {
                        appDrawer.createAppList()
                        packages = pk
                    }
                }
            }

            lazyScroll = rememberLazyListState()

            AppDrawer(
                modifier = Modifier
                    .offset { IntOffset(0, dragState2.requireOffset().roundToInt() + screenHeight.roundToInt()) },
                lazyScroll = lazyScroll,
                hostView = appDrawer.hostView,
                alphabet = appDrawer.alphabet,
                apps = appDrawer.apps,
                customScope = customScope,
                launchApp = { app -> appDrawer.launchApp(app) },
                hideApp = { app -> appDrawer.hideApp(app) },
                uninstallApp = { app -> appDrawer.uninstallApp(app) },
                textColor = textColor,
                bottomBar = bottomBar,
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
                getFiles = notes::getFiles,
                saveFile = notes::saveFile,
                saveFileOverride = notes::overrideFile,
                readFile = notes::readFile,
                saveFolder = notes::saveFolder,
                moveFile = notes::moveFile,
                deleteFiles = notes::deleteFile,
                rootFolderName = notes.rootFolderName,
                rootPath = notes.root,
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        appDrawer.deleteWidget()
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
                        appDrawer.createAppList()
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
                        appDrawer.createAppList()
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