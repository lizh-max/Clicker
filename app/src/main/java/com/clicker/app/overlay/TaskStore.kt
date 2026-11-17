package com.clicker.app.overlay

import android.content.Context
import com.clicker.app.engine.ClickTask
import com.clicker.app.engine.Step
import com.clicker.app.engine.StepType
import com.clicker.app.engine.taskFromJson
import com.clicker.app.engine.toJson
import org.json.JSONObject

/**
 * 任务配置本地持久化（SharedPreferences，无网络）。
 * 用 JSON 完整保存多步骤序列（TAP / SWIPE / DELAY）。
 */
object TaskStore {

    private const val PREFS = "clicker_tasks"
    private const val KEY_TASK = "task_json"

    // 兼容旧版本单点字段（用于从 v0.1 迁移）
    private const val KEY_X = "x"
    private const val KEY_Y = "y"

    fun save(context: Context, task: ClickTask) {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        sp.edit()
            .putString(KEY_TASK, task.toJson().toString())
            .apply()
    }

    fun loadCurrentTask(context: Context): ClickTask {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val json = sp.getString(KEY_TASK, null)
        if (json != null) {
            return try {
                taskFromJson(JSONObject(json))
            } catch (e: Exception) {
                migrateLegacy(context)
            }
        }
        return migrateLegacy(context)
    }

    /**
     * 从旧版单点字段迁移到新 JSON 格式。
     * 无任何数据时返回空步骤列表（初始无默认点位）。
     */
    private fun migrateLegacy(context: Context): ClickTask {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val x = sp.getFloat(KEY_X, -1f)
        val y = sp.getFloat(KEY_Y, -1f)
        val steps = if (x >= 0f && y >= 0f) {
            listOf(Step(StepType.TAP, x = x, y = y))
        } else {
            emptyList()
        }
        return ClickTask(steps = steps)
    }
}
