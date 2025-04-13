package com.example.my_launcher

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.sp

val Typography = Typography(
    titleLarge = TextStyle(
        fontFamily = FontFamily(Font(R.font.roboto_light)),
        fontWeight = FontWeight.Normal,
        fontSize = 40.sp,
        lineHeight = 45.sp,
        letterSpacing = TextUnit(0f, TextUnitType.Em),
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily(Font(R.font.roboto_light)),
        fontWeight = FontWeight.Normal,
        fontSize = 30.sp,
        lineHeight = 35.sp,
        letterSpacing = TextUnit(0f, TextUnitType.Em),
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily(Font(R.font.roboto_light)),
        fontWeight = FontWeight.Normal,
        fontSize = 25.sp,
        lineHeight = 30.sp,
        letterSpacing = TextUnit(0f, TextUnitType.Em),
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily(Font(R.font.roboto_light)),
        fontWeight = FontWeight.Normal,
        fontSize = 23.sp,
        lineHeight = 28.sp,
        letterSpacing = TextUnit(0f, TextUnitType.Em),
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily(Font(R.font.roboto_light)),
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
        lineHeight = 25.sp,
        letterSpacing = TextUnit(0f, TextUnitType.Em),
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily(Font(R.font.roboto_light)),
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 21.sp,
        letterSpacing = TextUnit(0f, TextUnitType.Em),
    ),
)