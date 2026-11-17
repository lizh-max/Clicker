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
 *
 * 运行状态：IDLE -> RUNNING <-> PAUSED -> IDLE
 * 调度采用「手势完成回调推进」方式，确保上一步手势真正结束后才分发下一步，
 * 避免间隔较短或手势耗时较长时出现步骤重叠。
 */
class ClickerService : AccessibilityService() {

    companion object {
        var instance: ClickerService? = null
            private set

        /** 运行状态（供 UI 查询） */
        @Volatile
        var runState: RunState = RunState.IDLE
            private set

        /** 兼容旧字段：是否正在运行 */
        @Volatile
        var isRunning: Boolean = false
            private set

        /** 固定次数任务的总轮数与当前轮（供 UI 显示进度） */
        @Volatile
        var totalCount: Int = 0
            private set

        @Volatile
        var currentCount: Int = 0
            private set
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var task: ClickTask? = null
    private var currentRound = 0
    private var paused = false
    private var stopped = true

    // 供 UI 查询运行状态的辅助方法

    fun isActive(): Boolean = !stopped

    fun isPaused(): Boolean = paused

    fun isRunningNow(): Boolean = !stopped && !paused

    fun totalCount(): Int = totalCount

    fun currentCount(): Int = currentCount

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
     * 启动连点任务。若已在运行则忽略。
     * @return true 表示成功启动，false 表示已在运行（忽略）。
     */
    fun start(t: ClickTask): Boolean {
        if (!stopped) return false
        stopped = false
        paused = false
        task = t
        currentRound = 0
        currentCount = 0
        totalCount = if (t.loop == LoopMode.FIXED) t.count else 0
        setState(RunState.RUNNING)
        runNextRound()
        return true
    }

    /**
     * 暂停：让当前手势执行完后停住，不再推进下一步。
     */
    fun pause() {
        if (stopped || paused) return
        paused = true
        setState(RunState.PAUSED)
    }

    /**
     * 继续：从暂停处恢复，执行下一轮。
     */
    fun resume() {
        if (stopped || !paused) return
        paused = false
        setState(RunState.RUNNING)
        runNextRound()
    }

    /**
     * 停止：立即清空调度队列，重置状态。
     */
    fun stop() {
        stopped = true
        paused = false
        currentRound = 0
        currentCount = 0
        mainHandler.removeCallbacksAndMessages(null)
        setState(RunState.IDLE)
    }

    private fun setState(s: RunState) {
        runState = s
        isRunning = (s == RunState.RUNNING)
    }

    /**
     * 执行下一轮。
     */
    private fun runNextRound() {
        if (stopped || paused) return
        val t = task ?: return

        // 次数控制
        if (t.loop == LoopMode.FIXED && currentRound >= t.count) {
            stop()
            return
        }
        currentRound++
        currentCount = currentRound

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
     * 依次执行一轮中的所有步骤。
     * 采用「手势完成回调推进」：每步手势 dispatch 后，在回调里等待 interval
     * 再执行下一步，避免步骤重叠。
     */
    private fun executeSteps(t: ClickTask) {
        if (stopped || paused) return
        val steps = t.steps
        if (steps.isEmpty()) {
            scheduleNextRound()
            return
        }
        executeStep(t, steps, 0)
    }

    /**
     * 递归执行第 i 个步骤；手势完成后，等待 interval 再执行下一步。
     */
    private fun executeStep(t: ClickTask, steps: List<Step>, i: Int) {
        if (stopped || paused) return
        if (i >= steps.size) {
            // 一轮完成
            scheduleNextRound()
            return
        }
        val step = steps[i]

        // 本步骤生效的间隔/抖动：优先用步骤自身配置，否则回退全局
        val stepInterval = step.interval
        val stepIntervalJitter = step.intervalJitter

        // DELAY 步骤：额外等待 duration，无手势
        if (step.type == StepType.DELAY) {
            val wait = step.duration + Randomizer.interval(stepInterval, stepIntervalJitter)
            mainHandler.postDelayed({ executeStep(t, steps, i + 1) }, wait)
            return
        }

        // 手势完成后的回调里，再推进下一步
        val onDone: () -> Unit = {
            val interval = Randomizer.interval(stepInterval, stepIntervalJitter)
            mainHandler.postDelayed({ executeStep(t, steps, i + 1) }, interval)
            Unit
        }

        val dispatched = dispatchStep(step, onDone)
        if (!dispatched) {
            // 手势分发失败：直接按间隔推进，避免卡死
            val interval = Randomizer.interval(stepInterval, stepIntervalJitter)
            mainHandler.postDelayed({ executeStep(t, steps, i + 1) }, interval)
        }
    }

    private fun scheduleNextRound() {
        if (stopped || paused) return
        mainHandler.post({ runNextRound() })
    }

    /**
     * 分发单个步骤。返回是否成功发起手势（用于回调推进）。
     */
    private fun dispatchStep(step: Step, onDone: () -> Unit): Boolean {
        val (x, y) = Randomizer.jitterCoord(step.x, step.y, step.coordJitter)
        val (x2, y2) = Randomizer.jitterCoord(step.x2, step.y2, step.coordJitter)

        return when (step.type) {
            StepType.TAP -> dispatchTap(x, y, step.pressTime, onDone)
            StepType.SWIPE -> dispatchSwipe(x, y, x2, y2, step.duration, onDone)
            StepType.DELAY -> {
                // 已在上层处理，不会走到这里
                onDone()
                true
            }
        }
    }

    private fun dispatchTap(x: Float, y: Float, pressTime: Long, onDone: () -> Unit): Boolean {
        val press = pressTime.coerceAtLeast(20L)
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, press))
            .build()
        return dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
                onDone()
            }
            override fun onCancelled(gestureDescription: GestureDescription?) {
                super.onCancelled(gestureDescription)
                onDone()
            }
        }, null)
    }

    private fun dispatchSwipe(
        x1: Float, y1: Float, x2: Float, y2: Float, duration: Long, onDone: () -> Unit,
    ): Boolean {
        val d = if (duration <= 0) 100L else duration
        val path = Path().apply {
            moveTo(x1, y1)
            lineTo(x2, y2)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, d))
            .build()
        return dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
                onDone()
            }
            override fun onCancelled(gestureDescription: GestureDescription?) {
                super.onCancelled(gestureDescription)
                onDone()
            }
        }, null)
    }
}

/**
 * 运行状态枚举。
 */
enum class RunState {
    IDLE,     // 未运行
    RUNNING,  // 运行中
    PAUSED,   // 已暂停
}
