package com.clicker.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import com.clicker.app.engine.*

/**
 * 连点器无障碍服务（核心引擎）。
 * 通过 dispatchGesture 注入点击/滑动手势，不读取任何屏幕内容。
 */
class ClickerService : AccessibilityService() {

    companion object {
        var instance: ClickerService? = null
            private set

        /** 当前是否正在运行 */
        @Volatile
        var isRunning: Boolean = false
            private set
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var task: ClickTask? = null
    private var currentRound = 0
    private var stopped = true

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 不读取屏幕内容，仅作占位
    }

    override fun onInterrupt() {
        stop()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        stop()
        instance = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        stop()
        instance = null
        super.onDestroy()
    }

    /**
     * 启动连点任务。
     */
    fun start(t: ClickTask) {
        if (!stopped) return
        stopped = false
        isRunning = true
        task = t
        currentRound = 0
        runNextRound()
    }

    /**
     * 停止连点任务。
     */
    fun stop() {
        stopped = true
        isRunning = false
        currentRound = 0
        mainHandler.removeCallbacksAndMessages(null)
    }

    /**
     * 执行下一轮。
     */
    private fun runNextRound() {
        if (stopped) return
        val t = task ?: return

        // 次数控制
        if (t.loop == LoopMode.FIXED && currentRound >= t.count) {
            stop()
            return
        }
        currentRound++

        // 随机停顿
        if (t.humanize.randomPauseEnabled &&
            t.humanize.randomPauseEvery > 0 &&
            currentRound % t.humanize.randomPauseEvery == 0
        ) {
            val pause = Randomizer.jitter(0, t.humanize.randomPauseRange)
            if (pause > 0) {
                mainHandler.postDelayed({ executeSteps(t) }, pause)
                return
            }
        }

        executeSteps(t)
    }

    /**
     * 依次执行一轮中的所有步骤，步与步之间按 interval 间隔。
     */
    private fun executeSteps(t: ClickTask) {
        if (stopped) return
        var delay = 0L
        for (step in t.steps) {
            mainHandler.postDelayed({ dispatchStep(t, step) }, delay)
            delay += Randomizer.interval(t.interval, t.humanize.intervalJitter)
        }
        // 一轮结束后，进入下一轮
        mainHandler.postDelayed({ runNextRound() }, delay)
    }

    /**
     * 分发单个步骤。
     */
    private fun dispatchStep(t: ClickTask, step: Step) {
        if (stopped) return
        val (x, y) = Randomizer.jitterCoord(step.x, step.y, t.humanize.coordJitter)
        val (x2, y2) = Randomizer.jitterCoord(step.x2, step.y2, t.humanize.coordJitter)

        when (step.type) {
            StepType.TAP -> dispatchTap(x, y, t.humanize)
            StepType.SWIPE -> dispatchSwipe(x, y, x2, y2, step.duration)
            StepType.DELAY -> { /* 等待由调度器的 postDelayed 处理 */ }
        }
    }

    private fun dispatchTap(x: Float, y: Float, h: HumanizeConfig) {
        val press = Randomizer.pressTime(h.pressTime, h.pressTimeJitter)
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, press))
            .build()
        dispatchGesture(gesture, null, null)
    }

    private fun dispatchSwipe(x1: Float, y1: Float, x2: Float, y2: Float, duration: Long) {
        val d = if (duration <= 0) 100L else duration
        val path = Path().apply {
            moveTo(x1, y1)
            lineTo(x2, y2)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, d))
            .build()
        dispatchGesture(gesture, null, null)
    }
}
