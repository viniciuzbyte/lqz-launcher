package com.lqz.launcher

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.lqz.launcher.data.ImageStore
import com.lqz.launcher.data.LauncherViewModel
import com.lqz.launcher.ui.HostActions
import com.lqz.launcher.ui.LauncherRoot
import com.lqz.launcher.ui.WidgetEnv
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private val vm: LauncherViewModel by viewModels()
    private lateinit var widgetHost: AppWidgetHost
    private lateinit var widgetManager: AppWidgetManager
    private var pendingWidgetId = -1
    private var resumed = false
    private var imageCallback: ((String?) -> Unit)? = null

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (resumed && vm.settings.value.lockEnabled) vm.lockNow()
        }
    }

    private val bindLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            afterBind(pendingWidgetId)
        } else {
            discardPendingWidget()
        }
    }

    private val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val callback = imageCallback
        imageCallback = null
        if (callback != null) {
            if (uri == null) {
                callback(null)
            } else {
                lifecycleScope.launch {
                    val path = withContext(Dispatchers.IO) { ImageStore.import(applicationContext, uri) }
                    callback(path)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        widgetHost = AppWidgetHost(applicationContext, HOST_ID)
        widgetManager = AppWidgetManager.getInstance(applicationContext)
        ContextCompat.registerReceiver(
            this,
            screenOffReceiver,
            IntentFilter(Intent.ACTION_SCREEN_OFF),
            ContextCompat.RECEIVER_EXPORTED
        )
        val env = WidgetEnv(widgetHost, widgetManager, ::requestWidget, ::removeWidget)
        val host = HostActions(
            pickImage = ::pickImage,
            dim = ::applyDim,
            openNotificationAccess = ::openNotificationAccess,
            systemBars = ::applySystemBars,
            blur = ::applyBlur
        )
        setContent {
            LauncherRoot(vm = vm, env = env, host = host)
        }
    }

    override fun onStart() {
        super.onStart()
        widgetHost.startListening()
    }

    override fun onStop() {
        widgetHost.stopListening()
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        resumed = true
    }

    override fun onPause() {
        resumed = false
        super.onPause()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN) vm.homeEvents.tryEmit(Unit)
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(screenOffReceiver) }
        super.onDestroy()
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CONFIGURE_REQUEST) {
            if (resultCode == RESULT_OK) finishWidget(pendingWidgetId) else discardPendingWidget()
        }
    }

    private fun applyBlur(radius: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.setBackgroundBlurRadius(radius)
        }
    }

    private fun applyDim(level: Float?) {
        val attrs = window.attributes
        attrs.screenBrightness = level ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        window.attributes = attrs
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        if (level != null) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    private fun applySystemBars(darkTheme: Boolean) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = !darkTheme
        controller.isAppearanceLightNavigationBars = !darkTheme
    }

    private fun openNotificationAccess() {
        runCatching { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
    }

    private fun pickImage(callback: (String?) -> Unit) {
        imageCallback = callback
        imagePicker.launch("image/*")
    }

    private fun requestWidget(info: AppWidgetProviderInfo) {
        val id = widgetHost.allocateAppWidgetId()
        pendingWidgetId = id
        if (widgetManager.bindAppWidgetIdIfAllowed(id, info.provider)) {
            afterBind(id)
        } else {
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
            bindLauncher.launch(intent)
        }
    }

    private fun afterBind(id: Int) {
        val info = widgetManager.getAppWidgetInfo(id)
        if (info?.configure != null) {
            widgetHost.startAppWidgetConfigureActivityForResult(this, id, 0, CONFIGURE_REQUEST, null)
        } else {
            finishWidget(id)
        }
    }

    private fun finishWidget(id: Int) {
        if (id >= 0) vm.addWidget(id)
        pendingWidgetId = -1
    }

    private fun discardPendingWidget() {
        if (pendingWidgetId >= 0) widgetHost.deleteAppWidgetId(pendingWidgetId)
        pendingWidgetId = -1
    }

    private fun removeWidget(id: Int) {
        widgetHost.deleteAppWidgetId(id)
        vm.removeWidget(id)
    }

    companion object {
        private const val HOST_ID = 0x4C5A
        private const val CONFIGURE_REQUEST = 4001
    }
}
