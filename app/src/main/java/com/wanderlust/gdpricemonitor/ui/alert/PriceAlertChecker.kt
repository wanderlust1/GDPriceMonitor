package com.wanderlust.gdpricemonitor.ui.alert

import android.content.Context
import com.wanderlust.gdpricemonitor.data.alert.AlertRuleStore
import com.wanderlust.gdpricemonitor.data.model.Quote

/**
 * 预警评估：对最新行情逐个判断 跌破位(现价 ≤ 阈值) / 涨破位(现价 ≥ 阈值)。
 *
 * 采用“穿越判定”：仅在价格从阈值外进入阈值内的那一刻通知一次，
 * 离开阈值后重置，下次再穿越才会再次通知，避免持续停留时刷屏。
 * 叠加 30 分钟冷却作为额外保护。
 */
object PriceAlertChecker {

    private const val COOLDOWN_MS = 30 * 60 * 1000L

    fun evaluate(context: Context, quotes: List<Quote>) {
        val store = AlertRuleStore(context)
        val now = System.currentTimeMillis()
        for (q in quotes) {
            if (q.isClosed) continue

            val below = store.getBelow(q.symbol)
            val above = store.getAbove(q.symbol)

            // 跌破位
            val breakBelow = below != null && q.latestPrice <= below
            if (breakBelow &&
                !store.wasBelow(q.symbol) &&
                now - store.lastNotifyBelow(q.symbol) > COOLDOWN_MS
            ) {
                NotificationSender.send(
                    context,
                    q.symbol,
                    "金价跌破位预警",
                    "${q.name} 现价 ${format(q.latestPrice)}，已跌破阈值 ${below?.let { format(it) }}"
                )
                store.setLastNotifyBelow(q.symbol, now)
            }
            store.setWasBelow(q.symbol, breakBelow)

            // 涨破位
            val breakAbove = above != null && q.latestPrice >= above
            if (breakAbove &&
                !store.wasAbove(q.symbol) &&
                now - store.lastNotifyAbove(q.symbol) > COOLDOWN_MS
            ) {
                NotificationSender.send(
                    context,
                    q.symbol,
                    "金价涨破位预警",
                    "${q.name} 现价 ${format(q.latestPrice)}，已涨破阈值 ${above?.let { format(it) }}"
                )
                store.setLastNotifyAbove(q.symbol, now)
            }
            store.setWasAbove(q.symbol, breakAbove)
        }
    }

    private fun format(v: Double): String = String.format("%.2f", v)
}
