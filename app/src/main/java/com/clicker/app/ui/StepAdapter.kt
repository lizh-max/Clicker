package com.clicker.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.clicker.app.R
import com.clicker.app.engine.Step
import com.clicker.app.engine.StepType

/**
 * 步骤列表适配器。
 */
class StepAdapter(
    private val items: MutableList<Step>,
    private val onDelete: (Int) -> Unit,
    private val onEdit: (Int) -> Unit,
) : RecyclerView.Adapter<StepAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val index: TextView = view.findViewById(R.id.tv_step_index)
        val type: TextView = view.findViewById(R.id.tv_step_type)
        val detail: TextView = view.findViewById(R.id.tv_step_detail)
        val delete: View = view.findViewById(R.id.btn_delete_step)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_step, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val step = items[position]
        holder.index.text = (position + 1).toString()
        holder.type.text = typeLabel(step.type)
        holder.detail.text = detailText(step)

        holder.delete.setOnClickListener { onDelete(position) }
        holder.itemView.setOnClickListener { onEdit(position) }
    }

    override fun getItemCount(): Int = items.size

    private fun typeLabel(type: StepType): String = when (type) {
        StepType.TAP -> "点击"
        StepType.SWIPE -> "滑动"
        StepType.DELAY -> "等待"
    }

    private fun detailText(step: Step): String = when (step.type) {
        StepType.TAP -> "(${step.x.toInt()}, ${step.y.toInt()})"
        StepType.SWIPE -> "(${step.x.toInt()}, ${step.y.toInt()}) → (${step.x2.toInt()}, ${step.y2.toInt()}) ${step.duration}ms"
        StepType.DELAY -> "${step.duration}ms"
    }
}
