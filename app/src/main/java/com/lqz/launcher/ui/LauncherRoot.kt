package com.lqz.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lqz.launcher.data.AppEntry
import com.lqz.launcher.data.LauncherViewModel
import com.lqz.launcher.data.openAppInfo
import com.lqz.launcher.data.openWallpaperPicker
import com.lqz.launcher.data.uninstallApp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@Composable
fun LauncherRoot(vm: LauncherViewModel, env: WidgetEnv, onBlur: (Int) -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val apps by vm.apps.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val widgets by vm.widgets.collectAsStateWithLifecycle()
    val locked by vm.locked.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pager = rememberPagerState(initialPage = 1) { 2 }
    val drawer = remember { Animatable(0f) }
    var showSettings by remember { mutableStateOf(false) }
    var showPicker by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var menuVisible by remember { mutableStateOf(false) }
    var menuApp by remember { mutableStateOf<AppEntry?>(null) }
    val drawerOpen by remember { derivedStateOf { drawer.value > 0.02f } }
    val overlayBlur by animateFloatAsState(
        targetValue = if (showSettings || showPicker) 1f else 0f,
        animationSpec = tween(320),
        label = "overlayBlur"
    )
    val favoriteApps = remember(apps, favorites) {
        val byKey = apps.associateBy { it.key }
        favorites.orEmpty().mapNotNull { byKey[it] }
    }

    MaterialTheme(colorScheme = darkColorScheme()) {
        CompositionLocalProvider(LocalSettings provides settings) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val heightPx = with(LocalDensity.current) { maxHeight.toPx() }

                LaunchedEffect(settings.blurRadius) {
                    snapshotFlow {
                        (maxOf(drawer.value, overlayBlur) * settings.blurRadius).toInt() / 4 * 4
                    }.distinctUntilChanged().collect { onBlur(it) }
                }

                LaunchedEffect(Unit) {
                    vm.homeEvents.collect {
                        showSettings = false
                        showPicker = false
                        showMenu = false
                        menuVisible = false
                        launch { drawer.animateTo(0f, spring(dampingRatio = 0.86f, stiffness = 260f)) }
                        launch { pager.animateScrollToPage(1) }
                    }
                }

                fun settle(velocity: Float) {
                    val target = when {
                        velocity < -900f -> 1f
                        velocity > 900f -> 0f
                        drawer.value > 0.35f -> 1f
                        else -> 0f
                    }
                    scope.launch { drawer.animateTo(target, spring(dampingRatio = 0.86f, stiffness = 260f)) }
                }

                fun dragDrawer(deltaPx: Float) {
                    scope.launch { drawer.snapTo((drawer.value - deltaPx / heightPx * 1.1f).coerceIn(0f, 1f)) }
                }

                BackHandler(
                    enabled = showSettings || showPicker || showMenu || menuVisible || drawerOpen || pager.currentPage != 1
                ) {
                    when {
                        showSettings -> showSettings = false
                        showPicker -> showPicker = false
                        showMenu -> showMenu = false
                        menuVisible -> menuVisible = false
                        drawerOpen -> scope.launch { drawer.animateTo(0f, spring(dampingRatio = 0.86f, stiffness = 260f)) }
                        else -> scope.launch { pager.animateScrollToPage(1) }
                    }
                }

                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.30f), Color.Transparent, Color.Black.copy(alpha = 0.40f))
                            )
                        )
                )

                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val p = drawer.value
                            scaleX = 1f - 0.06f * p
                            scaleY = scaleX
                            alpha = 1f - p
                        }
                ) {
                    HorizontalPager(
                        state = pager,
                        userScrollEnabled = !drawerOpen,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        if (page == 0) {
                            WidgetsPage(widgets, env, onAdd = { showPicker = true })
                        } else {
                            HomePage(
                                settings = settings,
                                favorites = favoriteApps,
                                onDrag = { dragDrawer(it) },
                                onDragStopped = { settle(it) },
                                onLongPress = { showMenu = true },
                                onAppMenu = {
                                    menuApp = it
                                    menuVisible = true
                                }
                            )
                        }
                    }
                }

                Box(
                    Modifier
                        .fillMaxSize()
                        .drawBehind { drawRect(Color.Black.copy(alpha = 0.4f * drawer.value)) }
                )

                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationY = (1f - drawer.value) * size.height
                            alpha = (drawer.value * 1.6f).coerceIn(0f, 1f)
                        }
                ) {
                    AppDrawer(
                        apps = apps,
                        isOpen = drawerOpen,
                        progress = { drawer.value },
                        onPull = { dragDrawer(it) },
                        onRelease = { settle(it) },
                        onLaunched = {
                            scope.launch {
                                delay(300)
                                drawer.snapTo(0f)
                            }
                        },
                        onAppMenu = {
                            menuApp = it
                            menuVisible = true
                        }
                    )
                }

                AnimatedVisibility(
                    visible = showPicker,
                    enter = fadeIn(tween(250)) + slideInVertically(tween(300)) { it / 8 },
                    exit = fadeOut(tween(200)) + slideOutVertically(tween(250)) { it / 8 }
                ) {
                    WidgetPicker(env = env, onClose = { showPicker = false })
                }

                AnimatedVisibility(
                    visible = showSettings,
                    enter = fadeIn(tween(250)) + slideInVertically(tween(300)) { it / 8 },
                    exit = fadeOut(tween(200)) + slideOutVertically(tween(250)) { it / 8 }
                ) {
                    SettingsScreen(
                        settings = settings,
                        onChange = { block -> vm.updateSettings(block) },
                        onClose = { showSettings = false },
                        onWallpaper = { openWallpaperPicker(context) },
                        onLockNow = {
                            showSettings = false
                            vm.lockNow()
                        }
                    )
                }

                GlassPopup(visible = showMenu, onDismiss = { showMenu = false }) {
                    PopupAction("Pengaturan LQZ") {
                        showMenu = false
                        showSettings = true
                    }
                    PopupAction("Tambah widget") {
                        showMenu = false
                        showPicker = true
                        scope.launch { pager.animateScrollToPage(0) }
                    }
                    PopupAction("Ganti wallpaper") {
                        showMenu = false
                        openWallpaperPicker(context)
                    }
                    PopupAction("Kunci layar") {
                        showMenu = false
                        vm.lockNow()
                    }
                }

                GlassPopup(visible = menuVisible, onDismiss = { menuVisible = false }) {
                    val app = menuApp
                    if (app != null) {
                        val isFavorite = favorites.orEmpty().contains(app.key)
                        Row(
                            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppIcon(app, scale = 0.9f)
                            Spacer(Modifier.width(14.dp))
                            Text(app.label, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                        PopupAction(if (isFavorite) "Hapus dari favorit" else "Tambah ke favorit") {
                            vm.toggleFavorite(app)
                            menuVisible = false
                        }
                        PopupAction("Info aplikasi") {
                            openAppInfo(context, app.packageName)
                            menuVisible = false
                        }
                        PopupAction("Copot pemasangan") {
                            uninstallApp(context, app.packageName)
                            menuVisible = false
                        }
                    }
                }

                AnimatedVisibility(
                    visible = locked,
                    enter = fadeIn(tween(300)),
                    exit = fadeOut(tween(150))
                ) {
                    LockScreen(settings = settings, onUnlock = { vm.unlock() })
                }
            }
        }
    }
}
