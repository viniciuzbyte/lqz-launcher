package com.lqz.launcher.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lqz.launcher.data.AppEntry
import com.lqz.launcher.data.launchApp
import kotlinx.coroutines.launch
import kotlin.math.exp

private sealed interface DrawerItem {
    data class Header(val letter: Char) : DrawerItem
    data class App(val app: AppEntry) : DrawerItem
}

private val RailLetters: List<Char> = listOf('#') + ('A'..'Z').toList()

private fun buildItems(apps: List<AppEntry>, grouped: Boolean): List<DrawerItem> {
    if (!grouped) return apps.map { DrawerItem.App(it) }
    val out = ArrayList<DrawerItem>()
    apps.groupBy { it.letter }
        .toSortedMap(compareBy<Char> { if (it == '#') '\u0000' else it })
        .forEach { (letter, list) ->
            out.add(DrawerItem.Header(letter))
            list.forEach { out.add(DrawerItem.App(it)) }
        }
    return out
}

@Composable
fun AppDrawer(
    apps: List<AppEntry>,
    isOpen: Boolean,
    progress: () -> Float,
    onPull: (Float) -> Unit,
    onRelease: (Float) -> Unit,
    onLaunched: () -> Unit,
    onAppMenu: (AppEntry) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val focus = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var query by remember { mutableStateOf("") }
    var bubble by remember { mutableStateOf<Char?>(null) }
    var lastBubble by remember { mutableStateOf('A') }

    val filtered = remember(apps, query) {
        val q = query.trim()
        if (q.isEmpty()) apps else apps.filter { it.label.contains(q, ignoreCase = true) }
    }
    val grouped = query.isBlank()
    val items = remember(filtered, grouped) { buildItems(filtered, grouped) }
    val letterIndex = remember(items) {
        val map = HashMap<Char, Int>()
        items.forEachIndexed { i, item -> if (item is DrawerItem.Header) map[item.letter] = i }
        map
    }

    LaunchedEffect(isOpen) {
        if (!isOpen) {
            query = ""
            focus.clearFocus()
            listState.scrollToItem(0)
        }
    }

    val currentPull by rememberUpdatedState(onPull)
    val currentRelease by rememberUpdatedState(onRelease)
    val currentProgress by rememberUpdatedState(progress)
    val connection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y < 0f && currentProgress() < 1f) {
                    currentPull(available.y)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y > 0f) {
                    currentPull(available.y)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                currentRelease(available.y)
                return available
            }
        }
    }

    fun targetIndex(ch: Char): Int? {
        val start = RailLetters.indexOf(ch)
        for (i in start until RailLetters.size) letterIndex[RailLetters[i]]?.let { return it }
        for (i in start downTo 0) letterIndex[RailLetters[i]]?.let { return it }
        return null
    }

    Box(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(Modifier.fillMaxSize()) {
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 17.sp),
                cursorBrush = SolidColor(Color.White),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    filtered.firstOrNull()?.let {
                        launchApp(context, it)
                        onLaunched()
                    }
                }),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) {
                            Text("Cari aplikasi", color = Color.White.copy(alpha = 0.55f), fontSize = 17.sp)
                        }
                        inner()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .liquidGlass(RoundedCornerShape(50), strength = 0.9f, elevation = 6.dp)
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            )
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .nestedScroll(connection),
                contentPadding = PaddingValues(start = 20.dp, end = 58.dp, top = 8.dp, bottom = 28.dp)
            ) {
                itemsIndexed(
                    items,
                    key = { _, item ->
                        when (item) {
                            is DrawerItem.Header -> "h${item.letter}"
                            is DrawerItem.App -> item.app.key
                        }
                    }
                ) { _, item ->
                    when (item) {
                        is DrawerItem.Header -> LetterHeader(item.letter, Modifier.animateItem())
                        is DrawerItem.App -> AppRow(
                            app = item.app,
                            modifier = Modifier.animateItem(),
                            onLaunch = {
                                launchApp(context, item.app)
                                onLaunched()
                            },
                            onMenu = { onAppMenu(item.app) }
                        )
                    }
                }
            }
        }

        if (grouped) {
            AlphabetRail(
                letters = RailLetters,
                available = letterIndex.keys,
                onTouch = { ch ->
                    bubble = ch
                    if (ch != null) {
                        lastBubble = ch
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val target = targetIndex(ch)
                        if (target != null) scope.launch { listState.scrollToItem(target) }
                    }
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp)
                    .fillMaxHeight(0.72f)
                    .width(40.dp)
            )
        }

        AnimatedVisibility(
            visible = bubble != null,
            modifier = Modifier.align(Alignment.Center),
            enter = fadeIn() + scaleIn(initialScale = 0.6f),
            exit = fadeOut() + scaleOut(targetScale = 0.8f)
        ) {
            Box(
                Modifier
                    .size(96.dp)
                    .liquidGlass(CircleShape, strength = 2f, elevation = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    lastBubble.toString(),
                    color = Color.White,
                    fontSize = 46.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun LetterHeader(letter: Char, modifier: Modifier = Modifier) {
    Box(
        modifier
            .padding(top = 14.dp, bottom = 4.dp, start = 4.dp)
            .size(36.dp)
            .liquidGlass(CircleShape, strength = 1f, elevation = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(letter.toString(), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AppRow(app: AppEntry, modifier: Modifier, onLaunch: () -> Unit, onMenu: () -> Unit) {
    Row(
        modifier
            .fillMaxWidth()
            .bounceClick(onLongClick = onMenu, onClick = onLaunch)
            .padding(horizontal = 6.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIcon(app)
        Spacer(Modifier.width(16.dp))
        Text(
            app.label,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            style = TextStyle(shadow = Shadow(Color.Black.copy(alpha = 0.35f), Offset(0f, 2f), 8f))
        )
    }
}

@Composable
private fun AlphabetRail(
    letters: List<Char>,
    available: Set<Char>,
    onTouch: (Char?) -> Unit,
    modifier: Modifier = Modifier
) {
    var touchY by remember { mutableFloatStateOf(-1f) }
    var heightPx by remember { mutableIntStateOf(1) }
    val strength by animateFloatAsState(
        targetValue = if (touchY >= 0f) 1f else 0f,
        animationSpec = spring(stiffness = 300f),
        label = "railStrength"
    )
    val currentOnTouch by rememberUpdatedState(onTouch)

    Column(
        modifier
            .onSizeChanged { heightPx = it.height.coerceAtLeast(1) }
            .pointerInput(letters) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var last = -1
                    fun handle(y: Float) {
                        touchY = y.coerceIn(0f, heightPx.toFloat() - 1f)
                        val idx = (touchY / heightPx * letters.size).toInt().coerceIn(0, letters.lastIndex)
                        if (idx != last) {
                            last = idx
                            currentOnTouch(letters[idx])
                        }
                    }
                    handle(down.position.y)
                    down.consume()
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        handle(change.position.y)
                        change.consume()
                    }
                    touchY = -1f
                    currentOnTouch(null)
                }
            }
    ) {
        letters.forEachIndexed { i, ch ->
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .graphicsLayer {
                        val itemH = size.height
                        val center = (i + 0.5f) * itemH
                        val d = if (touchY < 0f) 99f else (center - touchY) / (itemH * 2.2f)
                        val wave = exp(-d * d) * strength
                        translationX = -wave * 46.dp.toPx()
                        val s = 1f + wave * 0.9f
                        scaleX = s
                        scaleY = s
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    ch.toString(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (ch in available) Color.White else Color.White.copy(alpha = 0.28f)
                )
            }
        }
    }
}
