package com.example.my_launcher.app_drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.my_launcher.Typography

@Composable
fun AppDrawerDialog(
    toggleVisibility: () -> Unit,
    uninstall: () -> Unit,
    cancel: () -> Unit,
    selectedApp: MutableState<AppDrawer.ApplicationInformation?>,
) {
    Box(
        modifier = Modifier
            .zIndex(1f)
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                cancel()
            }
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .width(300.dp)
                .height(200.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { }
                .background(Color.DarkGray),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(30.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = selectedApp.value?.label!!,
                    color = Color.White,
                    fontFamily = Typography.titleMedium.fontFamily,
                    fontSize = Typography.titleMedium.fontSize,
                    fontWeight = Typography.titleMedium.fontWeight,
                    lineHeight = Typography.titleMedium.lineHeight,
                )

                Row(
                    modifier = Modifier
                        .fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Box(
                        modifier = Modifier
                            .width(90.dp)
                            .height(50.dp)
                            .border(3.dp, Color.LightGray, RoundedCornerShape(10.dp))
                            .clickable {
                                toggleVisibility()
                            },
                    ) {
                        Text(
                            modifier = Modifier
                                .align(Alignment.Center),
                            text = if (selectedApp.value!!.hidden == true) "show" else "hide",
                            color = Color.White,
                            fontFamily = Typography.bodyMedium.fontFamily,
                            fontSize = Typography.bodyMedium.fontSize,
                            fontWeight = Typography.bodyMedium.fontWeight,
                            lineHeight = Typography.bodyMedium.lineHeight,
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(90.dp)
                            .height(50.dp)
                            .border(3.dp, Color.LightGray, RoundedCornerShape(10.dp))
                            .clickable {
                                uninstall()
                            },
                    ) {
                        Text(
                            modifier = Modifier
                                .align(Alignment.Center),
                            text = "uninstall",
                            color = Color.White,
                            fontFamily = Typography.bodyMedium.fontFamily,
                            fontSize = Typography.bodyMedium.fontSize,
                            fontWeight = Typography.bodyMedium.fontWeight,
                            lineHeight = Typography.bodyMedium.lineHeight,
                        )
                    }
                }
            }
        }
    }
}