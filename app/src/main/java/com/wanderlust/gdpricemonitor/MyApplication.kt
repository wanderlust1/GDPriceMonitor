package com.wanderlust.gdpricemonitor

import android.app.Application
import com.wanderlust.gdpricemonitor.data.alert.AlertRuleStore
import com.wanderlust.gdpricemonitor.ui.alert.NotificationSender
import com.wanderlust.gdpricemonitor.worker.PriceAlertScheduler

/**
 * 应用入口：建通知渠道 + 按设置间隔注册后台检测闹钟（仅一次）。
 */
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationSender.ensureChannel(this)
        val minutes = AlertRuleStore(this).getBackgroundIntervalMinutes().coerceAtLeast(1)
        PriceAlertScheduler.schedule(this, minutes)
    }
}
