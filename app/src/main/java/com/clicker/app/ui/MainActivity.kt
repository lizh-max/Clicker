package com.clicker.app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
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
    private lateinit var tvSummary: TextView
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
        tvSummary = findViewById(R.id.tv_summary)
        etInterval = findViewById(R.id.et_interval)
        etCoordJitter = findViewById(R.id.et_coord_jitter)
        etIntervalJitter = findViewById(R.id.et_interval_jitter)
        rvSteps = findViewById(R.id.rv_steps)

        adapter = StepAdapter(steps, onDelete = { pos ->
            steps.removeAt(pos)
            adapter.notifyDataSetChanged()
            autoSave()
        }, onEdit = { pos ->
            StepEditDialog.show(this, steps[pos]) { newStep ->
                steps[pos] = newStep
                adapter.notifyDataSetChanged()
                autoSave()
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
                autoSave()
            }
        }
        findViewById<Button>(R.id.btn_save).setOnClickListener {
            saveTask()
            Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.btn_show_ball).setOnClickListener {
            // 显示悬浮球前：先保存当前编辑内容，并检查权限
            saveTask()
            if (!PermissionHelper.isOverlayEnabled(this)) {
                Toast.makeText(this, "请先开启悬浮窗权限", Toast.LENGTH_SHORT).show()
                PermissionHelper.openOverlaySettings(this)
                return@setOnClickListener
            }
            if (!PermissionHelper.isAccessibilityEnabled(this)) {
                Toast.makeText(this, "请先开启无障碍服务", Toast.LENGTH_SHORT).show()
                PermissionHelper.openAccessibilitySettings(this)
                return@setOnClickListener
            }
            val intent = Intent(this, FloatingBallService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }

        // 参数变化时自动保存
        listOf(etInterval, etCoordJitter, etIntervalJitter).forEach { et ->
            et.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) autoSave()
            }
        }

        loadTaskIntoForm()
        updateSummary()
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

    /**
     * 构建当前表单对应的任务对象（不落盘）。
     */
    private fun buildTask(): ClickTask {
        val interval = etInterval.text.toString().toLongOrNull()?.coerceAtLeast(0L) ?: 500L
        val coordJitter = etCoordJitter.text.toString().toFloatOrNull()?.coerceAtLeast(0f) ?: 0f
        val intervalJitter = etIntervalJitter.text.toString().toLongOrNull()?.coerceAtLeast(0L) ?: 0L

        return ClickTask(
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
    }

    /**
     * 保存任务到本地存储。
     */
    private fun saveTask() {
        TaskStore.save(this, buildTask())
        updateSummary()
    }

    /**
     * 自动保存：编辑后立即落盘，避免「忘记保存执行旧任务」。
     */
    private fun autoSave() {
        TaskStore.save(this, buildTask())
        updateSummary()
    }

    /**
     * 更新步骤摘要预览。
     */
    private fun updateSummary() {
        val n = steps.size
        if (n == 0) {
            tvSummary.text = "尚未添加步骤"
            return
        }
        val tap = steps.count { it.type == StepType.TAP }
        val swipe = steps.count { it.type == StepType.SWIPE }
        val delay = steps.count { it.type == StepType.DELAY }
        val interval = etInterval.text.toString().toLongOrNull() ?: 500L

        val parts = mutableListOf<String>()
        parts.add("共 $n 步")
        if (tap > 0) parts.add("点击 $tap")
        if (swipe > 0) parts.add("滑动 $swipe")
        if (delay > 0) parts.add("等待 $delay")
        parts.add("间隔 ${interval}ms")

        tvSummary.text = "步骤摘要：" + parts.joinToString(" · ") +
            "\n循环：无限 · 点位按序号 1→$n 依次执行"
    }
}
