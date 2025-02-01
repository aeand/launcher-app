package com.example.my_launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_launcher.app_drawer.AppDrawer
import com.example.my_launcher.app_drawer.AppDrawerUI
import kotlinx.coroutines.CoroutineScope
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/*
1. make alpha list letter follow when dragging
2. Performance. fix pager lag when going back to empty screen
3. Add duolingo widget support
4. make home button default to top of app list and open the wallpaper view
5. blur background when list is open
6. set text color dynamically depending on background color
7. add refresh app list. donno when tho
8. update date when date changes
9. add settings. I wanna hide specific apps
*/

/*
bug: app crashed when showing hidden apps. because i hide apps in ö
bug: alphabet doesn't udate when hiding apps
make scroll snappier
make scroll faster by reducing the height of appdrawer
bug: can init drag down when at bottom. Which will active the dim effect
also fix dimmer not always enabling. Might need more things
update date manually. I don't always get the event update
fix delay on startup by doing intents on non-blocking main thread
bug: when trying to show an app in show all apps list. it says hide. still works though
showing apps reveals discord in C category
*/

object AppColors {
    val primary = Color.Red
    val secondary = Color.Green
    val tertiary = Color.Blue
    val white = Color.White
    val black = Color.Black
    val transparent = Color.Transparent
    val lol = Color.Yellow
}

class MainActivity : ComponentActivity() {
    private val customScope = CoroutineScope(AndroidUiDispatcher.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appDrawer = AppDrawer(this, packageManager, customScope)
        val textColor = AppColors.white
        val date = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date())

        setContent {
            val isDarkMode = isSystemInDarkTheme()
            val context = LocalContext.current as ComponentActivity
            DisposableEffect(isDarkMode) {
                context.enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.dark(AppColors.transparent.toArgb()),
                    navigationBarStyle = SystemBarStyle.dark(AppColors.transparent.toArgb()),
                )

                onDispose { }
            }

            val lazyScroll = rememberLazyListState()

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
                    Box(modifier = Modifier.fillMaxSize())
                } else if (it == 1) {
                    AppDrawerUI(
                        Modifier,
                        appDrawer,
                        lazyScroll,
                        customScope,
                        textColor,
                    )
                }
            }
        }
    }


}