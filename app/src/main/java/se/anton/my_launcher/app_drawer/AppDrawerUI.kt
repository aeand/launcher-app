package se.anton.my_launcher.app_drawer

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import se.anton.my_launcher.R
import se.anton.my_launcher.Typography
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppDrawerUI(
    appDrawer: AppDrawer,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.4f)
                .padding(start = 15.dp, end = 15.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(40.dp)
                    .clickable {
                        appDrawer.showAllApps.value = !appDrawer.showAllApps.value
                        appDrawer.createAppList()
                    }
            ) {
                Icon(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(if (appDrawer.showAllApps.value) 30.dp else 20.dp)
                        .offset {
                            IntOffset(
                                x = if (appDrawer.showAllApps.value) (13) else (0),
                                y = if (appDrawer.showAllApps.value) (0) else (-10)
                            )
                        },
                    painter = painterResource(id = if (appDrawer.showAllApps.value) R.drawable.eye_cross else R.drawable.eye),
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }

        val selectedApp = remember { mutableStateOf<ApplicationInformation?>(null) }
        val lazyScroll = rememberLazyListState()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.6f)
                .align(Alignment.BottomEnd),
            horizontalArrangement = Arrangement.End
        ) {
            LazyColumn(
                state = lazyScroll,
                horizontalAlignment = Alignment.End
            ) {
                appDrawer.apps.forEach { app ->
                    item {
                        if (appDrawer.showAllApps.value || !app.hidden!!) {
                            val firstAppWithLetter = appDrawer.apps.find {
                                it.label?.uppercase()?.startsWith(app.label?.uppercase()!![0])!!
                                        && (it.hidden == false || appDrawer.showAllApps.value)
                            }!!

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
                                            .background(Color.White)
                                    )
                                    Text(
                                        modifier = Modifier
                                            .padding(end = 10.dp),
                                        text = app.label?.first()?.uppercaseChar().toString(),
                                        fontFamily = Typography.bodyMedium.fontFamily,
                                        fontSize = Typography.bodyMedium.fontSize,
                                        fontWeight = Typography.bodyMedium.fontWeight,
                                        lineHeight = Typography.bodyMedium.lineHeight,
                                        color = Color.White,
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .padding(start = 15.dp, bottom = 20.dp, end = 15.dp)
                                    .fillMaxHeight()
                                    .combinedClickable(
                                        onClick = {
                                            appDrawer.launchApp(app.packageName)
                                        },
                                        onLongClick = {
                                            if (selectedApp.value == app) {
                                                selectedApp.value = null
                                            } else {
                                                selectedApp.value = app
                                            }
                                        },
                                    ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (selectedApp.value == app) {
                                    Icon(
                                        modifier = Modifier
                                            .padding(end = 15.dp)
                                            .fillMaxHeight()
                                            .size(if (app.hidden == true) 40.dp else 30.dp)
                                            .clickable {
                                                if (selectedApp.value != null)
                                                    appDrawer.hideApp(app.packageName)

                                                selectedApp.value = null
                                            },
                                        painter = painterResource(id = if (app.hidden == true) R.drawable.eye_cross else R.drawable.eye),
                                        contentDescription = null,
                                        tint = Color.White
                                    )

                                    Icon(
                                        modifier = Modifier
                                            .padding(end = 15.dp)
                                            .fillMaxHeight()
                                            .size(30.dp)
                                            .clickable {
                                                if (selectedApp.value != null) {
                                                    appDrawer.openAppSettings(selectedApp.value!!.packageName)
                                                }

                                                selectedApp.value = null
                                            },
                                        painter = painterResource(id = R.drawable.settings),
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                } else {
                                    Text(
                                        modifier = Modifier
                                            .align(Alignment.CenterVertically)
                                            .padding(end = 10.dp)
                                            .width(300.dp),
                                        text = "${app.label}",
                                        color = Color.White,
                                        fontFamily = Typography.titleMedium.fontFamily,
                                        fontSize = Typography.titleMedium.fontSize,
                                        fontWeight = Typography.titleMedium.fontWeight,
                                        lineHeight = Typography.titleMedium.lineHeight,
                                        overflow = TextOverflow.Ellipsis,
                                        maxLines = 1,
                                        textAlign = TextAlign.End
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .drawBehind {
                                            drawIntoCanvas { canvas ->
                                                app.icon?.let { icon ->
                                                    icon.setBounds(0, 0, size.width.roundToInt(), size.height.roundToInt())
                                                    icon.draw(canvas.nativeCanvas)
                                                }
                                            }
                                        }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}