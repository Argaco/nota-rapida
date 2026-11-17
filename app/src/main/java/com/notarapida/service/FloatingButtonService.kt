package com.notarapida.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.notarapida.MainActivity
import com.notarapida.Prefs
import com.notarapida.QuickNoteActivity
import kotlin.math.abs

/**
 * Mantiene un botón amarillo flotante sobre cualquier app.
 * Al tocarlo se abre la nota rápida. Se puede arrastrar a otro lugar de la pantalla.
 */
class FloatingButtonService : Service() {

    private var windowManager: WindowManager? = null
    private var bubble: View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        // Android puede rechazar el arranque en primer plano (por ejemplo, al reiniciar el
        // servicio en segundo plano); en ese caso nos detenemos sin cerrar la app.
        val startedAsForeground = runCatching { startAsForeground() }.isSuccess

        // Sin el permiso de "mostrarse sobre otras apps" no hay nada que dibujar.
        if (!startedAsForeground || !Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        windowManager = getSystemService(WindowManager::class.java)
        addBubble()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            Prefs.setFloatingEnabled(this, false)
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        bubble?.let { view -> runCatching { windowManager?.removeView(view) } }
        bubble = null
        super.onDestroy()
    }

    private fun startAsForeground() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Botón flotante",
                NotificationManager.IMPORTANCE_MIN,
            ).apply {
                description = "Aviso fijo mientras el botón flotante está activo"
            },
        )

        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, FloatingButtonService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setContentTitle("Nota Rápida está activa")
            .setContentText("Toca el botón amarillo para anotar")
            .setContentIntent(openApp)
            .addAction(0, "Desactivar", stopIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()

        if (Build.VERSION.SDK_INT >= 34) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun addBubble() {
        val size = dp(56)
        val screenWidth = resources.displayMetrics.widthPixels

        val view = FrameLayout(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xFFFFEB3B.toInt())
                setStroke(dp(2), 0xFFB59B00.toInt())
            }
            contentDescription = "Nueva nota"
            addView(
                ImageView(context).apply {
                    setImageResource(android.R.drawable.ic_menu_edit)
                    setColorFilter(0xFF3E3A00.toInt())
                },
                FrameLayout.LayoutParams(dp(28), dp(28), Gravity.CENTER),
            )
            setOnClickListener { openQuickNote() }
        }

        val layoutParams = WindowManager.LayoutParams(
            size,
            size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = if (Prefs.bubbleOnRight(this@FloatingButtonService)) screenWidth - size else 0
            y = Prefs.bubbleY(this@FloatingButtonService)
        }

        val slop = ViewConfiguration.get(this).scaledTouchSlop
        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f
        var dragging = false

        view.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = layoutParams.x
                    startY = layoutParams.y
                    touchX = event.rawX
                    touchY = event.rawY
                    dragging = false
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - touchX
                    val dy = event.rawY - touchY
                    if (!dragging && (abs(dx) > slop || abs(dy) > slop)) dragging = true
                    if (dragging) {
                        layoutParams.x = startX + dx.toInt()
                        layoutParams.y = startY + dy.toInt()
                        windowManager?.updateViewLayout(v, layoutParams)
                    }
                    true
                }

                MotionEvent.ACTION_UP -> {
                    if (dragging) snapToEdge(v, layoutParams) else v.performClick()
                    true
                }

                MotionEvent.ACTION_CANCEL -> {
                    if (dragging) snapToEdge(v, layoutParams)
                    true
                }

                else -> false
            }
        }

        windowManager?.addView(view, layoutParams)
        bubble = view
    }

    /** Al soltar el botón, lo pega al borde izquierdo o derecho más cercano. */
    private fun snapToEdge(view: View, layoutParams: WindowManager.LayoutParams) {
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels
        val onRight = layoutParams.x + view.width / 2 > screenWidth / 2

        layoutParams.x = if (onRight) screenWidth - view.width else 0
        layoutParams.y = layoutParams.y.coerceIn(0, screenHeight - view.height)
        windowManager?.updateViewLayout(view, layoutParams)
        Prefs.setBubblePosition(this, onRight, layoutParams.y)
    }

    private fun openQuickNote() {
        val intent = Intent(this, QuickNoteActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val CHANNEL_ID = "floating_button"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_STOP = "com.notarapida.action.STOP"

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, FloatingButtonService::class.java),
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, FloatingButtonService::class.java))
        }
    }
}
