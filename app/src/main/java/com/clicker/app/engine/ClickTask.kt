package com.clicker.app.engine

import org.json.JSONArray
import org.json.JSONObject

/**
 * 手势步骤类型。
 */
enum class StepType {
    TAP,     // 点击
    SWIPE,   // 滑动
    DELAY    // 等待
}

/**
 * 一个手势步骤。
 */
data class Step(
    val type: StepType,
    val x: Float = 0f,
    val y: Float = 0f,
    val x2: Float = 0f,
    val y2: Float = 0f,
    val duration: Long = 100L, // SWIPE 滑动耗时 / DELAY 等待时长（毫秒）
)

/**
 * 拟人化随机参数（防检测）。
 * 所有参数均为「基准值 + 浮动范围」结构。
 */
data class HumanizeConfig(
    val coordJitter: Float = 0f,       // 坐标随机偏移范围（像素，如 3f 表示 ±3px）
    val pressTime: Long = 80L,         // 按压时长基准值（毫秒）
    val pressTimeJitter: Long = 40L,   // 按压时长浮动范围
    val intervalJitter: Long = 0L,     // 间隔随机浮动范围（毫秒）
    val randomPauseEnabled: Boolean = false, // 是否启用随机停顿
    val randomPauseEvery: Int = 10,    // 每 N 轮触发一次随机停顿
    val randomPauseRange: Long = 500L, // 随机停顿额外时长范围
)

/**
 * 连点任务定义。
 */
data class ClickTask(
    val name: String = "未命名任务",
    val steps: List<Step> = listOf(Step(StepType.TAP)),
    val interval: Long = 500L,          // 每轮之间的基准间隔（毫秒）
    val loop: LoopMode = LoopMode.INFINITE,
    val count: Int = 100,               // 固定次数时的总轮数
    val humanize: HumanizeConfig = HumanizeConfig(),
)

/**
 * 循环模式。
 */
enum class LoopMode {
    INFINITE,  // 无限循环
    FIXED      // 固定次数
}

// ---------------------------------------------------------------------------
// JSON 序列化（用于本地持久化，零外部依赖）
// ---------------------------------------------------------------------------

fun Step.toJson(): JSONObject = JSONObject().apply {
    put("type", type.name)
    put("x", x.toDouble())
    put("y", y.toDouble())
    put("x2", x2.toDouble())
    put("y2", y2.toDouble())
    put("duration", duration)
}

fun stepFromJson(obj: JSONObject): Step = Step(
    type = StepType.valueOf(obj.optString("type", "TAP")),
    x = obj.optDouble("x", 0.0).toFloat(),
    y = obj.optDouble("y", 0.0).toFloat(),
    x2 = obj.optDouble("x2", 0.0).toFloat(),
    y2 = obj.optDouble("y2", 0.0).toFloat(),
    duration = obj.optLong("duration", 100L),
)

fun HumanizeConfig.toJson(): JSONObject = JSONObject().apply {
    put("coordJitter", coordJitter.toDouble())
    put("pressTime", pressTime)
    put("pressTimeJitter", pressTimeJitter)
    put("intervalJitter", intervalJitter)
    put("randomPauseEnabled", randomPauseEnabled)
    put("randomPauseEvery", randomPauseEvery)
    put("randomPauseRange", randomPauseRange)
}

fun humanizeFromJson(obj: JSONObject): HumanizeConfig = HumanizeConfig(
    coordJitter = obj.optDouble("coordJitter", 0.0).toFloat(),
    pressTime = obj.optLong("pressTime", 80L),
    pressTimeJitter = obj.optLong("pressTimeJitter", 40L),
    intervalJitter = obj.optLong("intervalJitter", 0L),
    randomPauseEnabled = obj.optBoolean("randomPauseEnabled", false),
    randomPauseEvery = obj.optInt("randomPauseEvery", 10),
    randomPauseRange = obj.optLong("randomPauseRange", 500L),
)

fun ClickTask.toJson(): JSONObject = JSONObject().apply {
    put("name", name)
    val arr = JSONArray()
    steps.forEach { arr.put(it.toJson()) }
    put("steps", arr)
    put("interval", interval)
    put("loop", loop.name)
    put("count", count)
    put("humanize", humanize.toJson())
}

fun taskFromJson(obj: JSONObject): ClickTask {
    val stepArr = obj.optJSONArray("steps")
    val steps = mutableListOf<Step>()
    if (stepArr != null) {
        for (i in 0 until stepArr.length()) {
            steps.add(stepFromJson(stepArr.getJSONObject(i)))
        }
    }
    if (steps.isEmpty()) steps.add(Step(StepType.TAP))

    val loopName = obj.optString("loop", "INFINITE")
    val loop = try { LoopMode.valueOf(loopName) } catch (e: Exception) { LoopMode.INFINITE }

    val humanize = obj.optJSONObject("humanize")?.let { humanizeFromJson(it) } ?: HumanizeConfig()

    return ClickTask(
        name = obj.optString("name", "未命名任务"),
        steps = steps,
        interval = obj.optLong("interval", 500L),
        loop = loop,
        count = obj.optInt("count", 100),
        humanize = humanize,
    )
}
