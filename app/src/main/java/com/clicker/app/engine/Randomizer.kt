package com.clicker.app.engine

import kotlin.random.Random

/**
 * 拟人化随机参数生成器。
 * 在「基准值 + 浮动范围」内生成随机值，模拟真人手指的不规律性。
 */
object Randomizer {

    private val random = Random.Default

    /**
     * 在 [base] 附近 ±[jitter] 范围内取随机值。
     */
    fun jitter(base: Long, jitter: Long): Long {
        if (jitter <= 0) return base
        return base + random.nextLong(-jitter, jitter + 1)
    }

    /**
     * 坐标偏移：在 [x],[y] 附近 ±[range] 范围内取随机点。
     */
    fun jitterCoord(x: Float, y: Float, range: Float): Pair<Float, Float> {
        if (range <= 0f) return x to y
        val dx = random.nextFloat() * 2 * range - range
        val dy = random.nextFloat() * 2 * range - range
        return (x + dx) to (y + dy)
    }

    /**
     * 实际间隔 = 基准间隔 + 随机抖动。
     */
    fun interval(base: Long, jitter: Long): Long {
        val v = jitter(base, jitter)
        return v.coerceAtLeast(0L)
    }

    /**
     * 按压时长 = 基准 + 随机浮动，下限 20ms。
     */
    fun pressTime(base: Long, jitter: Long): Long {
        val v = jitter(base, jitter)
        return v.coerceAtLeast(20L)
    }
}
