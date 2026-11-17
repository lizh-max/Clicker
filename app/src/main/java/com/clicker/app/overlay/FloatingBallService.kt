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
import android.widget.ImageView
import android.widget.Toast
import com.clicker.app.R
import com.clicker.app.accessibility.ClickerService
import com.clicker.app.engine.ClickTask
import com.clicker.app.engine.Step
import com.clicker.app.engine.StepType
import com.clicker.app.util.PermissionHelper

/**
 * 悬浮球服务：可拖拽的小球，点击展开控制面板。
 * 同时管理点位数字标记层（PointMarkersOverlay）。
 */
class FloatingBallService : Service() {

    private lateinit var windowManager: WindowManager
    private var ballView: View? = null
    private var panelView: View? = null
    private var markers: PointMarkersOverlay? = null
    private var editOverlay: PointEditOverlay? = null

    private var startX = 0
    private var startY = 0
    private var touchStartX = 0f
    private var touchStartY = 0f
    private var isDragging = false

    // 悬浮球当前位置（Gravity.TOP|START 坐标系，即屏幕左上角为原点）
    private var ballX = 0
    private var ballY = 200

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startForegroundCompat()
        showBall()
        showMarkers()
    }

    /**
     * 以前台服务方式运行，避免切到后台后 2 秒左右被系统回收。
     */
    private fun startForegroundCompat() {
        val channelId = "clicker_overlay"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            val channel = android.app.NotificationChannel(
                channelId, "连点器悬浮窗", android.app.NotificationManager.IMPORTANCE_LOW
            )
            nm.createNotificationChannel(channel)
        }

        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.Notification.Builder(this, channelId)
        } else {
            @Suppress("DEPRECATION") android.app.Notification.Builder(this)
        }.apply {
            setSmallIcon(R.drawable.ic_notification)
            setContentTitle("连点器运行中")
            setContentText("悬浮球已启动，点击悬浮球进行操作")
            setOngoing(true)
        }.build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1001, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1001, notification)
        }
    }

    // ---------------------------------------------------------------------
    // 悬浮球
    // ---------------------------------------------------------------------

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
            x = ballX
            y = ballY
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
                        ballX = params.x
                        ballY = params.y
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
        val controls = panel.findViewById<View>(R.id.ll_controls)
        val llRunning = panel.findViewById<View>(R.id.ll_running)
        val tvStatus = panel.findViewById<android.widget.TextView>(R.id.tv_status)
        val btnPause = panel.findViewById<View>(R.id.btn_pause)
        val btnStop = panel.findViewById<View>(R.id.btn_stop)

        panel.findViewById<View>(R.id.btn_start).setOnClickListener {
            if (startClicking()) {
                // 真正启动成功后才切换运行状态 UI
                showRunningUi(controls, llRunning, tvStatus, btnPause)
            }
        }
        btnPause.setOnClickListener {
            ClickerService.instance?.let { svc ->
                if (svc.isPaused()) svc.resume() else svc.pause()
                updateRunningUi(tvStatus, btnPause)
            }
        }
        btnStop.setOnClickListener {
            stopClicking()
            controls.visibility = View.VISIBLE
            llRunning.visibility = View.GONE
        }
        panel.findViewById<View>(R.id.btn_pick).setOnClickListener { startPick() }
        panel.findViewById<View>(R.id.btn_close).setOnClickListener { removePanel() }
        panel.findViewById<View>(R.id.btn_quit).setOnClickListener { quit() }

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
            // 面板跟随悬浮球位置展开
            val (px, py) = panelPosition()
            x = px
            y = py
        }

        windowManager.addView(panel, params)
        panelView = panel

        // 若打开面板时已在运行/暂停，直接展示运行态 UI
        val svc = ClickerService.instance
        if (svc != null && svc.isActive()) {
            showRunningUi(controls, llRunning, tvStatus, btnPause)
        }
    }

    private fun showRunningUi(
        controls: View,
        llRunning: View,
        tvStatus: android.widget.TextView,
        btnPause: View,
    ) {
        controls.visibility = View.GONE
        llRunning.visibility = View.VISIBLE
        updateRunningUi(tvStatus, btnPause)
    }

    /**
     * 计算控制面板跟随悬浮球的定位。
     * 悬浮球在屏幕右半边 → 面板在球左侧；左半边 → 球右侧；
     * 靠近底部时向上偏移，避免超出屏幕。
     */
    private fun panelPosition(): Pair<Int, Int> {
        val dm = resources.displayMetrics
        val screenW = dm.widthPixels
        val screenH = dm.heightPixels

        // 悬浮球尺寸约 52dp（含 padding）
        val ballSize = (52 * dm.density).toInt()
        // 面板估算宽度（WRAP_CONTENT，这里给一个保守值用于越界判断）
        val panelW = (150 * dm.density).toInt()
        val panelH = (200 * dm.density).toInt()

        val ballCenterX = ballX + ballSize / 2

        // 横向：球靠右则面板在左，否则在右
        var px = if (ballCenterX > screenW / 2) {
            ballX - panelW
        } else {
            ballX + ballSize
        }
        // 纵向：面板顶部对齐球顶部，若靠近底部则上移
        var py = ballY
        if (py + panelH > screenH) {
            py = (screenH - panelH).coerceAtLeast(0)
        }

        // 横向边界保护
        px = px.coerceIn(0, (screenW - panelW).coerceAtLeast(0))

        return px to py
    }

    private fun updateRunningUi(tvStatus: android.widget.TextView, btnPause: View) {
        val svc = ClickerService.instance
        if (svc == null) {
            tvStatus.text = "已停止"
            tvStatus.setTextColor(0xFF888888.toInt())
            return
        }
        when {
            svc.isPaused() -> {
                tvStatus.text = "已暂停"
                tvStatus.setTextColor(0xFFB8860B.toInt())
                (btnPause as? android.widget.Button)?.text = "继续"
            }
            svc.isRunningNow() -> {
                val progress = progressText()
                tvStatus.text = if (progress != null) "运行中 $progress" else "运行中"
                tvStatus.setTextColor(0xFF2E7D32.toInt())
                (btnPause as? android.widget.Button)?.text = "暂停"
            }
            else -> {
                tvStatus.text = "已停止"
                tvStatus.setTextColor(0xFF888888.toInt())
                (btnPause as? android.widget.Button)?.text = "暂停"
            }
        }
    }

    private fun progressText(): String? {
        val svc = ClickerService.instance ?: return null
        if (svc.totalCount() <= 0) return null
        return "${svc.currentCount()}/${svc.totalCount()}"
    }

    private fun removePanel() {
        panelView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
        }
        panelView = null
    }

    /**
     * 启动连点。返回是否真正启动成功。
     * 启动前检查无障碍服务与悬浮窗权限，缺少时明确提示并提供跳转入口。
     */
    private fun startClicking(): Boolean {
        // 1. 无障碍服务未开启 → 提示并跳转
        if (!PermissionHelper.isAccessibilityEnabled(this)) {
            toast("未开启无障碍服务，无法模拟点击")
            PermissionHelper.openAccessibilitySettings(this)
            return false
        }

        // 2. 无障碍服务实例未连接 → 提示
        val service = ClickerService.instance
        if (service == null) {
            toast("无障碍服务未连接，请开启后重试")
            return false
        }

        // 3. 悬浮窗权限（本服务能运行到这里说明悬浮窗已有，但保险起见再查）
        if (!PermissionHelper.isOverlayEnabled(this)) {
            toast("缺少悬浮窗权限")
            PermissionHelper.openOverlaySettings(this)
            return false
        }

        val task = TaskStore.loadCurrentTask(this)
        if (task.steps.isEmpty()) {
            toast("任务没有步骤，请先添加点位")
            return false
        }

        val ok = service.start(task)
        if (!ok) {
            toast("任务已在运行")
            return false
        }
        return true
    }

    private fun stopClicking() {
        ClickerService.instance?.stop()
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    /**
     * 关闭悬浮窗：停止服务，移除悬浮球、面板和所有标记。
     */
    private fun quit() {
        stopClicking()
        markers?.clear()
        removePanel()
        ballView?.let { try { windowManager.removeView(it) } catch (_: Exception) {} }
        ballView = null
        stopSelf()
    }

    // ---------------------------------------------------------------------
    // 取点辅助
    // ---------------------------------------------------------------------

    private fun startPick() {
        removePanel()
        // 在屏幕中央新增一个点位，用户可拖动到目标位置
        val dm = resources.displayMetrics
        val centerX = dm.widthPixels / 2f
        val centerY = dm.heightPixels / 2f
        addPoint(centerX, centerY)
        toast("已添加点位，拖动到目标位置")
    }

    /**
     * 新增一个点击步骤（默认在指定坐标），并刷新标记。
     */
    private fun addPoint(x: Float, y: Float) {
        val task = TaskStore.loadCurrentTask(this)
        val newSteps = task.steps.toMutableList()
        newSteps.add(Step(StepType.TAP, x = x, y = y))
        val newTask = task.copy(steps = newSteps)
        TaskStore.save(this, newTask)
        showMarkers()
    }

    // ---------------------------------------------------------------------
    // 点位数字标记
    // ---------------------------------------------------------------------

    private fun showMarkers() {
        val task = TaskStore.loadCurrentTask(this)
        if (markers == null) {
            markers = PointMarkersOverlay(
                this,
                onMarkerClick = { index -> editPoint(index) },
                onMarkerDrag = { index, x, y -> movePoint(index, x, y) },
            )
        }
        markers?.render(task.steps)
    }

    /**
     * 点击标记：弹出参数编辑面板（改类型、时长、滑动终点，或删除）。
     */
    private fun editPoint(index: Int) {
        val task = TaskStore.loadCurrentTask(this)
        if (index < 0 || index >= task.steps.size) return
        val step = task.steps[index]

        editOverlay?.dismiss()
        editOverlay = PointEditOverlay(
            this,
            step,
            title = "点位 ${index + 1}",
            onSave = { newStep -> updatePoint(index, newStep) },
            onDelete = { deletePoint(index) },
        )
        editOverlay?.show()
    }

    private fun updatePoint(index: Int, newStep: Step) {
        val task = TaskStore.loadCurrentTask(this)
        val newSteps = task.steps.toMutableList()
        if (index < 0 || index >= newSteps.size) return
        newSteps[index] = newStep
        val newTask = task.copy(steps = newSteps)
        TaskStore.save(this, newTask)
        showMarkers()
    }

    private fun deletePoint(index: Int) {
        val task = TaskStore.loadCurrentTask(this)
        val newSteps = task.steps.toMutableList()
        if (index < 0 || index >= newSteps.size) return
        newSteps.removeAt(index)
        val newTask = task.copy(steps = newSteps)
        TaskStore.save(this, newTask)
        showMarkers()
        Toast.makeText(this, "已删除点位 ${index + 1}", Toast.LENGTH_SHORT).show()
    }

    private fun movePoint(index: Int, x: Float, y: Float) {
        val task = TaskStore.loadCurrentTask(this)
        val newSteps = task.steps.toMutableList()
        if (index < 0 || index >= newSteps.size) return
        val old = newSteps[index]
        newSteps[index] = old.copy(x = x, y = y)
        val newTask = task.copy(steps = newSteps)
        TaskStore.save(this, newTask)
        // 标记位置已在拖拽过程中实时更新，此处只持久化坐标，不重建标记（避免闪烁）
    }

    override fun onDestroy() {
        ballView?.let { try { windowManager.removeView(it) } catch (_: Exception) {} }
        removePanel()
        editOverlay?.dismiss()
        editOverlay = null
        markers?.clear()
        markers = null
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
