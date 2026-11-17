package com.clicker.app.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.clicker.app.R
import com.clicker.app.engine.Step

/**
 * 点位数字标记层：把任务中带坐标的步骤渲染为屏幕上的数字圆圈标记，
 * 支持拖拽移动、点击触发回调。多个点显示多个序号。
 */
class PointMarkersOverlay(
    private val context: Context,
    private val onMarkerClick: (index: Int) -> Unit,
    private val onMarkerDrag: (index: Int, x: Float, y: Float) -> Unit,
) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val markerViews = mutableListOf<View>()

    /** 标记点直径（dp） */
    private val markerSizeDp = 30f

    /** 标记点直径（px） */
    private val markerSizePx: Int = (markerSizeDp * context.resources.displayMetrics.density).toInt()

    private data class DragState(
        var dragging: Boolean = false,
        var startX: Int = 0,
        var startY: Int = 0,
        var touchX: Float = 0f,
        var touchY: Float = 0f,
    )

    /**
     * 根据 steps 渲染/刷新所有标记。只显示有坐标的 TAP/SWIPE 步骤。
     */
    @SuppressLint("ClickableViewAccessibility")
    fun render(steps: List<Step>) {
        clear()

        steps.forEachIndexed { index, step ->
            if (step.x == 0f && step.y == 0f) return@forEachIndexed

            val marker = LayoutInflater.from(context)
                .inflate(R.layout.view_point_marker, null) as TextView
            marker.text = (index + 1).toString()

            val params = WindowManager.LayoutParams(
                markerSizePx,
                markerSizePx,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = step.x.toInt() - markerCenter()
                y = step.y.toInt() - markerCenter()
            }

            // 标记可拖拽，短按触发点击回调
            val drag = DragState()
            marker.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        drag.dragging = false
                        drag.startX = params.x
                        drag.startY = params.y
                        drag.touchX = event.rawX
                        drag.touchY = event.rawY
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - drag.touchX
                        val dy = event.rawY - drag.touchY
                        if (Math.abs(dx) > 5 || Math.abs(dy) > 5) {
                            drag.dragging = true
                        }
                        if (drag.dragging) {
                            params.x = (drag.startX + dx).toInt()
                            params.y = (drag.startY + dy).toInt()
                            windowManager.updateViewLayout(v, params)
                        }
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (drag.dragging) {
                            // 拖拽结束，回传新坐标（标记中心点）
                            val cx = params.x + markerCenter()
                            val cy = params.y + markerCenter()
                            onMarkerDrag(index, cx.toFloat(), cy.toFloat())
                        } else {
                            onMarkerClick(index)
                        }
                        true
                    }
                    else -> false
                }
            }

            windowManager.addView(marker, params)
            markerViews.add(marker)
        }
    }

    private fun markerCenter(): Int {
        // 标记中心偏移 = 直径的一半
        return markerSizePx / 2
    }

    fun clear() {
        markerViews.forEach {
            try { windowManager.removeView(it) } catch (_: Exception) {}
        }
        markerViews.clear()
    }

    fun isShown(): Boolean = markerViews.isNotEmpty()
}
