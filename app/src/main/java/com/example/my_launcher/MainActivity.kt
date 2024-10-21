package com.example.my_launcher

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
import androidx.compose.runtime.DisposableEffect
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/*
0. change API version to minimum so more people can use it
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

class MainActivity : ComponentActivity() {
    private val customScope = CoroutineScope(AndroidUiDispatcher.Main)

    class ApplicationInformation {
        var label: String? = null
        var packageName: String? = null
        var icon: Drawable? = null
    }

    @OptIn(ExperimentalFoundationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val textColor = Color.White
        val date = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date())
        val apps = createAppList()
        val alphabet = createAlphabetList(apps)
        
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
                }
                else if (it == 1) {
                    Box(
                        modifier = Modifier
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
            }
        }
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
            a.label?.compareTo(b.label!!)!!
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
}
