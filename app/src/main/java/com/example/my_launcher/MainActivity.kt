package com.example.my_launcher

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_launcher.app_drawer.AppDrawer
import com.example.my_launcher.app_drawer.AppDrawerUI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/*
fix delay on startup by doing intents on non-blocking main thread
check out recompositions. reduce them as much as possible
refresh app list after install
fix app select background (currently grey)
*/

// Inspiration: https://github.com/markusfisch/PieLauncher/tree/master

object AppColors {
    var textColor = Color.White
    var background = Color.Transparent
}

class MainActivity : ComponentActivity() {
    private lateinit var receiver: BroadcastReceiver
    private val customScope = CoroutineScope(AndroidUiDispatcher.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appDrawer = AppDrawer(this, packageManager, customScope)
        var date = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date())

        @SuppressLint("SourceLockedOrientationActivity")
        this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        registerReceiver(appDrawer)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AppColors.background.toArgb()),
            navigationBarStyle = SystemBarStyle.dark(AppColors.background.toArgb()),
        )

        setContent {
            val pagerState = rememberPagerState(pageCount = { 2 })

            // DATE
            LaunchedEffect(true) {
                val calendar = Calendar.getInstance()
                val today = calendar.time
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val tomorrow = calendar.time

                var seconds = TimeUnit.MILLISECONDS.toSeconds(tomorrow.time - today.time)
                while (true) {
                    seconds += 1
                    if (seconds > 3600) {
                        date = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date())
                        seconds = 1
                    }
                    delay(1000)
                }
            }

            // DIMMER
            val floatAnim = animateFloatAsState(
                targetValue = if (pagerState.currentPage > 0) 0.5f else 0f,
                tween(
                    durationMillis = 1000,
                    easing = LinearEasing,
                ),
                label = "",
            )
            window.setDimAmount(floatAnim.value)

            // UPDATE APP LIST
            LaunchedEffect(pagerState.currentPage > 0) {
                val i = Intent(Intent.ACTION_MAIN, null)
                i.addCategory(Intent.CATEGORY_LAUNCHER)

                val pk: List<ResolveInfo> =
                    packageManager.queryIntentActivities(i, PackageManager.GET_META_DATA)

                if (appDrawer.packages.size != pk.size || appDrawer.packages.toSet() != pk.toSet()) {
                    appDrawer.createAppList()
                }
            }

            // BACK BUTTON PRESS
            BackHandler {
                customScope.launch {
                    pagerState.scrollToPage(0)
                }
            }

            Text(
                modifier = Modifier
                    .padding(start = 19.dp, top = 30.dp),
                text = date,
                color = AppColors.textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight(600)
            )

            VerticalPager(
                modifier = Modifier
                    .padding(top = 32.dp, bottom = 48.dp),
                state = pagerState,
            ) {
                if (it == 0) {
                    Box(modifier = Modifier.fillMaxSize())
                } else if (it == 1) {
                    AppDrawerUI(
                        appDrawer,
                        customScope,
                    )
                }
            }
        }
    }

    private fun registerReceiver(appDrawer: AppDrawer) {
        val filters = IntentFilter()
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val list = listOf(
                    Intent.ACTION_APPLICATION_LOCALE_CHANGED,
                    Intent.ACTION_MY_PACKAGE_REPLACED,
                    Intent.ACTION_PACKAGE_ADDED,
                    Intent.ACTION_PACKAGE_FULLY_REMOVED,
                    Intent.ACTION_PACKAGE_REMOVED,
                    Intent.ACTION_PACKAGE_REPLACED,
                    Intent.ACTION_UID_REMOVED,
                    Intent.ACTION_APPLICATION_PREFERENCES,
                    Intent.ACTION_PACKAGE_CHANGED,
                    Intent.ACTION_PACKAGE_INSTALL,
                    Intent.ACTION_MEDIA_BUTTON,
                    Intent.ACTION_CLOSE_SYSTEM_DIALOGS,
                    Intent.ACTION_DATE_CHANGED,
                    Intent.ACTION_TIMEZONE_CHANGED,
                    Intent.ACTION_TIME_CHANGED,
                )

                list.forEach {
                    when (intent.action) {
                        it -> {
                            println(it)
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
                        appDrawer.createAppList()
                    }

                    Intent.ACTION_APPLICATION_PREFERENCES, // intent for adjusting app preferences. recommended for all apps with settings
                    Intent.ACTION_PACKAGE_CHANGED, // broadcast a package has been changed
                    Intent.ACTION_PACKAGE_INSTALL, // deprecated broadcast trigger download and install of package
                        -> {
                    }

                    Intent.ACTION_MEDIA_BUTTON -> {
                        println(Intent.ACTION_MEDIA_BUTTON)
                        println(intent.extras)
                    }

                    Intent.ACTION_CLOSE_SYSTEM_DIALOGS -> {
                        appDrawer.createAppList()
                        //TODO -> scroll to page 0
                    }

                    Intent.ACTION_DATE_CHANGED,
                    Intent.ACTION_TIMEZONE_CHANGED,
                    Intent.ACTION_TIME_CHANGED -> {
                        //TODO -> set date
                        //date = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date())
                    }
                }
            }
        }

        registerReceiver(receiver, filters, RECEIVER_NOT_EXPORTED)
    }
}