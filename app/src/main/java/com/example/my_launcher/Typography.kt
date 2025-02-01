package com.example.my_launcher

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.sp

val roboto = mapOf(
    "regular" to Font(R.font.roboto_regular),
    "italic" to Font(R.font.roboto_italic),
    "thin" to Font(R.font.roboto_thin),
    "thin-italic" to Font(R.font.roboto_thin_italic),
    "light" to Font(R.font.roboto_light),
    "light-italic" to Font(R.font.roboto_light_italic),
    "medium" to Font(R.font.roboto_medium),
    "medium-italic" to Font(R.font.roboto_medium_italic),
    "bold" to Font(R.font.roboto_bold),
    "bold-italic" to Font(R.font.roboto_bold_italic),
    "black" to Font(R.font.roboto_black),
    "black-italic" to Font(R.font.roboto_black_italic),
    "light" to Font(R.font.roboto_condensed_light),
    "light-italic" to Font(R.font.roboto_condensed_light_italic),
    "regular" to Font(R.font.roboto_condensed_regular),
    "italic" to Font(R.font.roboto_condensed_italic),
    "bold" to Font(R.font.roboto_condensed_bold),
    "bold-italic" to Font(R.font.roboto_condensed_bold_italic),
)

val Typography = Typography(
    titleLarge = TextStyle(
        fontFamily = FontFamily(roboto["regular"]!!),
        fontWeight = FontWeight.Normal,
        fontSize = 40.sp,
        lineHeight = 45.sp,
        letterSpacing = TextUnit(0f, TextUnitType.Em),
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily(roboto["regular"]!!),
        fontWeight = FontWeight.Normal,
        fontSize = 30.sp,
        lineHeight = 35.sp,
        letterSpacing = TextUnit(0f, TextUnitType.Em),
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily(roboto["regular"]!!),
        fontWeight = FontWeight.Normal,
        fontSize = 25.sp,
        lineHeight = 30.sp,
        letterSpacing = TextUnit(0f, TextUnitType.Em),
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily(roboto["regular"]!!),
        fontWeight = FontWeight.Normal,
        fontSize = 23.sp,
        lineHeight = 28.sp,
        letterSpacing = TextUnit(0f, TextUnitType.Em),
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily(roboto["regular"]!!),
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
        lineHeight = 25.sp,
        letterSpacing = TextUnit(0f, TextUnitType.Em),
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily(roboto["regular"]!!),
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 21.sp,
        letterSpacing = TextUnit(0f, TextUnitType.Em),
    ),
)