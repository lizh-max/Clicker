package com.clicker.app.util

import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowManager

/**
 * 屏幕度量工具：分辨率、密度换算。
 */
object ScreenMetrics {

    fun getMetrics(context: Context): DisplayMetrics {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(metrics)
        return metrics
    }

    /** 屏幕物理宽度（像素） */
    fun widthPx(context: Context): Int = getMetrics(context).widthPixels

    /** 屏幕物理高度（像素） */
    fun heightPx(context: Context): Int = getMetrics(context).heightPixels

    /** 屏幕中心点坐标（作为默认点击位置） */
    fun centerPoint(context: Context): Pair<Float, Float> =
        widthPx(context) / 2f to heightPx(context) / 2f
}
