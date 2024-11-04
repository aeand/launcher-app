package com.example.my_launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import java.io.File

@Composable
fun NotesPage(
    modifier: Modifier,
    enabled: MutableState<Boolean>,
    error: MutableState<Boolean>,
    textColor: Color,
    saveFile: (name: String, folder: String, content: String) -> Unit,
    readFile: (file: File) -> String,
    dirContent: MutableList<File>,
) {
    val interactionSource = remember {
        MutableInteractionSource()
    }

    val text = remember {
        mutableStateOf("")
    }

    val showSaveDialog = remember {
        mutableStateOf(false)
    }

    val showDirMenu = remember {
        mutableStateOf(false)
    }

    val textFieldFocused = remember {
        mutableStateOf(false)
    }

    val presetFileName = remember {
        mutableStateOf("")
    }

    if (showSaveDialog.value) {
        NoteSaveDialog(
            showDialog = showSaveDialog,
            noteContent = text.value,
            saveFile = saveFile,
            presetFileName = presetFileName.value,
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        val focusManager = LocalFocusManager.current
        val focusRequester = remember {
            FocusRequester()
        }

        LaunchedEffect(enabled) {
            focusManager.clearFocus()
            textFieldFocused.value = false
            //TODO showDirMenu.value = false
        }

        val customTextSelectionColors = TextSelectionColors(
            handleColor = Color.Gray,
            backgroundColor = Color.DarkGray
        )

        CompositionLocalProvider(
            LocalTextSelectionColors provides customTextSelectionColors
        ) {
            BasicTextField(
                modifier = Modifier
                    .padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 50.dp)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(10.dp))
                    .focusRequester(focusRequester)
                    .onFocusChanged {
                        if (it.isFocused) {
                            textFieldFocused.value = true
                            showDirMenu.value = false
                        }
                    }
                    .background(Color.White),
                value = text.value,
                onValueChange = { it: String ->
                    text.value = it
                },
                cursorBrush = Brush.verticalGradient(
                    0.00f to Color.Black,
                    0.15f to Color.Black,
                    0.15f to Color.Black,
                    0.75f to Color.Black,
                    0.75f to Color.Black,
                    1.00f to Color.Black,
                ),
                enabled = enabled.value,
                textStyle = TextStyle(
                    textAlign = TextAlign.Start,
                    color = if (error.value) Color.Red else Color.Black,
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
                singleLine = false,
                maxLines = Int.MAX_VALUE,
                visualTransformation = VisualTransformation.None,
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        if (text.value.isEmpty()) {
                            Text(
                                modifier = Modifier,
                                text = "Write something",
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
                            innerTextField()
                        }
                    }
                },
                onTextLayout = {},
                interactionSource = interactionSource,
                minLines = 1,
            )
        }

        Text(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-60).dp, y = (-11).dp)
                .clickable {
                    focusManager.clearFocus()
                    textFieldFocused.value = false
                    showSaveDialog.value = true
                },
            text = "Save",
            color = textColor,
            fontFamily = Typography.bodyLarge.fontFamily,
            fontSize = Typography.bodyLarge.fontSize,
            fontWeight = Typography.bodyLarge.fontWeight,
            lineHeight = Typography.bodyLarge.lineHeight,
        )

        if (!showDirMenu.value) {
            Icon(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(50.dp)
                    .clickable { showDirMenu.value = !showDirMenu.value },
                painter = painterResource(R.drawable.burger_menu),
                contentDescription = null,
                tint = Color.White
            )
        }

        if (showDirMenu.value) {
            Column(
                modifier = Modifier
                    .width(200.dp)
                    .fillMaxHeight()
                    .align(Alignment.BottomEnd)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.DarkGray)
                    .clickable (interactionSource = interactionSource, indication = null) {}
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Bottom,
            ) {
                dirContent.forEach { dir ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clickable {
                                if (dir.isFile) {
                                    text.value = readFile(dir)
                                    presetFileName.value = dir.nameWithoutExtension
                                    showDirMenu.value = false
                                }
                                else {
                                    //TODO
                                    // expand/shrink folder
                                    // and if not file or folder handle case correctly
                                }
                            },
                    ) {
                        Icon(
                            modifier = Modifier
                                .padding(start = 5.dp, top = 10.dp, bottom = 10.dp),
                            painter = painterResource(if (dir.isFile) R.drawable.file else R.drawable.folder),
                            contentDescription = null,
                            tint = Color.Black,
                        )

                        Text( //TODO what if the file has a mega name. Will it fit?
                            modifier = Modifier
                                .padding(start = 5.dp, top = 10.dp, bottom = 10.dp),
                            text = if (dir.isFile) dir.nameWithoutExtension else dir.name,
                            color = textColor,
                            fontFamily = Typography.bodyLarge.fontFamily,
                            fontSize = Typography.bodyLarge.fontSize,
                            fontWeight = Typography.bodyLarge.fontWeight,
                            lineHeight = Typography.bodyLarge.lineHeight,
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 30.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Icon(
                        modifier = Modifier
                            .size(50.dp)
                            .clickable {
                                text.value = ""
                                presetFileName.value = ""
                                showDirMenu.value = !showDirMenu.value
                            },
                        painter = painterResource(R.drawable.plus),
                        contentDescription = null,
                        tint = Color.White,
                    )

                    Icon(
                        modifier = Modifier
                            .size(50.dp)
                            .clickable {
                                showDirMenu.value = !showDirMenu.value
                            },
                        painter = painterResource(R.drawable.x),
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun NoteSaveDialog(
    showDialog: MutableState<Boolean>,
    noteContent: String,
    saveFile: (name: String, folder: String, content: String) -> Unit,
    presetFileName: String,
) {
    Box(
        modifier = Modifier
            .zIndex(1f)
            .fillMaxSize()
            .clickable {
                showDialog.value = false
            },
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .width(300.dp)
                .height(400.dp)
                .clip(RoundedCornerShape(10.dp))
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

                val fileName = remember {
                    mutableStateOf(presetFileName)
                }

                val focusManager = LocalFocusManager.current
                val focusRequester = remember {
                    FocusRequester()
                }

                val textFieldFocused = remember {
                    mutableStateOf(false)
                }

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
                                    textFieldFocused.value = true
                                }
                            }
                            .background(Color.White),
                        value = fileName.value,
                        onValueChange = { it: String ->
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
                                        modifier = Modifier,
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
                                    innerTextField()
                                }
                            }
                        },
                    )
                }

                Text(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally),
                    text = "to: [implement dir picker]",
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
                                showDialog.value = false
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
                                    saveFile(fileName.value, "", noteContent)
                                    showDialog.value = false
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