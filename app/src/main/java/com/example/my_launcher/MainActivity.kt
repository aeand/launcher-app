package com.example.my_launcher

import android.Manifest
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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
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
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/* TODO
1. update duolingo widget
2. change API version to minimum so more people can use it
3. Performance. fix pager lag when going back to empty screen
4. update app list. don't know when tho
*/

/* Features
1. blur background when list is open
2. make home button default to top of app list and open the wallpaper view
3. make alpha list letter follow when dragging
4. add settings. I wanna hide specific apps
5. set text color dynamically depending on background color
6. add to app list: a letter at the top of the section for that letter. And a line. to improve readability
7. add a notes feature on swipe right or something
8. make it swipeable to open the status bar by using permission EXPAND_STATUS_BAR
9. could add that I can delete packages from list with REQUEST_DELETE_PACKAGES
10. could do something with permission VIBRATE
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
    private var appWidgetHost: AppWidgetHost? = null
    private var appWidgetManager: AppWidgetManager? = null

    /*private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        //check if I can bind the widget
        println("uri: $uri")
    }*/

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
        val apps = createAppList()
        val alphabet = createAlphabetList(apps)
        val value = getDuolingoWidgetView()
        val duolingoWidgetView: MutableState<AppWidgetHostView>?
        if (value != null) {
             duolingoWidgetView = mutableStateOf(value)
        }
        else {
            duolingoWidgetView = null
        }

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
            
            LaunchedEffect(key1 = true) {
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

            VerticalPager(
                modifier = Modifier
                    .padding(top = 32.dp, bottom = 48.dp),
                state = rememberPagerState(pageCount = {
                    2
                })
            ) {
                if (it == 0) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .width(300.dp)
                                .height(100.dp)
                        ) {
                            if (duolingoWidgetView != null)
                                AndroidView(factory = { duolingoWidgetView.value })
                        }
                    }
                }
                else if (it == 1) {
                    AppDrawer(
                        alphabet = alphabet,
                        apps = apps,
                        customScope = customScope,
                        launchApp = ::launchApp,
                        scrollToFirstItem = ::scrollToFirstItem,
                        textColor = textColor,
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        appWidgetHost!!.stopListening()
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

    private fun getDuolingoWidgetView(): AppWidgetHostView? {
        appWidgetHost = AppWidgetHost(applicationContext, 123123123)
        appWidgetManager = AppWidgetManager.getInstance(applicationContext)
        appWidgetHost!!.startListening()

        appWidgetManager!!.installedProviders.forEach {
            println(it.activityInfo.name)
        }

        val duolingoWidget: AppWidgetProviderInfo? = appWidgetManager!!.installedProviders.find { it.activityInfo.name.contains("com.duolingo.streak.streakWidget.MediumStreakWidgetProvider") }
        val appWidgetId = appWidgetHost!!.allocateAppWidgetId()

        if (!appWidgetManager!!.bindAppWidgetIdIfAllowed(appWidgetId, duolingoWidget!!.provider)) { //info.provider
            println("requesting permissions")
            /*val bindIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
            bindIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            bindIntent.putExtra(
                AppWidgetManager.EXTRA_APPWIDGET_PROVIDER,
                LauncherExperiment.widgetList.get(mParam1).provider
            )
            startActivityForResult(bindIntent, com.example.my_launcher.Manifest.permission.)*/

            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, duolingoWidget.provider) //info.provider
            }
            startActivity(intent)

            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.BIND_APPWIDGET,
                ),
                0
            )

            /*val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.componentName)
                // This is the options bundle described in the preceding section.
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_OPTIONS, options)
            }
            startActivityForResult(intent, REQUEST_BIND_APPWIDGET)*/
        }
        else {
            return appWidgetHost!!.createView(applicationContext, appWidgetId, duolingoWidget).apply {
                setAppWidget(appWidgetId, appWidgetInfo)
            }
        }

        return null

        /*val newOptions = Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, width)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, width)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, height)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, height)
        }
        appWidgetManager.updateAppWidgetOptions(appWidgetId, newOptions)*/

        //appWidgetHost.deleteAppWidgetId(appWidgetId)
    }
}

@Composable
fun AppDrawer(
    customScope: CoroutineScope,
    textColor: Color,
    apps: MutableList<MainActivity.ApplicationInformation>,
    launchApp: (String?) -> Unit,
    alphabet: MutableList<String>,
    scrollToFirstItem: (MutableList<MainActivity.ApplicationInformation>, String, LazyListState) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 10.dp, end = 10.dp)
    ) {
        val lazyScroll = rememberLazyListState()

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.4f)
        ) {
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