package com.clicker.app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.clicker.app.R
import com.clicker.app.engine.ClickTask
import com.clicker.app.engine.HumanizeConfig
import com.clicker.app.engine.LoopMode
import com.clicker.app.engine.Step
import com.clicker.app.engine.StepType
import com.clicker.app.overlay.FloatingBallService
import com.clicker.app.overlay.TaskStore
import com.clicker.app.util.PermissionHelper

class MainActivity : AppCompatActivity() {

    private lateinit var tvAccessibility: TextView
    private lateinit var tvOverlay: TextView
    private lateinit var etInterval: EditText
    private lateinit var etCoordJitter: EditText
    private lateinit var etIntervalJitter: EditText
    private lateinit var rvSteps: RecyclerView

    private val steps = mutableListOf<Step>()
    private lateinit var adapter: StepAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvAccessibility = findViewById(R.id.tv_accessibility_status)
        tvOverlay = findViewById(R.id.tv_overlay_status)
        etInterval = findViewById(R.id.et_interval)
        etCoordJitter = findViewById(R.id.et_coord_jitter)
        etIntervalJitter = findViewById(R.id.et_interval_jitter)
        rvSteps = findViewById(R.id.rv_steps)

        adapter = StepAdapter(steps, onDelete = { pos ->
            steps.removeAt(pos)
            adapter.notifyDataSetChanged()
        }, onEdit = { pos ->
            StepEditDialog.show(this, steps[pos]) { newStep ->
                steps[pos] = newStep
                adapter.notifyDataSetChanged()
            }
        })
        rvSteps.layoutManager = LinearLayoutManager(this)
        rvSteps.adapter = adapter

        findViewById<Button>(R.id.btn_accessibility).setOnClickListener {
            PermissionHelper.openAccessibilitySettings(this)
        }
        findViewById<Button>(R.id.btn_overlay).setOnClickListener {
            PermissionHelper.openOverlaySettings(this)
        }
        findViewById<Button>(R.id.btn_add_step).setOnClickListener {
            StepEditDialog.show(this, null) { newStep ->
                steps.add(newStep)
                adapter.notifyDataSetChanged()
            }
        }
        findViewById<Button>(R.id.btn_save).setOnClickListener { saveTask() }
        findViewById<Button>(R.id.btn_show_ball).setOnClickListener {
            if (!PermissionHelper.isOverlayEnabled(this)) {
                PermissionHelper.openOverlaySettings(this)
                return@setOnClickListener
            }
            startService(Intent(this, FloatingBallService::class.java))
        }

        loadTaskIntoForm()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
    }

    private fun updatePermissionStatus() {
        val acc = PermissionHelper.isAccessibilityEnabled(this)
        tvAccessibility.text = if (acc) "✓ 无障碍服务已开启" else "✗ 无障碍服务未开启"
        tvAccessibility.setTextColor(if (acc) 0xFF2E7D32.toInt() else 0xFFD32F2F.toInt())

        val overlay = PermissionHelper.isOverlayEnabled(this)
        tvOverlay.text = if (overlay) "✓ 悬浮窗权限已开启" else "✗ 悬浮窗权限未开启"
        tvOverlay.setTextColor(if (overlay) 0xFF2E7D32.toInt() else 0xFFD32F2F.toInt())
    }

    private fun loadTaskIntoForm() {
        val task = TaskStore.loadCurrentTask(this)
        steps.clear()
        steps.addAll(task.steps)
        adapter.notifyDataSetChanged()
        etInterval.setText(task.interval.toString())
        etCoordJitter.setText(task.humanize.coordJitter.toInt().toString())
        etIntervalJitter.setText(task.humanize.intervalJitter.toString())
    }

    private fun saveTask() {
        if (steps.isEmpty()) {
            steps.add(Step(StepType.TAP, x = 540f, y = 1200f))
        }
        val interval = etInterval.text.toString().toLongOrNull() ?: 500L
        val coordJitter = etCoordJitter.text.toString().toFloatOrNull() ?: 0f
        val intervalJitter = etIntervalJitter.text.toString().toLongOrNull() ?: 0L

        val task = ClickTask(
            name = "连点任务",
            steps = steps.toList(),
            interval = interval,
            loop = LoopMode.INFINITE,
            humanize = HumanizeConfig(
                coordJitter = coordJitter,
                intervalJitter = intervalJitter,
                pressTime = 80L,
                pressTimeJitter = 40L,
            ),
        )
        TaskStore.save(this, task)
    }
}
