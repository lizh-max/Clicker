package com.clicker.app.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import com.clicker.app.R
import com.clicker.app.engine.Step
import com.clicker.app.engine.StepType

/**
 * 点位参数编辑悬浮面板（供 Service 使用，避免普通 Dialog 的 window type 限制）。
 * 支持修改类型、时长、滑动终点，以及每点位的间隔/抖动/偏移/按压等拟人化参数。
 * 居中显示，不跟随悬浮球。
 */
class PointEditOverlay(
    private val context: Context,
    private val step: Step,
    private val title: String,
    private val onSave: (Step) -> Unit,
    private val onDelete: () -> Unit,
) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var rootView: View? = null

    private val typeLabels = arrayOf("点击", "滑动", "等待")

    @SuppressLint("InflateParams")
    fun show() {
        val layout = LayoutInflater.from(context).inflate(R.layout.overlay_point_edit, null)
        val tvTitle = layout.findViewById<TextView>(R.id.tv_edit_title)
        val spinner = layout.findViewById<Spinner>(R.id.spinner_type)
        val etX2 = layout.findViewById<EditText>(R.id.et_x2)
        val etY2 = layout.findViewById<EditText>(R.id.et_y2)
        val etDuration = layout.findViewById<EditText>(R.id.et_duration)
        val labelX2 = layout.findViewById<TextView>(R.id.label_x2)
        val labelY2 = layout.findViewById<TextView>(R.id.label_y2)
        val labelDuration = layout.findViewById<TextView>(R.id.label_duration)

        // 本点位参数
        val etInterval = layout.findViewById<EditText>(R.id.et_interval)
        val etIntervalJitter = layout.findViewById<EditText>(R.id.et_interval_jitter)
        val etCoordJitter = layout.findViewById<EditText>(R.id.et_coord_jitter)
        val etPressTime = layout.findViewById<EditText>(R.id.et_press_time)

        tvTitle.text = title

        spinner.adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, typeLabels)
            .apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        spinner.setSelection(step.type.ordinal)

        etX2.setText(if (step.x2 == 0f) "" else step.x2.toInt().toString())
        etY2.setText(if (step.y2 == 0f) "" else step.y2.toInt().toString())
        etDuration.setText(step.duration.toString())

        // 预填本点位参数（非零默认值，用户可直接改）
        etInterval.setText(step.interval.toString())
        etIntervalJitter.setText(step.intervalJitter.toString())
        etCoordJitter.setText(trimFloat(step.coordJitter))
        etPressTime.setText(step.pressTime.toString())

        fun updateVisibility() {
            val type = StepType.values()[spinner.selectedItemPosition]
            val isSwipe = type == StepType.SWIPE
            val isDelay = type == StepType.DELAY
            labelX2.visibility = if (isSwipe) View.VISIBLE else View.GONE
            etX2.visibility = if (isSwipe) View.VISIBLE else View.GONE
            labelY2.visibility = if (isSwipe) View.VISIBLE else View.GONE
            etY2.visibility = if (isSwipe) View.VISIBLE else View.GONE
            labelDuration.visibility = if (isSwipe || isDelay) View.VISIBLE else View.GONE
            etDuration.visibility = if (isSwipe || isDelay) View.VISIBLE else View.GONE
        }
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateVisibility()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        updateVisibility()

        layout.findViewById<Button>(R.id.btn_edit_save).setOnClickListener {
            val type = StepType.values()[spinner.selectedItemPosition]
            val x2 = etX2.text.toString().toFloatOrNull() ?: 0f
            val y2 = etY2.text.toString().toFloatOrNull() ?: 0f
            val duration = etDuration.text.toString().toLongOrNull() ?: 100L

            val interval = etInterval.text.toString().toLongOrNull() ?: 500L
            val intervalJitter = etIntervalJitter.text.toString().toLongOrNull() ?: 100L
            val coordJitter = etCoordJitter.text.toString().toFloatOrNull() ?: 3f
            val pressTime = etPressTime.text.toString().toLongOrNull() ?: 80L

            val newStep = step.copy(
                type = type,
                x2 = x2,
                y2 = y2,
                duration = duration,
                interval = interval,
                intervalJitter = intervalJitter,
                coordJitter = coordJitter,
                pressTime = pressTime,
            )
            dismiss()
            onSave(newStep)
        }
        layout.findViewById<Button>(R.id.btn_edit_delete).setOnClickListener {
            dismiss()
            onDelete()
        }
        layout.findViewById<Button>(R.id.btn_edit_cancel).setOnClickListener { dismiss() }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            // 需要 focus 才能让 EditText 弹出软键盘
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        windowManager.addView(layout, params)
        rootView = layout
    }

    private fun trimFloat(v: Float): String =
        if (v == v.toInt().toFloat()) v.toInt().toString() else v.toString()

    fun dismiss() {
        rootView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
        }
        rootView = null
    }
}
