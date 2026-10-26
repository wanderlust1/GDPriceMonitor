package com.wanderlust.gdpricemonitor.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.wanderlust.gdpricemonitor.data.alert.AlertRuleStore

/**
 * 后台检测闹钟接收器：每次被 AlarmManager 唤醒时，
 * 1) 入队一次 [PriceAlertWorker] 执行行情拉取与预警评估；
 * 2) 按最新设置排定下一次闹钟（间隔取自 [AlertRuleStore]）。
 */
class AlertAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val appContext = context.applicationContext
        // 触发一次后台检测
        WorkManager.getInstance(appContext)
            .enqueue(OneTimeWorkRequestBuilder<PriceAlertWorker>().build())
        // 排定下一次（读取最新间隔，设置页改了也能在下一轮生效）
        val minutes = AlertRuleStore(appContext).getBackgroundIntervalMinutes().coerceAtLeast(1)
        PriceAlertScheduler.schedule(appContext, minutes)
    }
}
