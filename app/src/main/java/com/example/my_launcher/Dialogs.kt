package com.example.my_launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

@Composable
fun DialogSaveFolder(
    confirm: (name: String, path: String) -> Unit,
    cancel: () -> Unit,
) {
    val outsideInteractionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .zIndex(1f)
            .fillMaxSize()
            .clickable(
                interactionSource = outsideInteractionSource,
                indication = null,
            ) {
                cancel()
            },
    ) {
        val interactionSource = remember { MutableInteractionSource() }

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .width(300.dp)
                .height(400.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable(interactionSource = interactionSource, indication = null) {  }
                .background(Color.DarkGray),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(30.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally),
                    text = "Create folder",
                    fontFamily = Typography.bodyMedium.fontFamily,
                    fontSize = Typography.bodyMedium.fontSize,
                    fontWeight = Typography.bodyMedium.fontWeight,
                    lineHeight = Typography.bodyMedium.lineHeight,
                    color = Color.White,
                )

                val focusManager = LocalFocusManager.current
                val focusRequester = remember { FocusRequester() }

                val pathName = remember { mutableStateOf("") }
                val pathTextFieldFocused = remember { mutableStateOf(false) }

                val customTextSelectionColors = TextSelectionColors(
                    handleColor = Color.Gray,
                    backgroundColor = Color.DarkGray
                )

                CompositionLocalProvider(
                    LocalTextSelectionColors provides customTextSelectionColors
                ) {
                    BasicTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .focusRequester(focusRequester)
                            .onFocusChanged {
                                if (it.isFocused) {
                                    pathTextFieldFocused.value = true
                                }
                            }
                            .background(Color.White),
                        value = pathName.value,
                        onValueChange = {
                            pathName.value = it
                        },
                        cursorBrush = Brush.verticalGradient(
                            0.00f to Color.Black,
                            0.15f to Color.Black,
                            0.15f to Color.Black,
                            0.75f to Color.Black,
                            0.75f to Color.Black,
                            1.00f to Color.Black,
                        ),
                        textStyle = TextStyle(
                            textAlign = TextAlign.Start,
                            color = Color.Black,
                            fontFamily = Typography.titleMedium.fontFamily,
                            fontSize = Typography.titleMedium.fontSize,
                            lineHeight = Typography.titleMedium.lineHeight,
                            letterSpacing = Typography.titleMedium.letterSpacing,
                        ),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            autoCorrectEnabled = false,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                pathTextFieldFocused.value = false
                            }
                        ),
                        singleLine = true,
                        maxLines = 1,
                        visualTransformation = VisualTransformation.None,
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                            ) {
                                if (pathName.value.isEmpty()) {
                                    Text(
                                        modifier = Modifier
                                            .align(Alignment.CenterStart)
                                            .padding(5.dp),
                                        text = "Name the path",
                                        textAlign = TextAlign.Left,
                                        fontFamily = FontFamily(
                                            Font(R.font.roboto_italic)
                                        ),
                                        fontSize = Typography.titleMedium.fontSize,
                                        fontWeight = Typography.titleMedium.fontWeight,
                                        lineHeight = Typography.titleMedium.lineHeight,
                                        color = Color.Gray
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.CenterStart)
                                            .padding(5.dp)
                                    ) {
                                        innerTextField()
                                    }
                                }
                            }
                        },
                    )
                }

                val fileName = remember { mutableStateOf("") }
                val textFieldFocused = remember { mutableStateOf(false) }

                CompositionLocalProvider(
                    LocalTextSelectionColors provides customTextSelectionColors
                ) {
                    BasicTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .focusRequester(focusRequester)
                            .onFocusChanged {
                                if (it.isFocused) {
                                    textFieldFocused.value = true
                                }
                            }
                            .background(Color.White),
                        value = fileName.value,
                        onValueChange = {
                            fileName.value = it
                        },
                        cursorBrush = Brush.verticalGradient(
                            0.00f to Color.Black,
                            0.15f to Color.Black,
                            0.15f to Color.Black,
                            0.75f to Color.Black,
                            0.75f to Color.Black,
                            1.00f to Color.Black,
                        ),
                        textStyle = TextStyle(
                            textAlign = TextAlign.Start,
                            color = Color.Black,
                            fontFamily = Typography.titleMedium.fontFamily,
                            fontSize = Typography.titleMedium.fontSize,
                            lineHeight = Typography.titleMedium.lineHeight,
                            letterSpacing = Typography.titleMedium.letterSpacing,
                        ),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            autoCorrectEnabled = false,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                textFieldFocused.value = false
                            }
                        ),
                        singleLine = true,
                        maxLines = 1,
                        visualTransformation = VisualTransformation.None,
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                            ) {
                                if (fileName.value.isEmpty()) {
                                    Text(
                                        modifier = Modifier
                                            .align(Alignment.CenterStart)
                                            .padding(5.dp),
                                        text = "Name the folder",
                                        textAlign = TextAlign.Left,
                                        fontFamily = FontFamily(
                                            Font(R.font.roboto_italic)
                                        ),
                                        fontSize = Typography.titleMedium.fontSize,
                                        fontWeight = Typography.titleMedium.fontWeight,
                                        lineHeight = Typography.titleMedium.lineHeight,
                                        color = Color.Gray
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.CenterStart)
                                            .padding(5.dp)
                                    ) {
                                        innerTextField()
                                    }
                                }
                            }
                        },
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        modifier = Modifier
                            .clickable {
                                cancel()
                            },
                        text = "Cancel",
                        fontFamily = Typography.bodyMedium.fontFamily,
                        fontSize = Typography.bodyMedium.fontSize,
                        fontWeight = Typography.bodyMedium.fontWeight,
                        lineHeight = Typography.bodyMedium.lineHeight,
                        color = Color.White,
                    )

                    Text(
                        modifier = Modifier
                            .clickable {
                                confirm(fileName.value, pathName.value)
                            },
                        text = "Save",
                        fontFamily = Typography.bodyMedium.fontFamily,
                        fontSize = Typography.bodyMedium.fontSize,
                        fontWeight = Typography.bodyMedium.fontWeight,
                        lineHeight = Typography.bodyMedium.lineHeight,
                        color = if (fileName.value.isNotEmpty()) Color.White else Color.Gray,
                    )
                }
            }
        }
    }
}

@Composable
fun DialogSaveFile(
    confirm: (name: String, folder: String) -> Unit,
    cancel: () -> Unit,
    presetFileName: String,
    presetPath: String,
) {
    val outsideInteractionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .zIndex(1f)
            .fillMaxSize()
            .clickable(
                interactionSource = outsideInteractionSource,
                indication = null,
            ) {
                cancel()
            },
    ) {
        val interactionSource = remember { MutableInteractionSource() }

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .width(300.dp)
                .height(400.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable(interactionSource = interactionSource, indication = null) {  }
                .background(Color.DarkGray),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(30.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally),
                    text = "Save note",
                    fontFamily = Typography.bodyMedium.fontFamily,
                    fontSize = Typography.bodyMedium.fontSize,
                    fontWeight = Typography.bodyMedium.fontWeight,
                    lineHeight = Typography.bodyMedium.lineHeight,
                    color = Color.White,
                )

                val focusManager = LocalFocusManager.current
                val focusRequester = remember { FocusRequester() }

                val pathName = remember { mutableStateOf(presetPath) }
                val pathTextFieldFocused = remember { mutableStateOf(false) }

                val customTextSelectionColors = TextSelectionColors(
                    handleColor = Color.Gray,
                    backgroundColor = Color.DarkGray
                )

                CompositionLocalProvider(
                    LocalTextSelectionColors provides customTextSelectionColors
                ) {
                    BasicTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .focusRequester(focusRequester)
                            .onFocusChanged {
                                if (it.isFocused) {
                                    pathTextFieldFocused.value = true
                                }
                            }
                            .background(Color.White),
                        value = pathName.value,
                        onValueChange = {
                            pathName.value = it
                        },
                        cursorBrush = Brush.verticalGradient(
                            0.00f to Color.Black,
                            0.15f to Color.Black,
                            0.15f to Color.Black,
                            0.75f to Color.Black,
                            0.75f to Color.Black,
                            1.00f to Color.Black,
                        ),
                        textStyle = TextStyle(
                            textAlign = TextAlign.Start,
                            color = Color.Black,
                            fontFamily = Typography.titleMedium.fontFamily,
                            fontSize = Typography.titleMedium.fontSize,
                            lineHeight = Typography.titleMedium.lineHeight,
                            letterSpacing = Typography.titleMedium.letterSpacing,
                        ),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            autoCorrectEnabled = false,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                pathTextFieldFocused.value = false
                            }
                        ),
                        singleLine = true,
                        maxLines = 1,
                        visualTransformation = VisualTransformation.None,
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                            ) {
                                if (pathName.value.isEmpty()) {
                                    Text(
                                        modifier = Modifier
                                            .align(Alignment.CenterStart)
                                            .padding(5.dp),
                                        text = "Name the path",
                                        textAlign = TextAlign.Left,
                                        fontFamily = FontFamily(
                                            Font(R.font.roboto_italic)
                                        ),
                                        fontSize = Typography.titleMedium.fontSize,
                                        fontWeight = Typography.titleMedium.fontWeight,
                                        lineHeight = Typography.titleMedium.lineHeight,
                                        color = Color.Gray
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.CenterStart)
                                            .padding(5.dp)
                                    ) {
                                        innerTextField()
                                    }
                                }
                            }
                        },
                    )
                }

                val fileName = remember { mutableStateOf(presetFileName) }
                val textFieldFocused = remember { mutableStateOf(false) }

                CompositionLocalProvider(
                    LocalTextSelectionColors provides customTextSelectionColors
                ) {
                    BasicTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .focusRequester(focusRequester)
                            .onFocusChanged {
                                if (it.isFocused) {
                                    textFieldFocused.value = true
                                }
                            }
                            .background(Color.White),
                        value = fileName.value,
                        onValueChange = {
                            fileName.value = it
                        },
                        cursorBrush = Brush.verticalGradient(
                            0.00f to Color.Black,
                            0.15f to Color.Black,
                            0.15f to Color.Black,
                            0.75f to Color.Black,
                            0.75f to Color.Black,
                            1.00f to Color.Black,
                        ),
                        textStyle = TextStyle(
                            textAlign = TextAlign.Start,
                            color = Color.Black,
                            fontFamily = Typography.titleMedium.fontFamily,
                            fontSize = Typography.titleMedium.fontSize,
                            lineHeight = Typography.titleMedium.lineHeight,
                            letterSpacing = Typography.titleMedium.letterSpacing,
                        ),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            autoCorrectEnabled = false,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                textFieldFocused.value = false
                            }
                        ),
                        singleLine = true,
                        maxLines = 1,
                        visualTransformation = VisualTransformation.None,
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                            ) {
                                if (fileName.value.isEmpty()) {
                                    Text(
                                        modifier = Modifier
                                            .align(Alignment.CenterStart)
                                            .padding(5.dp),
                                        text = "Name the file",
                                        textAlign = TextAlign.Left,
                                        fontFamily = FontFamily(
                                            Font(R.font.roboto_italic)
                                        ),
                                        fontSize = Typography.titleMedium.fontSize,
                                        fontWeight = Typography.titleMedium.fontWeight,
                                        lineHeight = Typography.titleMedium.lineHeight,
                                        color = Color.Gray
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.CenterStart)
                                            .padding(5.dp)
                                    ) {
                                        innerTextField()
                                    }
                                }
                            }
                        },
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        modifier = Modifier
                            .clickable {
                                cancel()
                            },
                        text = "Cancel",
                        fontFamily = Typography.bodyMedium.fontFamily,
                        fontSize = Typography.bodyMedium.fontSize,
                        fontWeight = Typography.bodyMedium.fontWeight,
                        lineHeight = Typography.bodyMedium.lineHeight,
                        color = Color.White,
                    )

                    Text(
                        modifier = Modifier
                            .clickable {
                                if (fileName.value.isNotEmpty()) {
                                    confirm(fileName.value, pathName.value)
                                }
                            },
                        text = "Save",
                        fontFamily = Typography.bodyMedium.fontFamily,
                        fontSize = Typography.bodyMedium.fontSize,
                        fontWeight = Typography.bodyMedium.fontWeight,
                        lineHeight = Typography.bodyMedium.lineHeight,
                        color = if (fileName.value.isNotEmpty()) Color.White else Color.Gray,
                    )
                }
            }
        }
    }
}

@Composable
fun DialogOverride(
    confirm: () -> Unit,
    cancel: () -> Unit,
) {
    val outsideInteractionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .zIndex(1f)
            .fillMaxSize()
            .clickable(
                interactionSource = outsideInteractionSource,
                indication = null,
            ) {
                cancel()
            },
    ) {
        val interactionSource = remember { MutableInteractionSource() }

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .width(300.dp)
                .height(200.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable(interactionSource = interactionSource, indication = null) {  }
                .background(Color.DarkGray),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(30.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally),
                    text = "Override note",
                    fontFamily = Typography.bodyMedium.fontFamily,
                    fontSize = Typography.bodyMedium.fontSize,
                    fontWeight = Typography.bodyMedium.fontWeight,
                    lineHeight = Typography.bodyMedium.lineHeight,
                    color = Color.White,
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        modifier = Modifier
                            .clickable {
                                cancel()
                            },
                        text = "Cancel",
                        fontFamily = Typography.bodyMedium.fontFamily,
                        fontSize = Typography.bodyMedium.fontSize,
                        fontWeight = Typography.bodyMedium.fontWeight,
                        lineHeight = Typography.bodyMedium.lineHeight,
                        color = Color.White,
                    )

                    Text(
                        modifier = Modifier
                            .clickable {
                                confirm()
                            },
                        text = "Save",
                        fontFamily = Typography.bodyMedium.fontFamily,
                        fontSize = Typography.bodyMedium.fontSize,
                        fontWeight = Typography.bodyMedium.fontWeight,
                        lineHeight = Typography.bodyMedium.lineHeight,
                        color = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
fun DialogRenameRootFolder(
    confirm: (name: String) -> Unit,
    cancel: () -> Unit,
    presetFolderName: String,
) {
    val outsideInteractionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .zIndex(1f)
            .fillMaxSize()
            .clickable(
                interactionSource = outsideInteractionSource,
                indication = null,
            ) {
                cancel()
            },
    ) {
        val interactionSource = remember { MutableInteractionSource() }

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .width(300.dp)
                .height(400.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable(interactionSource = interactionSource, indication = null) {  }
                .background(Color.DarkGray),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(30.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally),
                    text = "Save note",
                    fontFamily = Typography.bodyMedium.fontFamily,
                    fontSize = Typography.bodyMedium.fontSize,
                    fontWeight = Typography.bodyMedium.fontWeight,
                    lineHeight = Typography.bodyMedium.lineHeight,
                    color = Color.White,
                )

                val focusManager = LocalFocusManager.current
                val focusRequester = remember { FocusRequester() }

                val customTextSelectionColors = TextSelectionColors(
                    handleColor = Color.Gray,
                    backgroundColor = Color.DarkGray
                )

                val folderName = remember { mutableStateOf(presetFolderName) }
                val textFieldFocused = remember { mutableStateOf(false) }

                CompositionLocalProvider(
                    LocalTextSelectionColors provides customTextSelectionColors
                ) {
                    BasicTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .focusRequester(focusRequester)
                            .onFocusChanged {
                                if (it.isFocused) {
                                    textFieldFocused.value = true
                                }
                            }
                            .background(Color.White),
                        value = folderName.value,
                        onValueChange = {
                            folderName.value = it
                        },
                        cursorBrush = Brush.verticalGradient(
                            0.00f to Color.Black,
                            0.15f to Color.Black,
                            0.15f to Color.Black,
                            0.75f to Color.Black,
                            0.75f to Color.Black,
                            1.00f to Color.Black,
                        ),
                        textStyle = TextStyle(
                            textAlign = TextAlign.Start,
                            color = Color.Black,
                            fontFamily = Typography.titleMedium.fontFamily,
                            fontSize = Typography.titleMedium.fontSize,
                            lineHeight = Typography.titleMedium.lineHeight,
                            letterSpacing = Typography.titleMedium.letterSpacing,
                        ),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            autoCorrectEnabled = false,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                textFieldFocused.value = false
                            }
                        ),
                        singleLine = true,
                        maxLines = 1,
                        visualTransformation = VisualTransformation.None,
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                            ) {
                                if (folderName.value.isEmpty()) {
                                    Text(
                                        modifier = Modifier
                                            .align(Alignment.CenterStart)
                                            .padding(5.dp),
                                        text = "Name the file",
                                        textAlign = TextAlign.Left,
                                        fontFamily = FontFamily(
                                            Font(R.font.roboto_italic)
                                        ),
                                        fontSize = Typography.titleMedium.fontSize,
                                        fontWeight = Typography.titleMedium.fontWeight,
                                        lineHeight = Typography.titleMedium.lineHeight,
                                        color = Color.Gray
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.CenterStart)
                                            .padding(5.dp)
                                    ) {
                                        innerTextField()
                                    }
                                }
                            }
                        },
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        modifier = Modifier
                            .clickable {
                                cancel()
                            },
                        text = "Cancel",
                        fontFamily = Typography.bodyMedium.fontFamily,
                        fontSize = Typography.bodyMedium.fontSize,
                        fontWeight = Typography.bodyMedium.fontWeight,
                        lineHeight = Typography.bodyMedium.lineHeight,
                        color = Color.White,
                    )

                    Text(
                        modifier = Modifier
                            .clickable {
                                if (folderName.value.isNotEmpty()) {
                                    confirm(folderName.value)
                                }
                            },
                        text = "Save",
                        fontFamily = Typography.bodyMedium.fontFamily,
                        fontSize = Typography.bodyMedium.fontSize,
                        fontWeight = Typography.bodyMedium.fontWeight,
                        lineHeight = Typography.bodyMedium.lineHeight,
                        color = if (folderName.value.isNotEmpty()) Color.White else Color.Gray,
                    )
                }
            }
        }
    }
}