package com.clicker.app.overlay

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import com.clicker.app.R
import com.clicker.app.accessibility.ClickerService

/**
 * 悬浮球服务：可拖拽的小球，点击展开控制面板。
 */
class FloatingBallService : Service() {

    private lateinit var windowManager: WindowManager
    private var ballView: View? = null
    private var panelView: View? = null

    private var startX = 0
    private var startY = 0
    private var touchStartX = 0f
    private var touchStartY = 0f
    private var isDragging = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        showBall()
    }

    @SuppressLint("ClickableViewAccessibility", "InflateParams")
    private fun showBall() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 200
        }

        val ball = ImageView(this).apply {
            setImageResource(R.drawable.ic_ball)
            setPadding(20, 20, 20, 20)
            setBackgroundColor(0x00000000)
        }

        ball.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x
                    startY = params.y
                    touchStartX = event.rawX
                    touchStartY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - touchStartX
                    val dy = event.rawY - touchStartY
                    if (Math.abs(dx) > 5 || Math.abs(dy) > 5) {
                        isDragging = true
                    }
                    if (isDragging) {
                        params.x = (startX + dx).toInt()
                        params.y = (startY + dy).toInt()
                        windowManager.updateViewLayout(ball, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        togglePanel()
                    }
                    true
                }
                else -> false
            }
        }

        windowManager.addView(ball, params)
        ballView = ball
    }

    @SuppressLint("InflateParams")
    private fun togglePanel() {
        if (panelView != null) {
            removePanel()
            return
        }

        val panel = LayoutInflater.from(this).inflate(R.layout.overlay_panel, null)
        panel.findViewById<View>(R.id.btn_start).setOnClickListener { startClicking() }
        panel.findViewById<View>(R.id.btn_stop).setOnClickListener { stopClicking() }
        panel.findViewById<View>(R.id.btn_close).setOnClickListener { removePanel() }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        windowManager.addView(panel, params)
        panelView = panel
    }

    private fun removePanel() {
        panelView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
        }
        panelView = null
    }

    private fun startClicking() {
        val service = ClickerService.instance
        if (service != null) {
            val task = TaskStore.loadCurrentTask(this)
            service.start(task)
        }
    }

    private fun stopClicking() {
        ClickerService.instance?.stop()
    }

    override fun onDestroy() {
        ballView?.let { try { windowManager.removeView(it) } catch (_: Exception) {} }
        removePanel()
        ballView = null
        super.onDestroy()
    }

    companion object {
        fun isEnabled(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                android.provider.Settings.canDrawOverlays(context)
            } else true
        }
    }
}
