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
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Companion.End
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Companion.Start
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_launcher.app_drawer.AppDrawer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/* TODO Launcher
- check out recompositions. reduce them as much as possible

- fix widget host (error logs and not updating)
- fix app & alphabet list not updating when uninstalling app (look at files in notes)
- bug: when installed a completely new app and updating the lists. The hitbox for button J broke when the app was alone in J (could be the letters hitboxes being incorrect or commpletely off)
- refresh app list after install
- fix app select background (currently grey)
*/

// Inspiration: https://github.com/markusfisch/PieLauncher/tree/master

@OptIn(ExperimentalFoundationApi::class)
class MainActivity: ComponentActivity() {
    private lateinit var appDrawer: AppDrawer
    private var date: String = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date())
    private lateinit var listState: LazyListState
    private lateinit var dragState: AnchoredDraggableState<AnimatedContentTransitionScope. SlideDirection>
    private val customScope = CoroutineScope(AndroidUiDispatcher.Main)
    private lateinit var receiver: BroadcastReceiver

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
        date = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date())
        var packages = appDrawer.getPackages()
        appDrawer.createAppList()
        appDrawer.createDuolingoWidget()

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.Transparent.toArgb()),
            navigationBarStyle = SystemBarStyle.dark(Color.Transparent.toArgb()),
        )

        setContent {
            BackHandler {
                backToHome()
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
                color = Color.White,
            )

            val screenHeight = 2340f
            val bottomBar = 48f

            val decayAnimationSpec = rememberSplineBasedDecay<Float>()
            dragState = remember {
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

            val floatAnim = animateFloatAsState(
                targetValue = if (dragState.lastVelocity < 0) 0.5f else 0f,
                tween(
                    durationMillis = 1000,
                    easing = LinearEasing,
                ),
                label = "",
            )
            window.setDimAmount(floatAnim.value)

            Box(
                modifier = Modifier
                    .anchoredDraggable(
                        state = dragState,
                        enabled = true,
                        orientation = Orientation.Vertical
                    )
                    .fillMaxSize()
                    .offset {
                        IntOffset(
                            0,
                            dragState
                                .requireOffset()
                                .roundToInt()
                        )
                    }
            )

            // create app list and alphabet list when scrolled down to bottom
            LaunchedEffect(dragState.requireOffset().roundToInt() == -screenHeight.roundToInt()) {
                if (dragState.requireOffset().roundToInt() == -screenHeight.roundToInt()) {
                    val i = Intent(Intent.ACTION_MAIN, null)
                    i.addCategory(Intent.CATEGORY_LAUNCHER)
                    val pk: List<ResolveInfo> = packageManager.queryIntentActivities(i, PackageManager.GET_META_DATA)
                    if (packages.size != pk.size || packages.toSet() != pk.toSet()) {
                        appDrawer.createAppList()
                        packages = pk
                    }
                }
            }

            listState = rememberLazyListState()

            AppDrawer(
                modifier = Modifier
                    .offset { IntOffset(0, dragState.requireOffset().roundToInt() + screenHeight.roundToInt()) },
                lazyScroll = listState,
                hostView = appDrawer.hostView,
                alphabet = appDrawer.alphabet,
                apps = appDrawer.apps,
                customScope = customScope,
                launchApp = { app -> appDrawer.launchApp(app) },
                hideApp = { app -> appDrawer.hideApp(app) },
                uninstallApp = { app -> appDrawer.uninstallApp(app) },
                textColor = Color.White,
                bottomBar = bottomBar,
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        appDrawer.deleteWidget()
    }

    private fun backToHome() {
        customScope.launch {
            listState.scrollToItem(0)
            dragState.updateAnchors(DraggableAnchors {
                Start at 0f
                End at 0f
            })

            dragState.updateAnchors(DraggableAnchors {
                Start at 0f
                End at -2340f
            })
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
                        backToHome()
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