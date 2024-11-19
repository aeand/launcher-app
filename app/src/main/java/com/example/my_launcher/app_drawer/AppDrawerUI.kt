package com.example.my_launcher.app_drawer

import android.annotation.SuppressLint
import android.appwidget.AppWidgetHostView
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.viewinterop.AndroidView
import com.example.my_launcher.R
import com.example.my_launcher.Typography
import com.example.my_launcher.rememberDrawablePainter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@SuppressLint("UseOfNonLambdaOffsetOverload")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppDrawer(
    modifier: Modifier,
    lazyScroll: LazyListState,
    hostView: AppWidgetHostView?,
    customScope: CoroutineScope,
    textColor: Color,
    apps: List<AppDrawer.ApplicationInformation>,
    launchApp: (String?) -> Unit,
    hideApp: (String?) -> Unit,
    uninstallApp: (String?) -> Unit,
    alphabet: List<String>,
    bottomBar: Float,
) {
    val showAllApps = remember { mutableStateOf(false) }
    val showDialog = remember { mutableStateOf(false) }
    val selectedApp = remember { mutableStateOf<AppDrawer.ApplicationInformation?>(null) }

    if (showDialog.value) {
        AppDrawerDialog(
            hide = {
                if (selectedApp.value != null)
                    hideApp(selectedApp.value!!.packageName)

                selectedApp.value = null
                showDialog.value = false
            },
            uninstall = {
                if (selectedApp.value != null)
                    uninstallApp(selectedApp.value!!.packageName)

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
                        x = if (showAllApps.value) (-35).dp else (-40).dp,
                        y = if (showAllApps.value) (-1).dp else (-5).dp
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
                    .offset(0.dp, (-50).dp)
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
                .padding(bottom = bottomBar.dp),
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
                                        text = app.label?.first()?.uppercaseChar().toString(),
                                        fontFamily = Typography.bodyMedium.fontFamily,
                                        fontSize = Typography.bodyMedium.fontSize,
                                        fontWeight = Typography.bodyMedium.fontWeight,
                                        lineHeight = Typography.bodyMedium.lineHeight,
                                        color = textColor,
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .padding(bottom = 20.dp)
                                    .combinedClickable(
                                        onClick = {
                                            launchApp(app.packageName)
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
                                    color = textColor,
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
                alphabet.forEach { letter ->
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
                                            customScope.launch {
                                                var i = 0
                                                var found = false

                                                apps.forEachIndexed { index, app ->
                                                    if (
                                                        !found
                                                        && app.label != null
                                                        && app.label!![0].uppercaseChar() == letter.toCharArray()[0].uppercaseChar()
                                                    ) {
                                                        i = index
                                                        found = true
                                                    }
                                                }

                                                lazyScroll.animateScrollToItem(i)
                                            }

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
                        color = textColor,
                        fontSize = if (selectedLetter == letter) 40.sp else 16.sp,
                        fontWeight = FontWeight(600)
                    )
                }
            }
        }
    }
}