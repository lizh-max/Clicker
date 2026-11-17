package com.clicker.app.overlay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

/**
 * 取点坐标回传：取点在悬浮窗（Service）中完成，结果通过广播通知主界面。
 */
object PickCoordBus {

    const val ACTION_PICKED = "com.clicker.app.ACTION_COORD_PICKED"
    const val EXTRA_X = "x"
    const val EXTRA_Y = "y"

    /** 最近一次取点结果（供主界面 onResume 时读取，兜底广播丢失的情况） */
    @Volatile
    var lastPicked: Pair<Float, Float>? = null

    fun notifyPicked(context: Context, x: Float, y: Float) {
        lastPicked = x to y
        val intent = Intent(ACTION_PICKED).apply {
            setPackage(context.packageName)
            putExtra(EXTRA_X, x)
            putExtra(EXTRA_Y, y)
        }
        context.sendBroadcast(intent)
    }

    fun register(context: Context, receiver: BroadcastReceiver) {
        val filter = IntentFilter(ACTION_PICKED)
        context.registerReceiver(receiver, filter)
    }
}
