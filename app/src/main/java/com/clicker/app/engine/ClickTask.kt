package com.clicker.app.engine

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
