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
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material3.lightColorScheme
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lqz.launcher.data.AppEntry
import com.lqz.launcher.data.CustomWidget
import com.lqz.launcher.data.LauncherViewModel
import com.lqz.launcher.data.openAppInfo
import com.lqz.launcher.data.openWallpaperPicker
import com.lqz.launcher.data.uninstallApp
import com.lqz.launcher.notif.NotificationStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class HostActions(
    val pickImage: ((String?) -> Unit) -> Unit,
    val dim: (Float?) -> Unit,
    val openNotificationAccess: () -> Unit,
    val systemBars: (Boolean) -> Unit,
    val blur: (Int) -> Unit
)

@Composable
fun LauncherRoot(vm: LauncherViewModel, env: WidgetEnv, host: HostActions) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val apps by vm.apps.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val widgets by vm.widgets.collectAsStateWithLifecycle()
    val customWidgets by vm.customWidgets.collectAsStateWithLifecycle()
    val locked by vm.locked.collectAsStateWithLifecycle()
    val notifications by NotificationStore.items.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val systemDark = isSystemInDarkTheme()
    val dark = when (settings.themeMode) {
        1 -> false
        2 -> true
        else -> systemDark
    }
    val accent = AccentColors[settings.accent.coerceIn(0, AccentColors.lastIndex)]
    val palette = remember(dark, accent) { makePalette(dark, accent) }
    val lockPalette = remember(accent) { makePalette(true, accent) }

    var notifAccess by remember { mutableStateOf(NotificationStore.isEnabled(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        notifAccess = NotificationStore.isEnabled(context)
    }
    LaunchedEffect(dark) { host.systemBars(dark) }

    val pager = rememberPagerState(initialPage = 1) { 2 }
    val drawer = remember { Animatable(0f) }
    var showSettings by remember { mutableStateOf(false) }
    var showPicker by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var menuVisible by remember { mutableStateOf(false) }
    var menuApp by remember { mutableStateOf<AppEntry?>(null) }
    var editing by remember { mutableStateOf<CustomWidget?>(null) }
    var editingIsNew by remember { mutableStateOf(false) }
    val drawerOpen by remember { derivedStateOf { drawer.value > 0.02f } }
    val overlayBlur by animateFloatAsState(
        targetValue = if (showSettings || showPicker || editing != null) 1f else 0f,
        animationSpec = tween(320),
        label = "overlayBlur"
    )
    val favoriteApps = remember(apps, favorites) {
        val byKey = apps.associateBy { it.key }
        favorites.orEmpty().mapNotNull { byKey[it] }
    }

    CompositionLocalProvider(LocalSettings provides settings, LocalPalette provides palette) {
        MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val heightPx = with(LocalDensity.current) { maxHeight.toPx() }

                LaunchedEffect(settings.blurRadius) {
                    snapshotFlow {
                        (maxOf(drawer.value, overlayBlur) * settings.blurRadius).toInt() / 4 * 4
                    }.distinctUntilChanged().collect { host.blur(it) }
                }

                LaunchedEffect(Unit) {
                    vm.homeEvents.collect {
                        showSettings = false
                        showPicker = false
                        showMenu = false
                        menuVisible = false
                        editing = null
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
                    enabled = editing != null || showSettings || showPicker || showMenu || menuVisible ||
                        drawerOpen || pager.currentPage != 1
                ) {
                    when {
                        editing != null -> editing = null
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
                                listOf(
                                    palette.scrim,
                                    palette.scrim.copy(alpha = palette.scrim.alpha * 0.55f),
                                    palette.scrim.copy(alpha = (palette.scrim.alpha * 1.2f).coerceAtMost(1f))
                                )
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
                            WidgetsPage(
                                systemWidgets = widgets,
                                customWidgets = customWidgets,
                                env = env,
                                onAddSystem = { showPicker = true },
                                onNewCustom = {
                                    editingIsNew = true
                                    editing = newCustomWidget()
                                },
                                onEditCustom = {
                                    editingIsNew = false
                                    editing = it
                                },
                                onDeleteCustom = { vm.deleteCustomWidget(it) }
                            )
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
                        .drawBehind {
                            drawRect(palette.overlay.copy(alpha = palette.overlay.alpha * 0.7f * drawer.value))
                        }
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
                        notifAccess = notifAccess,
                        onChange = { block -> vm.updateSettings(block) },
                        onClose = { showSettings = false },
                        onWallpaper = { openWallpaperPicker(context) },
                        onLockNow = {
                            showSettings = false
                            vm.lockNow()
                        },
                        onNotifAccess = host.openNotificationAccess
                    )
                }

                AnimatedVisibility(
                    visible = editing != null,
                    enter = fadeIn(tween(250)) + slideInVertically(tween(300)) { it / 8 },
                    exit = fadeOut(tween(200)) + slideOutVertically(tween(250)) { it / 8 }
                ) {
                    val target = editing
                    if (target != null) {
                        WidgetEditor(
                            initial = target,
                            isNew = editingIsNew,
                            onSave = {
                                vm.saveCustomWidget(it)
                                editing = null
                            },
                            onCancel = { editing = null },
                            onPickImage = host.pickImage
                        )
                    }
                }

                GlassPopup(visible = showMenu, onDismiss = { showMenu = false }) {
                    PopupAction("Pengaturan LQZ") {
                        showMenu = false
                        showSettings = true
                    }
                    PopupAction("Buat widget") {
                        showMenu = false
                        editingIsNew = true
                        editing = newCustomWidget()
                        scope.launch { pager.animateScrollToPage(0) }
                    }
                    PopupAction("Widget sistem") {
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
                            Text(
                                app.label,
                                color = ThemeText,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
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
                    CompositionLocalProvider(LocalPalette provides lockPalette) {
                        LockScreen(
                            settings = settings,
                            notifications = if (settings.lockShowNotifications) notifications else emptyList(),
                            notifAccess = notifAccess,
                            onUnlock = { vm.unlock() },
                            onRequestAccess = host.openNotificationAccess,
                            onDim = host.dim
                        )
                    }
                }
            }
        }
    }
}
