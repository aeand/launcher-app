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
    "regular" to R.font.roboto_regular,
    "italic" to R.font.roboto_italic,
    "thin" to R.font.roboto_thin,
    "thin-italic" to R.font.roboto_thin_italic,
    "light" to R.font.roboto_light,
    "light-italic" to R.font.roboto_light_italic,
    "medium" to R.font.roboto_medium,
    "medium-italic" to R.font.roboto_medium_italic,
    "bold" to R.font.roboto_bold,
    "bold-italic" to R.font.roboto_bold_italic,
    "black" to R.font.roboto_black,
    "black-italic" to R.font.roboto_black_italic,
    "light" to R.font.roboto_condensed_light,
    "light-italic" to R.font.roboto_condensed_light_italic,
    "regular" to R.font.roboto_condensed_regular,
    "italic" to R.font.roboto_condensed_italic,
    "bold" to R.font.roboto_condensed_bold,
    "bold-italic" to R.font.roboto_condensed_bold_italic,
)

val Typography = Typography(
    titleLarge = TextStyle(
        fontFamily = FontFamily(
            Font(roboto["regular"]!!),
        ),
        fontWeight = FontWeight.Normal,
        fontSize = 40.sp,
        lineHeight = 40.sp,
        letterSpacing = TextUnit(-0.02f, TextUnitType.Em),
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily(
            Font(roboto["regular"]!!),
        ),
        fontWeight = FontWeight.Normal,
        fontSize = 30.sp,
        lineHeight = 30.sp,
        letterSpacing = TextUnit(-0.02f, TextUnitType.Em),
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily(
            Font(roboto["regular"]!!),
        ),
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 24.sp,
        letterSpacing = TextUnit(-0.02f, TextUnitType.Em),
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily(
            Font(roboto["regular"]!!)
        ),
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 19.8.sp,
        letterSpacing = TextUnit(-0.02f, TextUnitType.Em),
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily(
            Font(roboto["regular"]!!)
        ),
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 17.6.sp,
        letterSpacing = TextUnit(-0.02f, TextUnitType.Em),
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily(
            Font(roboto["regular"]!!)
        ),
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 15.4.sp,
        letterSpacing = TextUnit(-0.02f, TextUnitType.Em),
    ),
)