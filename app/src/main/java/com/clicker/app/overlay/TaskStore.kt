package com.clicker.app.overlay

import android.content.Context
import com.clicker.app.engine.ClickTask
import com.clicker.app.engine.HumanizeConfig
import com.clicker.app.engine.LoopMode
import com.clicker.app.engine.Step
import com.clicker.app.engine.StepType

/**
 * 任务配置本地持久化（SharedPreferences，无网络）。
 */
object TaskStore {

    private const val PREFS = "clicker_tasks"
    private const val KEY_NAME = "name"
    private const val KEY_X = "x"
    private const val KEY_Y = "y"
    private const val KEY_INTERVAL = "interval"
    private const val KEY_LOOP = "loop"
    private const val KEY_COUNT = "count"
    private const val KEY_COORD_JITTER = "coord_jitter"
    private const val KEY_INTERVAL_JITTER = "interval_jitter"
    private const val KEY_PRESS_TIME = "press_time"
    private const val KEY_PRESS_TIME_JITTER = "press_time_jitter"

    fun save(context: Context, task: ClickTask) {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val tap = task.steps.firstOrNull { it.type == StepType.TAP } ?: Step(StepType.TAP)
        sp.edit()
            .putString(KEY_NAME, task.name)
            .putFloat(KEY_X, tap.x)
            .putFloat(KEY_Y, tap.y)
            .putLong(KEY_INTERVAL, task.interval)
            .putString(KEY_LOOP, task.loop.name)
            .putInt(KEY_COUNT, task.count)
            .putFloat(KEY_COORD_JITTER, task.humanize.coordJitter)
            .putLong(KEY_INTERVAL_JITTER, task.humanize.intervalJitter)
            .putLong(KEY_PRESS_TIME, task.humanize.pressTime)
            .putLong(KEY_PRESS_TIME_JITTER, task.humanize.pressTimeJitter)
            .apply()
    }

    fun loadCurrentTask(context: Context): ClickTask {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val x = sp.getFloat(KEY_X, 540f)
        val y = sp.getFloat(KEY_Y, 1200f)
        val name = sp.getString(KEY_NAME, "未命名任务") ?: "未命名任务"
        val interval = sp.getLong(KEY_INTERVAL, 500L)
        val loop = LoopMode.valueOf(sp.getString(KEY_LOOP, LoopMode.INFINITE.name) ?: LoopMode.INFINITE.name)
        val count = sp.getInt(KEY_COUNT, 100)
        val humanize = HumanizeConfig(
            coordJitter = sp.getFloat(KEY_COORD_JITTER, 0f),
            intervalJitter = sp.getLong(KEY_INTERVAL_JITTER, 0L),
            pressTime = sp.getLong(KEY_PRESS_TIME, 80L),
            pressTimeJitter = sp.getLong(KEY_PRESS_TIME_JITTER, 40L),
        )
        return ClickTask(
            name = name,
            steps = listOf(Step(StepType.TAP, x = x, y = y)),
            interval = interval,
            loop = loop,
            count = count,
            humanize = humanize,
        )
    }
}
