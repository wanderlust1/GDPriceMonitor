package com.wanderlust.gdpricemonitor.data.alert

import android.content.Context
import android.content.SharedPreferences

/**
 * 预警阈值与穿越状态的本地存储（SharedPreferences）。
 *
 * 每个品种保存：跌破位阈值(below)、涨破位阈值(above)；
 * 以及穿越判定用的上一次状态(wasBelow/wasAbove)与上次通知时间(用于冷却)。
 * 阈值留空(null)表示该方向不监控。
 * 另存全局设置：后台检测间隔(background_interval_minutes，单位分钟，默认 5)。
 */
class AlertRuleStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("price_alert_rules", Context.MODE_PRIVATE)

    /** 后台检测间隔（分钟），默认 5。 */
    fun getBackgroundIntervalMinutes(): Int = prefs.getInt("background_interval_minutes", 5)
    fun setBackgroundIntervalMinutes(value: Int) =
        prefs.edit().putInt("background_interval_minutes", value).apply()

    fun getBelow(symbol: String): Double? = prefs.getString("below_$symbol", null)?.toDoubleOrNull()
    fun getAbove(symbol: String): Double? = prefs.getString("above_$symbol", null)?.toDoubleOrNull()

    fun setBelow(symbol: String, value: Double?) =
        prefs.edit().putString("below_$symbol", value?.toString()).apply()

    fun setAbove(symbol: String, value: Double?) =
        prefs.edit().putString("above_$symbol", value?.toString()).apply()

    /** 清空某品种的预警策略：移除双阈值并重置穿越状态与冷却时间。 */
    fun clearRule(symbol: String) =
        prefs.edit()
            .remove("below_$symbol")
            .remove("above_$symbol")
            .putBoolean("wasBelow_$symbol", false)
            .putBoolean("wasAbove_$symbol", false)
            .putLong("lastBelow_$symbol", 0L)
            .putLong("lastAbove_$symbol", 0L)
            .apply()

    fun wasBelow(symbol: String): Boolean = prefs.getBoolean("wasBelow_$symbol", false)
    fun setWasBelow(symbol: String, v: Boolean) =
        prefs.edit().putBoolean("wasBelow_$symbol", v).apply()

    fun wasAbove(symbol: String): Boolean = prefs.getBoolean("wasAbove_$symbol", false)
    fun setWasAbove(symbol: String, v: Boolean) =
        prefs.edit().putBoolean("wasAbove_$symbol", v).apply()

    fun lastNotifyBelow(symbol: String): Long = prefs.getLong("lastBelow_$symbol", 0L)
    fun setLastNotifyBelow(symbol: String, t: Long) =
        prefs.edit().putLong("lastBelow_$symbol", t).apply()

    fun lastNotifyAbove(symbol: String): Long = prefs.getLong("lastAbove_$symbol", 0L)
    fun setLastNotifyAbove(symbol: String, t: Long) =
        prefs.edit().putLong("lastAbove_$symbol", t).apply()
}
