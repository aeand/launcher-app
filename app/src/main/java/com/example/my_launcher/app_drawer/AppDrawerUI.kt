package com.example.my_launcher.app_drawer

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_launcher.AppColors
import com.example.my_launcher.R
import com.example.my_launcher.Typography
import com.example.my_launcher.rememberDrawablePainter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppDrawerUI(
    appDrawer: AppDrawer,
    customScope: CoroutineScope,
) {
    val lazyScroll = rememberLazyListState()

    val showDialog = remember { mutableStateOf(false) }
    val selectedApp = remember { mutableStateOf<AppDrawer.ApplicationInformation?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 10.dp, end = 10.dp)
    ) {
        if (showDialog.value) {
            AppDrawerDialog(
                toggleVisibility = {
                    if (selectedApp.value != null)
                        appDrawer.hideApp(selectedApp.value!!.packageName)

                    selectedApp.value = null
                    showDialog.value = false
                },
                uninstall = {
                    if (selectedApp.value != null) {
                        appDrawer.uninstallApp(selectedApp.value!!.packageName)
                    }

                    selectedApp.value = null
                    showDialog.value = false
                },
                cancel = {
                    selectedApp.value = null
                    showDialog.value = false
                },
                selectedApp = selectedApp
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.4f)
        ) {
            Icon(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(if (appDrawer.showAllApps.value) 30.dp else 20.dp)
                    .offset {
                        IntOffset(
                            x = if (appDrawer.showAllApps.value) (-88) else (-100),
                            y = if (appDrawer.showAllApps.value) (0) else (-10)
                        )
                    }
                    .clickable {
                        appDrawer.showAllApps.value = !appDrawer.showAllApps.value
                        appDrawer.createAppList()
                    },
                painter = painterResource(id = if (appDrawer.showAllApps.value) R.drawable.eye_cross else R.drawable.eye),
                contentDescription = null,
                tint = AppColors.textColor
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
                tint = AppColors.textColor
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
                appDrawer.apps.forEach { app ->
                    item {
                        if (appDrawer.showAllApps.value || !app.hidden!!) {
                            val firstAppWithLetter = appDrawer.apps.find {
                                it.label?.uppercase()
                                    ?.startsWith(app.label?.uppercase()!![0])!! && (it.hidden == false || appDrawer.showAllApps.value)
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
                                            .offset(0.dp, 2.dp)
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
                                        color = AppColors.textColor,
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .padding(bottom = 20.dp)
                                    .combinedClickable(
                                        onClick = {
                                            appDrawer.launchApp(app.packageName)
                                        },
                                        onLongClick = {
                                            selectedApp.value = app
                                            showDialog.value = true
                                        },
                                    )
                            ) {
                                Text(
                                    modifier = Modifier
                                        .align(Alignment.CenterVertically)
                                        .padding(end = 10.dp)
                                        .width(300.dp),
                                    text = "${app.label}",
                                    color = AppColors.textColor,
                                    fontFamily = Typography.titleMedium.fontFamily,
                                    fontSize = Typography.titleMedium.fontSize,
                                    fontWeight = Typography.titleMedium.fontWeight,
                                    lineHeight = Typography.titleMedium.lineHeight,
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
                appDrawer.alphabet.forEach { letter ->
                    Text(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        try {
                                            selectedLetter = letter
                                            offsetY = -150f
                                            awaitRelease()
                                        } finally {
                                            appDrawer.scrollToFirstItem(selectedLetter, lazyScroll)

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
                        text = letter.uppercase(),
                        color = AppColors.textColor,
                        fontSize = if (selectedLetter == letter) 40.sp else 16.sp,
                        fontWeight = FontWeight(600)
                    )
                }
            }
        }
    }
}