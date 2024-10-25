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
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.lang.reflect.Method
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/* TODO
- refresh app list after uninstall and install
- set text color dynamically depending on background color
- blur background when list is open
- make home button open the wallpaper view
- add a notes feature on swipe right
- make it swipeable to open the status bar by using permission EXPAND_STATUS_BAR (use setExpandNotificationDrawer(true))
*/

/* Inspiration
https://www.youtube.com/watch?v=aVg3RkfNtqE
https://medium.com/@muhammadzaeemkhan/top-9-open-source-android-launchers-you-need-to-try-56c5f975e2f8
*/

class MainActivity : ComponentActivity() {
    private val customScope = CoroutineScope(AndroidUiDispatcher.Main)
    private lateinit var receiver: BroadcastReceiver

    private var date: String = ""
    private var apps: MutableList<ApplicationInformation>? = null
    private lateinit var lazyScroll: LazyListState

    private lateinit var widgetHost: AppWidgetHost
    private lateinit var widgetManager: AppWidgetManager
    private var widgetId: Int = 0
    private lateinit var duoWidget: AppWidgetProviderInfo
    private lateinit var options: Bundle
    private lateinit var hostView: AppWidgetHostView

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

    class ApplicationInformation {
        var label: String? = null
        var packageName: String? = null
        var icon: Drawable? = null
        var hidden: Boolean? = null
    }

    @OptIn(ExperimentalFoundationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        val filters = IntentFilter()
        filters.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
        filters.addAction(Intent.ACTION_DATE_CHANGED)
        filters.addAction(Intent.ACTION_PACKAGE_ADDED)
        filters.addAction(Intent.ACTION_UNINSTALL_PACKAGE)
        filters.addAction(Intent.ACTION_PACKAGE_ADDED)
        filters.addAction(Intent.ACTION_INSTALL_PACKAGE)
        filters.addAction(Intent.ACTION_PACKAGE_REMOVED)
        filters.addAction(Intent.ACTION_PACKAGE_REPLACED)
        filters.addAction(Intent.ACTION_PACKAGE_INSTALL)

        receiver = object:BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action.equals(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) {
                    customScope.launch {
                        lazyScroll.scrollToItem(0)
                        //TODO also make this scroll the anchorDraggable back to start
                    }
                }
                else if (intent.action.equals(Intent.ACTION_DATE_CHANGED)) {
                    date = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date())
                }
                else if (intent.action.equals(Intent.ACTION_DELETE)) {
                    println("REOIADNFOAEUGB")
                    createAppList()
                }
                else if (intent.action.equals(Intent.ACTION_UNINSTALL_PACKAGE)) {
                    println("ACTION_UNINSTALL_PACKAGE")
                }
                else if (intent.action.equals(Intent.ACTION_PACKAGE_ADDED)) {
                    println("ACTION_PACKAGE_ADDED")
                }
                else if (intent.action.equals(Intent.ACTION_INSTALL_PACKAGE)) {
                    println("ACTION_INSTALL_PACKAGE")
                }
                else if (intent.action.equals(Intent.ACTION_PACKAGE_REMOVED)) {
                    println("ACTION_PACKAGE_REMOVED")
                }
                else if (intent.action.equals(Intent.ACTION_PACKAGE_REPLACED)) {
                    println("ACTION_PACKAGE_REPLACED")
                }
                else if (intent.action.equals(Intent.ACTION_PACKAGE_INSTALL)) {
                    println("ACTION_PACKAGE_INSTALL")
                }
            }
        }

        registerReceiver(receiver, filters, RECEIVER_NOT_EXPORTED)

        val textColor = Color.White
        date = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date())
        val packageIntent = Intent(Intent.ACTION_MAIN, null)
        packageIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        var packages: List<ResolveInfo> = packageManager.queryIntentActivities(packageIntent, PackageManager.GET_META_DATA)
        createAppList()
        var alphabet = createAlphabetList(apps!!)
        createDuolingoWidget()

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

            BackHandler {
                //TODO
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
                    createAppList()
                    alphabet = createAlphabetList(apps!!)
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
                apps = apps!!,
                customScope = customScope,
                launchApp = ::launchApp,
                hideApp = ::hideApp,
                uninstallApp = ::uninstallApp,
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
        widgetHost.stopListening()
        widgetHost.deleteAppWidgetId(widgetId)
    }

    private fun launchApp(packageName: String?) {
        if (packageName == null)
            return

        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        startActivity(launchIntent)
    }

    private fun hideApp(packageName: String?) {
        val app = apps?.find { it.packageName?.lowercase() == packageName?.lowercase() }
        apps?.find { it.packageName?.lowercase() == packageName?.lowercase() }?.hidden = !app?.hidden!!
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

    private fun createAppList() {
        val intent = Intent(Intent.ACTION_MAIN, null)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        val packages: List<ResolveInfo> = packageManager.queryIntentActivities(intent, PackageManager.GET_META_DATA)

        val appList = mutableListOf<ApplicationInformation>()
        packages.forEach { app ->
            val appInfo = ApplicationInformation()
            appInfo.label = app.loadLabel(packageManager).toString()
            appInfo.packageName = app.activityInfo.packageName
            appInfo.icon = app.loadIcon(packageManager)
            val previousApp = apps?.find { it.packageName?.lowercase() == appInfo.packageName!!.lowercase() }
            if (previousApp != null)
                appInfo.hidden = previousApp.hidden
            else
                appInfo.hidden = false

            appList.add(appInfo)
        }
        appList.sortWith { a, b ->
            a.label?.uppercase()!!.compareTo(b.label?.uppercase()!!)
        }

        apps = appList
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

    private fun createDuolingoWidget() {
        widgetHost = AppWidgetHost(applicationContext, 0)
        widgetHost.startListening()
        widgetManager = AppWidgetManager.getInstance(applicationContext)
        duoWidget = widgetManager.installedProviders.find { it.activityInfo.name.contains("com.duolingo.streak.streakWidget.MediumStreakWidgetProvider") }!!
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
            println("invalid")
            requestWidgetPermissionsLauncher.launch(intent)
        }

        hostView = widgetHost.createView(applicationContext, widgetId, duoWidget)
        hostView.setAppWidget(widgetId, duoWidget)
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

@SuppressLint("UseOfNonLambdaOffsetOverload")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppDrawer(
    modifier: Modifier,
    lazyScroll: LazyListState,
    hostView: AppWidgetHostView?,
    customScope: CoroutineScope,
    textColor: Color,
    apps: MutableList<MainActivity.ApplicationInformation>,
    launchApp: (String?) -> Unit,
    hideApp: (String?) -> Unit,
    uninstallApp: (String?) -> Unit,
    alphabet: MutableList<String>,
    scrollToFirstItem: (MutableList<MainActivity.ApplicationInformation>, String, LazyListState) -> Unit
) {
    val showAllApps = remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 10.dp, end = 10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.4f)
        ) {
            Icon(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(if (showAllApps.value) 30.dp else 20.dp)
                    .offset(
                        x = if (showAllApps.value) -35.dp else -40.dp,
                        y = if (showAllApps.value) -1.dp else -5.dp
                    )
                    .clickable {
                        showAllApps.value = !showAllApps.value
                    },
                painter = painterResource(id = if (showAllApps.value) R.drawable.eye_cross else R.drawable.eye),
                contentDescription = null,
                tint = textColor
            )

            AndroidView(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(0.dp, -50.dp)
                    .width(350.dp)
                    .height(200.dp),
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
                .align(Alignment.BottomEnd)
                .padding(bottom = 48.dp),
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
                        if (showAllApps.value || !app.hidden!!) {
                            val showOptions = remember { mutableStateOf(false) }
                            val firstAppWithLetter = apps.find { it.label?.uppercase()?.startsWith(app.label?.uppercase()!![0])!! }!!

                            if (app.label?.uppercase() == firstAppWithLetter.label?.uppercase()) {
                                Row(
                                    modifier = Modifier
                                        .padding(bottom = 4.dp),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(150.dp)
                                            .height(1.dp)
                                            .padding(end = 10.dp)
                                            .offset(0.dp, 2.dp)
                                            .background(Color.White)
                                    )
                                    Text(
                                        modifier = Modifier
                                            .padding(end = 10.dp),
                                        text = app.label?.first().toString(),
                                        color = textColor,
                                        fontSize = 20.sp
                                    )
                                }
                            }

                            if (showOptions.value) {
                                AlertDialog(
                                    icon = {  },
                                    title = { Text(text = "ACTION") },
                                    text = { Text(text = "What to do with ${app.label}?") },
                                    onDismissRequest = {
                                        showOptions.value = false
                                    },
                                    confirmButton = {
                                        Button(onClick = {
                                            uninstallApp(app.packageName)
                                            showOptions.value = false
                                        }) {
                                            Text("uninstall")
                                        }
                                    },
                                    dismissButton = {
                                        Button(onClick = {
                                            showOptions.value = false
                                            hideApp(app.packageName)
                                        }) {
                                            Text(if (app.hidden != null && app.hidden!!) "show" else "hide")
                                        }
                                    }
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .padding(bottom = 20.dp)
                                    .combinedClickable(
                                        onClick = {
                                            launchApp(app.packageName)
                                        },
                                        onLongClick = {
                                            showOptions.value = true
                                        },
                                    )
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