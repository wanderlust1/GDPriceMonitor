package com.wanderlust.gdpricemonitor.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.wanderlust.gdpricemonitor.data.GoldPriceRepository
import com.wanderlust.gdpricemonitor.ui.alert.PriceAlertChecker

/**
 * 后台周期任务：拉取最新行情并评估预警（App 关闭后也能触发通知）。
 * 网络失败返回 retry，由 WorkManager 按退避策略重试。
 */
class PriceAlertWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val quotes = GoldPriceRepository().getQuotes()
        if (quotes.isEmpty()) return Result.retry()
        PriceAlertChecker.evaluate(applicationContext, quotes)
        return Result.success()
    }
}
