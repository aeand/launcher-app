package com.example.my_launcher

import android.R.attr.maxHeight
import android.R.attr.maxWidth
import android.R.attr.minHeight
import android.annotation.SuppressLint
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.os.Bundle
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.reflect.Method
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt


/* TODO

*/

/* Features
1. blur background when list is open
2. make home button default to top of app list and open the wallpaper view
3. make alpha list letter follow when dragging
4. add settings. I wanna hide specific apps
5. set text color dynamically depending on background color
6. add to app list: a letter at the top of the section for that letter. And a line. to improve readability
7. add a notes feature on swipe right or something
8. make it swipeable to open the status bar by using permission EXPAND_STATUS_BAR (use setExpandNotificationDrawer(true))
9. could add that I can delete packages from list with REQUEST_DELETE_PACKAGES
10. could do something with permission VIBRATE
11. enforce portrait orientation with permissions
*/

/* Inspiration
https://www.youtube.com/watch?v=aVg3RkfNtqE
https://medium.com/@muhammadzaeemkhan/top-9-open-source-android-launchers-you-need-to-try-56c5f975e2f8
*/

/* Resources
https://developer.android.com/develop/ui/views/appwidgets/host
https://medium.com/@philipp.cherubim/how-to-display-widgets-inside-your-app-f3885cc27cff
https://stackoverflow.com/questions/77911492/appwidgethostview-is-not-updating-inside-androidview-composable
https://stackoverflow.com/questions/14000415/binding-widgets-in-custom-launcher
https://stackoverflow.com/questions/26847824/android-adding-widgets-to-app-programmatically-warning-message
https://stackoverflow.com/questions/77532675/how-to-host-and-draw-installed-app-widgets-in-a-compose-app
*/

class MainActivity : ComponentActivity() {
    private val customScope = CoroutineScope(AndroidUiDispatcher.Main)
    private var widgetHost: AppWidgetHost? = null
    private var widgetManager: AppWidgetManager? = null
    private var widgetId: Int? = null
    private var duoWidget: AppWidgetProviderInfo? = null
    private var options: Bundle? = null
    private var hostView: AppWidgetHostView? = null

    private var requestWidgetPermissionsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        println(result)
        if (result.resultCode == RESULT_OK) {
            println("onActivityResult: ${widgetManager!!.bindAppWidgetIdIfAllowed(widgetId!!, duoWidget!!.provider, options)}")
            if (widgetManager!!.bindAppWidgetIdIfAllowed(widgetId!!, duoWidget!!.provider, options)) {
                hostView = widgetHost!!.createView(applicationContext, widgetId!!, duoWidget)
                hostView!!.setAppWidget(widgetId!!, duoWidget)
            }
        }
    }

    class ApplicationInformation {
        var label: String? = null
        var packageName: String? = null
        var icon: Drawable? = null
    }

    @OptIn(ExperimentalFoundationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val textColor = Color.White
        var date = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date())
        val packageIntent = Intent(Intent.ACTION_MAIN, null)
        packageIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        var packages: List<ResolveInfo> = packageManager.queryIntentActivities(packageIntent, PackageManager.GET_META_DATA)
        var apps = createAppList()
        var alphabet = createAlphabetList(apps)

        widgetHost = AppWidgetHost(applicationContext, 0)
        widgetHost!!.startListening()
        widgetManager = AppWidgetManager.getInstance(applicationContext)
        duoWidget = widgetManager!!.installedProviders.find { it.activityInfo.name.contains("com.duolingo.streak.streakWidget.MediumStreakWidgetProvider") }
        widgetId = widgetHost!!.allocateAppWidgetId()

        options = Bundle()
        options!!.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, maxWidth)
        options!!.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, minHeight)
        options!!.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, maxWidth)
        options!!.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, maxHeight)

        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, duoWidget!!.provider)
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_OPTIONS, options)

        if (!widgetManager!!.bindAppWidgetIdIfAllowed(widgetId!!, duoWidget!!.provider)) {
            println("invalid")
            requestWidgetPermissionsLauncher.launch(intent)
        }

        hostView = widgetHost!!.createView(applicationContext, widgetId!!, duoWidget)
        hostView!!.setAppWidget(widgetId!!, duoWidget)

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
                delay(3600000)
                val newDate = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date())
                if (date != newDate)
                    date = newDate
            }

            Text(
                modifier = Modifier
                    .padding(start = 19.dp, top = 30.dp),
                text = date,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight(600)
            )

            val screenWidth = 1080f
            val screenHeight = 2340f

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

            if (dragState2.requireOffset().roundToInt() == -screenHeight.roundToInt()) {
                val i = Intent(Intent.ACTION_MAIN, null)
                i.addCategory(Intent.CATEGORY_LAUNCHER)
                val pk: List<ResolveInfo> = packageManager.queryIntentActivities(i, PackageManager.GET_META_DATA)
                if (packages.size == pk.size && packages.toSet() == pk.toSet()) {
                    apps = createAppList()
                    alphabet = createAlphabetList(apps)
                    packages = pk
                }
            }

            AppDrawer(
                modifier = Modifier
                    .offset { IntOffset(0, dragState2.requireOffset().roundToInt() + screenHeight.roundToInt()) },
                hostView = hostView,
                alphabet = alphabet,
                apps = apps,
                customScope = customScope,
                launchApp = ::launchApp,
                scrollToFirstItem = ::scrollToFirstItem,
                textColor = textColor,
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset {
                        IntOffset(
                            dragState
                                .requireOffset()
                                .roundToInt() + screenWidth.roundToInt(),
                            dragState2
                                .requireOffset()
                                .roundToInt()
                        )
                    }
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(100.dp)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.White,
                                    Color.Cyan
                                )
                            )
                        )
                )
                Text(
                    modifier = Modifier
                        .align(Alignment.Center),
                    text = "Notes page!"
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        widgetHost!!.stopListening()
        if (widgetHost != null)
            widgetHost!!.deleteAppWidgetId(widgetId!!)
    }

    private fun launchApp(packageName: String?) {
        if (packageName == null)
            return

        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        startActivity(launchIntent)
    }

    private fun scrollToFirstItem(apps: MutableList<ApplicationInformation>, letter: String, lazyScroll: LazyListState) {
        customScope.launch {
            var i = 0
            var found = false

            apps.forEachIndexed { index, app ->
                if (!found && app.label != null && app.label!![0].uppercaseChar() == letter.toCharArray()[0].uppercaseChar()) {
                    i = index
                    found = true
                }
            }

            lazyScroll.animateScrollToItem(i)
        }
    }

    private fun createAppList(): MutableList<ApplicationInformation> {
        val intent = Intent(Intent.ACTION_MAIN, null)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        val packages: List<ResolveInfo> = packageManager.queryIntentActivities(intent, PackageManager.GET_META_DATA)

        val apps = mutableListOf<ApplicationInformation>()
        for (app in packages) {
            val appInfo = ApplicationInformation()
            appInfo.label = app.loadLabel(packageManager).toString()
            appInfo.packageName = app.activityInfo.packageName
            appInfo.icon = app.loadIcon(packageManager)
            apps.add(appInfo)
        }
        apps.sortWith { a, b ->
            a.label?.uppercase()!!.compareTo(b.label?.uppercase()!!)
        }

        return apps
    }

    private fun createAlphabetList(apps: MutableList<ApplicationInformation>): MutableList<String> {
        val tempAlphabet = "1234567890qwertyuiopasdfghjklzxcvbnm".split("").dropLast(1).toMutableList()
        val alphabet = tempAlphabet.subList(1, tempAlphabet.size)
        alphabet.sortWith { a, b ->
            a.compareTo(b)
        }
        alphabet.add("å")
        alphabet.add("ä")
        alphabet.add("ö")

        val removeLetters = mutableListOf<String>()
        alphabet.forEach { letter ->
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
            alphabet.remove(letter)
        }

        return alphabet
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
}

@Composable
fun AppDrawer(
    modifier: Modifier,
    hostView: AppWidgetHostView?,
    customScope: CoroutineScope,
    textColor: Color,
    apps: MutableList<MainActivity.ApplicationInformation>,
    launchApp: (String?) -> Unit,
    alphabet: MutableList<String>,
    scrollToFirstItem: (MutableList<MainActivity.ApplicationInformation>, String, LazyListState) -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 10.dp, end = 10.dp)
    ) {
        val lazyScroll = rememberLazyListState()

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.4f)
        ) {
            AndroidView(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .width(500.dp)
                    .height(269.dp),
                factory = { hostView!! }
            )

            Icon(
                modifier = Modifier
                    .width(36.dp)
                    .height(30.dp)
                    .align(Alignment.BottomEnd)
                    .clickable {
                        customScope.launch {
                            lazyScroll.animateScrollToItem(0)
                        }
                    },
                imageVector = Icons.Rounded.KeyboardArrowUp,
                contentDescription = null,
                tint = textColor
            )
        }

        Row(
            modifier = Modifier
                .padding(start = 10.dp, end = 10.dp)
                .fillMaxWidth()
                .fillMaxHeight(0.6f)
                .align(Alignment.BottomEnd),
            horizontalArrangement = Arrangement.End
        ) {
            LazyColumn(
                modifier = Modifier
                    .padding(end = 20.dp),
                state = lazyScroll,
                horizontalAlignment = Alignment.End
            ) {
                apps.forEach { app ->
                    item {
                        Row(
                            modifier = Modifier
                                .padding(bottom = 20.dp)
                                .clickable {
                                    launchApp(app.packageName)
                                }
                        ) {
                            Text(
                                modifier = Modifier
                                    .align(Alignment.CenterVertically)
                                    .padding(end = 10.dp)
                                    .width(300.dp),
                                text = "${app.label}",
                                color = textColor,
                                fontSize = 24.sp,
                                fontWeight = FontWeight(weight = 700),
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1,
                                textAlign = TextAlign.End
                            )

                            Image(
                                modifier = Modifier
                                    .size(50.dp),
                                painter = rememberDrawablePainter(drawable = app.icon),
                                contentDescription = null
                            )
                        }
                    }
                }
            }

            var offsetY by remember { mutableFloatStateOf(0f) }
            var selectedLetter by remember { mutableStateOf("") }

            Column(
                modifier = Modifier
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                alphabet.forEach { letter ->
                    Text(
                        modifier = Modifier
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        try {
                                            selectedLetter = letter
                                            offsetY = -150f
                                            awaitRelease()
                                        } finally {
                                            scrollToFirstItem(
                                                apps,
                                                letter,
                                                lazyScroll
                                            )
                                            offsetY = 0f
                                            selectedLetter = ""
                                        }
                                    },
                                )
                            }
                            .offset {
                                if (selectedLetter == letter) IntOffset(
                                    0,
                                    offsetY.roundToInt()
                                ) else IntOffset(0, 0)
                            }
                            .drawBehind {
                                if (selectedLetter == letter)
                                    drawCircle(
                                        radius = 80f,
                                        color = Color.Black
                                    )
                            },
                        text = letter,
                        color = textColor,
                        fontSize = if (selectedLetter == letter) 40.sp else 16.sp,
                        fontWeight = FontWeight(600)
                    )
                }
            }
        }
    }
}