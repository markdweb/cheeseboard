package com.colorgrid.keyboard

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView

class GridOverlayService : Service() {

    companion object {
        const val ACTION_STOP = "com.colorgrid.keyboard.GRID_STOP"

        /** True while the overlay is on screen. The keyboard's Grid button reads this. */
        @Volatile
        var running = false

        /** Called (on the main thread) whenever [running] changes, so the keyboard can redraw its button. */
        var listener: (() -> Unit)? = null
        private const val CHANNEL_ID = "colorgrid"
        private const val NOTIF_ID = 1
        private const val COLS = 12
        private const val ROWS = 3
        private val PINK = Color.parseColor("#FF2D95")
    }

    private lateinit var wm: WindowManager
    private lateinit var prefs: SharedPreferences
    private var d = 1f
    private var screenW = 0
    private var barH = 0
    private var gripS = 0

    // grid rect (px, absolute sa screen)
    private var gx = 0
    private var gy = 0
    private var gw = 0
    private var gh = 0

    private var built = false

    private lateinit var gridView: GridOverlayView
    private lateinit var barView: TextView
    private lateinit var gripView: View

    private lateinit var gridLp: WindowManager.LayoutParams
    private lateinit var barLp: WindowManager.LayoutParams
    private lateinit var gripLp: WindowManager.LayoutParams

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        running = true
        listener?.invoke()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        prefs = getSharedPreferences("grid", MODE_PRIVATE)
        d = resources.displayMetrics.density
        screenW = resources.displayMetrics.widthPixels
        barH = dp(34)
        gripS = dp(34)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        startAsForeground()
        if (!built) {
            try {
                buildWindows()
                built = true
            } catch (e: Exception) {
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        if (built) {
            for (v in listOf<View>(gridView, barView, gripView)) {
                try {
                    wm.removeView(v)
                } catch (e: Exception) {
                    // ignore
                }
            }
            built = false
        }
        running = false
        listener?.invoke()
        super.onDestroy()
    }

    // ---------------- foreground notification ----------------
    private fun startAsForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Color Grid", NotificationManager.IMPORTANCE_LOW)
        )
        val stopPi = PendingIntent.getService(
            this, 0,
            Intent(this, GridOverlayService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val openPi = PendingIntent.getActivity(
            this, 1,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val notif = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("Color Grid")
            .setContentText("Naka-on ang Grid. I-off sa keyboard menu (Grid) o pindutin ang Stop.")
            .setContentIntent(openPi)
            .addAction(Notification.Action.Builder(null, "Stop", stopPi).build())
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, notif)
        }
    }

    // ---------------- helpers ----------------
    private fun dp(v: Int) = (v * d).toInt()

    private fun rounded(color: Int, radiusDp: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radiusDp * d
    }

    private fun lp(w: Int, h: Int, x: Int, y: Int, touchable: Boolean): WindowManager.LayoutParams {
        var flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        if (!touchable) flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        return WindowManager.LayoutParams(
            w, h,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            this.x = x
            this.y = y
        }
    }

    private fun save() {
        prefs.edit().putInt("x", gx).putInt("y", gy).putInt("w", gw).putInt("h", gh).apply()
    }

    // ---------------- build windows ----------------
    private fun buildWindows() {
        val dm = resources.displayMetrics
        gw = prefs.getInt("w", (dm.widthPixels * 0.70f).toInt())
        gh = prefs.getInt("h", (dm.heightPixels * 0.075f).toInt())
        gx = prefs.getInt("x", (dm.widthPixels * 0.15f).toInt())
        gy = prefs.getInt("y", (dm.heightPixels * 0.38f).toInt()).coerceAtLeast(barH)

        // 1) grid: hindi touchable, kaya pwede pa ring mag-scroll/click sa Brave sa ilalim.
        //    alpha <= 0.8 para payagan ng Android 12+ ang touch na dumaan sa overlay.
        gridView = GridOverlayView(this, COLS, ROWS)
        gridLp = lp(gw, gh, gx, gy, false).apply { alpha = 0.8f }
        wm.addView(gridView, gridLp)

        // 2) drag bar (nasa itaas ng grid)
        barView = TextView(this).apply {
            text = "≡  drag to move"
            setTextColor(Color.WHITE)
            textSize = 11f
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#E6444444"))
                val r = 8f * d
                cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
            }
        }
        barLp = lp(gw, barH, gx, gy - barH, true)
        wm.addView(barView, barLp)

        var offX = 0f
        var offY = 0f
        barView.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    offX = e.rawX - gx
                    offY = e.rawY - gy
                }
                MotionEvent.ACTION_MOVE -> {
                    gx = (e.rawX - offX).toInt().coerceAtLeast(0)
                    gy = (e.rawY - offY).toInt().coerceAtLeast(barH)
                    layoutAll()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> save()
            }
            true
        }

        // 3) resize grip (kanang-ibaba ng grid)
        gripView = View(this).apply {
            background = GradientDrawable().apply {
                setColor(PINK)
                setStroke(dp(2), Color.WHITE)
                cornerRadius = 6f * d
            }
        }
        gripLp = lp(gripS, gripS, (gx + gw).coerceAtMost(screenW - gripS), gy + gh, true)
        wm.addView(gripView, gripLp)

        var sx = 0f
        var sy = 0f
        var sw = 0
        var sh = 0
        gripView.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    sx = e.rawX
                    sy = e.rawY
                    sw = gw
                    sh = gh
                }
                MotionEvent.ACTION_MOVE -> {
                    gw = (sw + (e.rawX - sx)).toInt().coerceAtLeast(COLS * dp(6))
                    gh = (sh + (e.rawY - sy)).toInt().coerceAtLeast(ROWS * dp(6))
                    layoutAll()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> save()
            }
            true
        }

        layoutAll()
    }

    private fun layoutAll() {
        gridLp.x = gx
        gridLp.y = gy
        gridLp.width = gw
        gridLp.height = gh

        barLp.x = gx
        barLp.y = gy - barH
        barLp.width = gw

        gripLp.x = (gx + gw).coerceAtMost(screenW - gripS)
        gripLp.y = gy + gh

        try {
            wm.updateViewLayout(gridView, gridLp)
            wm.updateViewLayout(barView, barLp)
            wm.updateViewLayout(gripView, gripLp)
        } catch (e: Exception) {
            // ignore
        }
    }
}
