package se.anton.my_launcher

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
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
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import se.anton.my_launcher.app_drawer.AppDrawer
import se.anton.my_launcher.app_drawer.AppDrawerUI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/* TODO
Bugs:
- takes long to visually react on unlock
- hiding app gator resulted in gallery getting its own G category with my other app gallery starting a new G category
- installing the same app under a different package name but same app name. This creates two letter category. eg "...Maps ---N Notes ---N Notes ---P Phone Play Store..."

Fix:
- disable scroll for section with overlapping scroll
- callback on opening app settings? could refresh app list after going back
- add drag down to open notifications again (try with fade)

Performance:
- check out recompositions. reduce them as much as possible
- improve the pager to swipe faster
- load apps on separate thread to make it faster and not freeze
- disable battery saving for app?

Look into:
- refresh app list after install and uninstall
- gray app select background
- fade when dragging down notifications
- home button going to home app

other launchers to look into
- octopi
- compose
- indistractable
- ratio productivity homescreen
- p
- xos
- lynx
- square home app
- before launcher go minimal
- mini desktop
- aio
- mur
- hyperion
- ace smart
- zen detox minimalist
- pro launcher productive you
*/

class MainActivity : ComponentActivity() {
    private val customScope = CoroutineScope(AndroidUiDispatcher.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appDrawer = AppDrawer(this, packageManager, applicationContext)
        var date = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date())

        @SuppressLint("SourceLockedOrientationActivity")
        this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

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
                    .padding(start = 19.dp),
                text = date,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight(600)
            )

            VerticalPager(state = pagerState) {
                if (it == 0) {
                    Box(modifier = Modifier.fillMaxSize())
                } else if (it == 1) {
                    AppDrawerUI(appDrawer)
                }
            }
        }
    }
}