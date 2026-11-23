package com.netcorte.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.WindowManager
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class FloatingButtonService : Service() {

    private var wm: WindowManager? = null
    private var button: FloatingButtonView? = null
    private var params: WindowManager.LayoutParams? = null

    private val stateListener: (Boolean) -> Unit = { button?.setCut(it) }

    override fun onCreate() {
        super.onCreate()
        running = true
        instance = this
        try {
            startAsForeground()
        } catch (e: Exception) {
            stopSelf(); return
        }
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return }

        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        createButton()
        KillSwitchState.addListener(stateListener)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createButton() {
        val size = dp(Prefs.sizeDp(this))
        val metrics = resources.displayMetrics
        val type = if (Build.VERSION.SDK_INT >= 26)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

        val p = WindowManager.LayoutParams(
            size, size, type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        p.gravity = Gravity.TOP or Gravity.START
        val savedX = Prefs.posX(this)
        val savedY = Prefs.posY(this)
        p.x = if (savedX >= 0) savedX else metrics.widthPixels - size - dp(16)
        p.y = if (savedY >= 0) savedY else (metrics.heightPixels * 0.4f).toInt()
        params = p

        val v = FloatingButtonView(this)
        v.alpha = Prefs.alphaPct(this) / 100f
        v.setCut(KillSwitchState.active)
        attachTouch(v)
        button = v
        wm?.addView(v, p)
    }

    private fun attachTouch(v: FloatingButtonView) {
        val slop = ViewConfiguration.get(this).scaledTouchSlop
        var startX = 0; var startY = 0
        var downX = 0f; var downY = 0f
        var moved = false

        v.setOnTouchListener { view, e ->
            val p = params ?: return@setOnTouchListener false
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = p.x; startY = p.y
                    downX = e.rawX; downY = e.rawY
                    moved = false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - downX
                    val dy = e.rawY - downY
                    if (!moved && (abs(dx) > slop || abs(dy) > slop)) moved = true
                    if (moved) {
                        p.x = startX + dx.toInt()
                        p.y = startY + dy.toInt()
                        clamp(p)
                        wm?.updateViewLayout(view, p)
                    }
                }
                MotionEvent.ACTION_UP -> {
                    if (moved) {
                        Prefs.setPos(this, p.x, p.y)
                    } else {
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        KillSwitchController.toggle(this)
                    }
                }
            }
            true
        }
    }

    fun applyConfig() {
        val p = params ?: return
        val v = button ?: return
        val size = dp(Prefs.sizeDp(this))
        p.width = size
        p.height = size
        clamp(p)
        v.alpha = Prefs.alphaPct(this) / 100f
        wm?.updateViewLayout(v, p)
    }

    private fun clamp(p: WindowManager.LayoutParams) {
        val m = resources.displayMetrics
        p.x = max(0, min(p.x, m.widthPixels - p.width))
        p.y = max(0, min(p.y, m.heightPixels - p.height))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val p = params ?: return
        val v = button ?: return
        clamp(p)
        wm?.updateViewLayout(v, p)
    }

    private fun startAsForeground() {
        val channelId = "netcorte_floating"
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(
                NotificationChannel(channelId, getString(R.string.notif_channel), NotificationManager.IMPORTANCE_LOW)
            )
        }
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val toggle = PendingIntent.getService(
            this, 1,
            Intent(this, KillSwitchVpnService::class.java).setAction(KillSwitchVpnService.ACTION_TOGGLE),
            PendingIntent.FLAG_IMMUTABLE
        )
        @Suppress("DEPRECATION")
        val b = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, channelId) else Notification.Builder(this)
        val n = b.setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(getString(R.string.notif_text))
            .setContentIntent(open)
            .addAction(Notification.Action.Builder(null, getString(R.string.notif_action), toggle).build())
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, n, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, n)
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density + 0.5f).toInt()

    override fun onDestroy() {
        KillSwitchState.removeListener(stateListener)
        try { button?.let { wm?.removeView(it) } } catch (_: Exception) {}
        button = null
        instance = null
        running = false
        super.onDestroy()
    }

    companion object {
        @Volatile var running = false
        @Volatile var instance: FloatingButtonService? = null
    }
}
