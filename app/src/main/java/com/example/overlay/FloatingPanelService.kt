package com.example.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.MainActivity
import com.example.R

class FloatingPanelService : Service(), LifecycleOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private var windowManager: WindowManager? = null
    private var overlayView: ComposeView? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        createNotificationChannel()
        startForegroundCompat()

        if (!Settings.canDrawOverlays(this)) {
            OverlayPanelStateHolder.setOverlayRunning(false)
            stopSelf()
            return
        }

        setupOverlayWindow()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        OverlayPanelStateHolder.setOverlayRunning(true)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopOverlayAndSelf()
            return START_NOT_STICKY
        }

        if (!Settings.canDrawOverlays(this)) {
            stopOverlayAndSelf()
            return START_NOT_STICKY
        }

        if (overlayView == null) {
            setupOverlayWindow()
        }
        OverlayPanelStateHolder.setOverlayRunning(true)
        return START_STICKY
    }

    override fun onDestroy() {
        removeOverlayWindow()
        OverlayPanelStateHolder.setOverlayRunning(false)
        if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.CREATED)) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun setupOverlayWindow() {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager = wm

        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val density = resources.displayMetrics.density
        val initialX = (16 * density).toInt()
        val initialY = (160 * density).toInt()

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = initialX
            y = initialY
        }
        layoutParams = params

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@FloatingPanelService)
            setViewTreeSavedStateRegistryOwner(this@FloatingPanelService)
            setContent {
                val state by OverlayPanelStateHolder.state.collectAsStateWithLifecycle()
                FloatingOverlayContent(
                    state = state,
                    onToggleExpanded = { OverlayPanelStateHolder.toggleExpanded() },
                    onToggleAimbot = { OverlayPanelStateHolder.toggleAimbot() },
                    onToggleEsp = { OverlayPanelStateHolder.toggleLocationEsp() },
                    onReset = { OverlayPanelStateHolder.resetToggles() },
                    onClose = { stopOverlayAndSelf() },
                    onDragDelta = { dx, dy -> updateWindowPosition(dx, dy) }
                )
            }
        }
        overlayView = composeView
        composeView.addOnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
            val metrics = resources.displayMetrics
            val maxX = (metrics.widthPixels - view.width).coerceAtLeast(0)
            val maxY = (metrics.heightPixels - view.height).coerceAtLeast(0)
            val clampedX = params.x.coerceIn(0, maxX)
            val clampedY = params.y.coerceIn(0, maxY)
            if (clampedX != params.x || clampedY != params.y) {
                params.x = clampedX
                params.y = clampedY
                if (view.isAttachedToWindow) {
                    wm.updateViewLayout(view, params)
                }
            }
        }
        wm.addView(composeView, params)
    }

    private fun updateWindowPosition(dx: Int, dy: Int) {
        val wm = windowManager ?: return
        val view = overlayView ?: return
        val params = layoutParams ?: return

        val metrics = resources.displayMetrics
        val maxX = (metrics.widthPixels - view.width).coerceAtLeast(0)
        val maxY = (metrics.heightPixels - view.height).coerceAtLeast(0)

        params.x = (params.x + dx).coerceIn(0, maxX)
        params.y = (params.y + dy).coerceIn(0, maxY)

        if (view.isAttachedToWindow) {
            wm.updateViewLayout(view, params)
        }
    }

    private fun removeOverlayWindow() {
        val view = overlayView ?: return
        val wm = windowManager
        if (view.isAttachedToWindow && wm != null) {
            runCatching { wm.removeViewImmediate(view) }
        }
        overlayView = null
        layoutParams = null
    }

    private fun stopOverlayAndSelf() {
        OverlayPanelStateHolder.setOverlayRunning(false)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startForegroundCompat() {
        val notification = buildNotification()
        val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, fgsType)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.overlay_notification_channel),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val closeIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, FloatingPanelService::class.java).apply {
                action = ACTION_STOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_reticle)
            .setContentTitle(getString(R.string.overlay_notification_title))
            .setContentText(getString(R.string.overlay_notification_text))
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(openAppIntent)
            .addAction(
                0,
                getString(R.string.overlay_notification_close),
                closeIntent
            )
            .build()
    }

    companion object {
        const val ACTION_START = "com.example.overlay.ACTION_START"
        const val ACTION_STOP = "com.example.overlay.ACTION_STOP"
        private const val CHANNEL_ID = "faiz_overlay_channel"
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, FloatingPanelService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, FloatingPanelService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
