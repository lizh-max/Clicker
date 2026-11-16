package com.clicker.app.ui

import android.app.Dialog
import android.content.Context
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.clicker.app.R
import com.clicker.app.engine.Step
import com.clicker.app.engine.StepType

/**
 * 步骤编辑对话框：新增或编辑一个步骤。
 */
object StepEditDialog {

    private val typeLabels = arrayOf("点击", "滑动", "等待")

    fun show(
        context: Context,
        existing: Step?,
        onSave: (Step) -> Unit,
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

        // 回填已有值
        val initType = existing?.type ?: StepType.TAP
        spinner.setSelection(initType.ordinal)
        existing?.let {
            etX.setText(if (it.x == 0f) "" else it.x.toInt().toString())
            etY.setText(if (it.y == 0f) "" else it.y.toInt().toString())
            etX2.setText(if (it.x2 == 0f) "" else it.x2.toInt().toString())
            etY2.setText(if (it.y2 == 0f) "" else it.y2.toInt().toString())
            etDuration.setText(it.duration.toString())
        }

        // 根据类型显隐字段
        fun updateVisibility() {
            val type = StepType.values()[spinner.selectedItemPosition]
            val isSwipe = type == StepType.SWIPE
            val isDelay = type == StepType.DELAY
            // 点击/滑动需要坐标，等待不需要
            val needCoord = !isDelay
            labelX.visibility = if (needCoord) View.VISIBLE else View.GONE
            etX.visibility = if (needCoord) View.VISIBLE else View.GONE
            labelY.visibility = if (needCoord) View.VISIBLE else View.GONE
            etY.visibility = if (needCoord) View.VISIBLE else View.GONE
            // 仅滑动需要终点
            labelX2.visibility = if (isSwipe) View.VISIBLE else View.GONE
            etX2.visibility = if (isSwipe) View.VISIBLE else View.GONE
            labelY2.visibility = if (isSwipe) View.VISIBLE else View.GONE
            etY2.visibility = if (isSwipe) View.VISIBLE else View.GONE
            // 滑动/等待需要时长
            labelDuration.visibility = if (isSwipe || isDelay) View.VISIBLE else View.GONE
            etDuration.visibility = if (isSwipe || isDelay) View.VISIBLE else View.GONE
        }
        spinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateVisibility()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
        updateVisibility()

        val dialog = AlertDialog.Builder(context)
            .setTitle(if (existing == null) "添加步骤" else "编辑步骤")
            .setView(view)
            .setPositiveButton("确定", null)
            .setNegativeButton("取消", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val type = StepType.values()[spinner.selectedItemPosition]
                val x = etX.text.toString().toFloatOrNull() ?: 0f
                val y = etY.text.toString().toFloatOrNull() ?: 0f
                val x2 = etX2.text.toString().toFloatOrNull() ?: 0f
                val y2 = etY2.text.toString().toFloatOrNull() ?: 0f
                val duration = etDuration.text.toString().toLongOrNull() ?: 100L

                // 点击至少要有坐标
                if (!(type == StepType.DELAY) && (x == 0f || y == 0f)) {
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
