package com.clicker.app.ui

import android.content.Context
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.clicker.app.R
import com.clicker.app.engine.Step
import com.clicker.app.engine.StepType
import com.clicker.app.overlay.PointPicker
import com.clicker.app.util.PermissionHelper

/**
 * 步骤编辑对话框：新增或编辑一个步骤，支持取点辅助。
 */
object StepEditDialog {

    private val typeLabels = arrayOf("点击", "滑动", "等待")

    fun show(
        context: Context,
        existing: Step?,
        onSave: (Step) -> Unit,
    ) {
        showInternal(context, existing, onSave, null)
    }

    private fun showInternal(
        context: Context,
        existing: Step?,
        onSave: (Step) -> Unit,
        pendingCoord: Pair<Float, Float>?,
    ) {
        val view = View.inflate(context, R.layout.dialog_step_edit, null)
        val spinner = view.findViewById<Spinner>(R.id.spinner_type)
        val etX = view.findViewById<EditText>(R.id.et_step_x)
        val etY = view.findViewById<EditText>(R.id.et_step_y)
        val etX2 = view.findViewById<EditText>(R.id.et_step_x2)
        val etY2 = view.findViewById<EditText>(R.id.et_step_y2)
        val etDuration = view.findViewById<EditText>(R.id.et_step_duration)

        val labelX = view.findViewById<TextView>(R.id.label_x)
        val labelY = view.findViewById<TextView>(R.id.label_y)
        val labelX2 = view.findViewById<TextView>(R.id.label_x2)
        val labelY2 = view.findViewById<TextView>(R.id.label_y2)
        val labelDuration = view.findViewById<TextView>(R.id.label_duration)

        spinner.adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, typeLabels)
            .apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        val initType = existing?.type ?: StepType.TAP
        spinner.setSelection(initType.ordinal)

        // 回填坐标：优先取点结果，其次已有值
        val initX = pendingCoord?.first ?: existing?.x ?: 0f
        val initY = pendingCoord?.second ?: existing?.y ?: 0f
        etX.setText(if (initX == 0f) "" else initX.toInt().toString())
        etY.setText(if (initY == 0f) "" else initY.toInt().toString())
        existing?.let {
            etX2.setText(if (it.x2 == 0f) "" else it.x2.toInt().toString())
            etY2.setText(if (it.y2 == 0f) "" else it.y2.toInt().toString())
            etDuration.setText(it.duration.toString())
        }

        fun updateVisibility() {
            val type = StepType.values()[spinner.selectedItemPosition]
            val isSwipe = type == StepType.SWIPE
            val isDelay = type == StepType.DELAY
            val needCoord = !isDelay
            labelX.visibility = if (needCoord) View.VISIBLE else View.GONE
            etX.visibility = if (needCoord) View.VISIBLE else View.GONE
            labelY.visibility = if (needCoord) View.VISIBLE else View.GONE
            etY.visibility = if (needCoord) View.VISIBLE else View.GONE
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

        val dialog = AlertDialog.Builder(context)
            .setTitle(if (existing == null) "添加步骤" else "编辑步骤")
            .setView(view)
            .setPositiveButton("确定", null)
            .setNegativeButton("取消", null)
            .create()

        // 取点按钮：关闭对话框 → 显示取点层 → 取点后重开对话框并填入坐标
        view.findViewById<Button>(R.id.btn_pick_point).setOnClickListener {
            dialog.dismiss()
            if (!PermissionHelper.isOverlayEnabled(context)) {
                PermissionHelper.openOverlaySettings(context)
                return@setOnClickListener
            }
            val picker = PointPicker(context) { x, y ->
                showInternal(context, existing, onSave, x to y)
            }
            picker.show()
        }

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val type = StepType.values()[spinner.selectedItemPosition]
                val x = etX.text.toString().toFloatOrNull() ?: 0f
                val y = etY.text.toString().toFloatOrNull() ?: 0f
                val x2 = etX2.text.toString().toFloatOrNull() ?: 0f
                val y2 = etY2.text.toString().toFloatOrNull() ?: 0f
                val duration = etDuration.text.toString().toLongOrNull() ?: 100L

                if (type != StepType.DELAY && (x == 0f || y == 0f)) {
                    etX.error = "请输入坐标"
                    return@setOnClickListener
                }

                val step = Step(
                    type = type,
                    x = x,
                    y = y,
                    x2 = x2,
                    y2 = y2,
                    duration = duration,
                )
                onSave(step)
                dialog.dismiss()
            }
        }
        dialog.show()
    }
}
