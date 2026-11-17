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

/**
 * 取点辅助：全屏透明层，手指滑动显示十字准星与实时坐标，
 * 手指抬起记录该点坐标并通过 [onPicked] 回调返回。
 *
 * 注意：取点层会拦截触摸（因为要拿坐标），取点期间目标 App 不响应触摸，
 * 取点完成关闭本层后即恢复。
 */
class PointPicker(
    private val context: Context,
    private val onPicked: (Float, Float) -> Unit,
) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var rootView: View? = null
    private var crossV: View? = null
    private var crossH: View? = null
    private var tvCoord: TextView? = null

    private var lastX = 0f
    private var lastY = 0f
    private var hasPoint = false

    @SuppressLint("ClickableViewAccessibility", "InflateParams")
    fun show() {
        val layout = LayoutInflater.from(context).inflate(R.layout.overlay_picker, null)
        crossV = layout.findViewById(R.id.cross_v)
        crossH = layout.findViewById(R.id.cross_h)
        tvCoord = layout.findViewById(R.id.tv_coord)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            // 不设 NOT_FOCUSABLE，让层可接收触摸以取坐标
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        layout.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    updateCross(event.rawX, event.rawY)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    updateCross(event.rawX, event.rawY)
                    val x = lastX
                    val y = lastY
                    hasPoint = true
                    dismiss()
                    if (hasPoint) {
                        onPicked(x, y)
                    }
                    true
                }
                else -> false
            }
        }

        windowManager.addView(layout, params)
        rootView = layout
    }

    private fun updateCross(x: Float, y: Float) {
        lastX = x
        lastY = y

        crossV?.let { v ->
            val lp = v.layoutParams as android.widget.FrameLayout.LayoutParams
            lp.leftMargin = x.toInt()
            v.layoutParams = lp
        }
        crossH?.let { v ->
            val lp = v.layoutParams as android.widget.FrameLayout.LayoutParams
            lp.topMargin = y.toInt()
            v.layoutParams = lp
        }
        tvCoord?.let { tv ->
            val lp = tv.layoutParams as android.widget.FrameLayout.LayoutParams
            // 标签放在手指右下方，避免被手指挡住
            lp.leftMargin = x.toInt() + 20
            lp.topMargin = y.toInt() + 20
            tv.layoutParams = lp
            tv.text = "(${x.toInt()}, ${y.toInt()})"
        }
    }

    fun dismiss() {
        rootView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
        }
        rootView = null
    }
}
